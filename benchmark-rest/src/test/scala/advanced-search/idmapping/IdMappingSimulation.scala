import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import scala.concurrent.duration._
import com.typesafe.config._
import scala.io.Source

/**
 * Simulates users accessing the /idmapping service to generate a range of performance statistics
 * for different sizes of source 'ids'.
 */
class IdMappingSimulation extends Simulation {

  val conf = ConfigFactory.load()

  val httpConf = http
    .userAgentHeader("Benchmarker")
    .doNotTrackHeader("1")

  val host = conf.getString("a.s.host")
  println("host = "+host)
  val rawAccessions = Source.fromFile(conf.getString("a.s.idmapping.accessions.csv")).getLines.mkString
  println("rawAccessions = "+rawAccessions)
  val accessions = rawAccessions.split(",").map(_.trim()).toSeq
  println("accessions = "+accessions)

  def getRandomisedIds(count: Integer): String = {
    val randomList = scala.util.Random.shuffle(accessions).slice(0, count).mkString(",")
    println("randomList = " + randomList)
    return randomList
  }

  // --------- IDMAPPING SCENARIO ----------
  def getIdMappingFlowRequest(ids: String): ChainBuilder = {
    val count = "${randomIds}".count(_ == ',')
    val httpReqInfo: String = "POST /run [UniProtKB Acc->EMBL, "+count+"]"
    val queryRequestStr: String = host + "/uniprot/api/idmapping/run"


//    val ids = getRandomisedIds(count)
    println(">>> "+queryRequestStr + " with ids="+ids)

    val request =
//        pause(15 seconds, 120 seconds)
        pause(1 seconds, 5 seconds)
        .exec(http(httpReqInfo)
          .post(queryRequestStr)
          .formParam("ids", ids)
          .formParam("from", "UniProtKB_AC-ID")
          .formParam("to", "EMBL-GenBank-DDBJ_CDS")
          .check(
            jsonPath("$.jobId").saveAs("jobId")
          )
        )
          .doIf("${jobId.exists()}") {
            tryMax(100) {
              exec(
                http("GET /status/JOB_ID [UniProtKB Acc->EMBL, "+count+"]")
                  .get(host + "/uniprot/api/idmapping/status/${jobId}")
                  .disableFollowRedirect
                  .check(
                     status.not(400), status.not(500),
                     jsonPath("$.jobStatus").saveAs("jobStatus")
                  )
              )
                .doIfEquals("${jobStatus}", "FINISHED") {
                    exec( http("GET /results/JOB_ID [UniProtKB Acc->EMBL, "+count+"]")
                      .get(host + "/uniprot/api/idmapping/results/${jobId}")
                      .check(status.is(200)))
                }
            }
          }

    return request
  }

//  val idMappingRequestFlowSeq = Seq(
//    getIdMappingFlowRequest(5)
//  )

  val idMapping50Instance =
    scenario("IdMapping UniProtKB Acc->EMBL (#ids=50) Scenario")
      .forever {
        exec {
          session => session.set("randomIds", getRandomisedIds(5))
        }
          // TODO: randomIds not being interpolated
          .exec(getIdMappingFlowRequest("${randomIds}"))
//        .exec(idMappingRequestFlowSeq)
      }

  setUp(
    idMapping50Instance.inject(atOnceUsers(conf.getInt("a.s.multi.filters.download.users")))
  )
    .protocols(httpConf)
    .assertions(global.responseTime.percentile3.lt(conf.getInt("a.s.multi.filters.percentile3.responseTime")), //percentile3 == 95th Percentile
      global.successfulRequests.percent.gt(conf.getInt("a.s.multi.filters.successPercentGreaterThan")))
    .maxDuration(conf.getInt("a.s.multi.filters.maxDuration") minutes)
}
