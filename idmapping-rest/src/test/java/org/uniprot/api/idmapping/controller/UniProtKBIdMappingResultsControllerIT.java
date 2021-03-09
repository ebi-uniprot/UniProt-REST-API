package org.uniprot.api.idmapping.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.respository.facet.impl.UniprotKBFacetConfig;
import org.uniprot.api.rest.service.RDFPrologs;
import org.uniprot.core.cv.xdb.UniProtDatabaseDetail;
import org.uniprot.core.gene.Gene;
import org.uniprot.core.json.parser.taxonomy.TaxonomyLineageTest;
import org.uniprot.core.json.parser.uniprot.FeatureTest;
import org.uniprot.core.json.parser.uniprot.GeneLocationTest;
import org.uniprot.core.json.parser.uniprot.OrganimHostTest;
import org.uniprot.core.json.parser.uniprot.UniProtKBCrossReferenceTest;
import org.uniprot.core.json.parser.uniprot.comment.AlternativeProductsCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.BPCPCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.CatalyticActivityCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.CofactorCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.DiseaseCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.FreeTextCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.InteractionCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.MassSpectrometryCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.RnaEditingCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.SequenceCautionCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.SubcellularLocationCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.WebResourceCommentTest;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.comment.Comment;
import org.uniprot.core.uniprotkb.comment.CommentType;
import org.uniprot.core.uniprotkb.comment.FreeTextComment;
import org.uniprot.core.uniprotkb.comment.impl.FreeTextCommentBuilder;
import org.uniprot.core.uniprotkb.comment.impl.FreeTextCommentImpl;
import org.uniprot.core.uniprotkb.evidence.impl.EvidencedValueBuilder;
import org.uniprot.core.uniprotkb.feature.UniProtKBFeature;
import org.uniprot.core.uniprotkb.feature.UniprotKBFeatureType;
import org.uniprot.core.uniprotkb.impl.GeneBuilder;
import org.uniprot.core.uniprotkb.impl.GeneNameBuilder;
import org.uniprot.core.uniprotkb.impl.ORFNameBuilder;
import org.uniprot.core.uniprotkb.impl.OrderedLocusNameBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.core.uniprotkb.xdb.UniProtKBCrossReference;
import org.uniprot.cv.chebi.ChebiRepo;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.cv.go.GORepo;
import org.uniprot.cv.xdb.UniProtDatabaseTypes;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniprot.mockers.PathwayRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.indexer.uniprotkb.converter.UniProtEntryConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniprot.UniProtDocument;
import org.uniprot.store.search.domain.EvidenceGroup;
import org.uniprot.store.search.domain.EvidenceItem;
import org.uniprot.store.search.domain.impl.GoEvidences;

/**
 * @author sahmad
 * @created 18/02/2021
 */
@ActiveProfiles(profiles = "offline")
@ContextConfiguration(classes = {DataStoreTestConfig.class, IdMappingREST.class})
@WebMvcTest(UniProtKBIdMappingResultsController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBIdMappingResultsControllerIT extends AbstractIdMappingResultsControllerIT {
    private static final String UNIPROTKB_ID_MAPPING_RESULT_PATH =
            "/idmapping/uniprotkb/results/{jobId}";
    private static final String UNIPROTKB_ID_MAPPING_STREAM_RESULT_PATH =
            "/idmapping/uniprotkb/results/stream/{jobId}";

    static final String UNIPROTKB_AC_ID_STR = "UniProtKB_AC-ID";
    static final String UNIPROTKB_STR = "UniProtKB";

    @Autowired private UniprotKBFacetConfig facetConfig;

    @Autowired private UniProtStoreClient<UniProtKBEntry> storeClient;

    @Qualifier("uniproKBfacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate facetTupleStreamTemplate;

    @Qualifier("uniProtKBTupleStreamTemplate")
    @Autowired
    private TupleStreamTemplate tupleStreamTemplate;

    @Autowired protected JobOperation uniProtKBIdMappingJobOp;

    @Autowired private MockMvc mockMvc;

    @Autowired private RestTemplate uniProtKBRestTemplate;

    private final UniProtEntryConverter documentConverter =
            new UniProtEntryConverter(
                    TaxonomyRepoMocker.getTaxonomyRepo(),
                    mock(GORepo.class),
                    PathwayRepoMocker.getPathwayRepo(),
                    mock(ChebiRepo.class),
                    mock(ECRepo.class),
                    new HashMap<>());

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniprot);
    }

    @Override
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return tupleStreamTemplate;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return facetTupleStreamTemplate;
    }

    @Override
    protected MockMvc getMockMvc() {
        return mockMvc;
    }

    @Override
    protected String getIdMappingResultPath() {
        return UNIPROTKB_ID_MAPPING_RESULT_PATH;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPROTKB;
    }

    @Override
    protected FacetConfig getFacetConfig() {
        return facetConfig;
    }

    @Override
    protected JobOperation getJobOperation() {
        return uniProtKBIdMappingJobOp;
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        String value = "";
        if (searchField.startsWith("ftlen_") || searchField.startsWith("xref_count_")) {
            value = "[* TO *]";
        } else {
            switch (searchField) {
                case "accession_id":
                case "accession":
                    value = "Q00011";
                    break;
                case "mass":
                case "length":
                    value = "[* TO *]";
                    break;
                case "organism_id":
                case "virus_host_id":
                case "taxonomy_id":
                    value = "9606";
                    break;
                case "date_modified":
                case "date_sequence_modified":
                case "date_created":
                case "lit_pubdate":
                    String now = Instant.now().toString();
                    value = "[* TO " + now + "]";
                    break;
                case "proteome":
                    value = "UP000000000";
                    break;
                case "annotation_score":
                    value = "5";
                    break;
            }
        }
        return value;
    }

    private static final UniProtKBEntry TEMPLATE_ENTRY =
            UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);

    @BeforeAll
    void saveEntriesStore() throws Exception {

        when(uniProtKBRestTemplate.getUriTemplateHandler())
                .thenReturn(new DefaultUriBuilderFactory());
        when(uniProtKBRestTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);

        for (int i = 1; i <= 20; i++) {
            UniProtKBEntryBuilder entryBuilder = UniProtKBEntryBuilder.from(TEMPLATE_ENTRY);
            String acc = String.format("Q%05d", i);
            entryBuilder.primaryAccession(acc);
            if (i % 2 == 0) {
                entryBuilder.entryType(UniProtKBEntryType.SWISSPROT);
            } else {
                entryBuilder.entryType(UniProtKBEntryType.TREMBL);
            }

            List<Comment> comments = createAllComments();
            entryBuilder.extraAttributesAdd(
                    UniProtKBEntryBuilder.UNIPARC_ID_ATTRIB, "UP1234567890");
            entryBuilder.lineagesAdd(TaxonomyLineageTest.getCompleteTaxonomyLineage());
            entryBuilder.geneLocationsAdd(GeneLocationTest.getGeneLocation());
            Gene gene =
                    new GeneBuilder()
                            .geneName(new GeneNameBuilder().value("gene " + i).build())
                            .orderedLocusNamesAdd(
                                    new OrderedLocusNameBuilder().value("gene " + i).build())
                            .orfNamesAdd(new ORFNameBuilder().value("gene " + i).build())
                            .build();
            entryBuilder.genesAdd(gene);
            entryBuilder.organismHostsAdd(OrganimHostTest.getOrganismHost());
            UniProtKBEntry uniProtKBEntry = entryBuilder.build();
            uniProtKBEntry.getComments().addAll(comments);

            uniProtKBEntry.getUniProtKBCrossReferences().addAll(createDatabases());
            uniProtKBEntry.getFeatures().addAll(getFeatures());

            storeClient.saveEntry(uniProtKBEntry);

            UniProtDocument doc = documentConverter.convert(uniProtKBEntry);
            doc.otherOrganism = "otherValue";
            doc.unirefCluster50 = "UniRef50_P0001";
            doc.unirefCluster90 = "UniRef90_P0001";
            doc.unirefCluster100 = "UniRef100_P0001";
            doc.uniparc = "UPI000000000";
            doc.computationalPubmedIds.add("890123456");
            doc.communityPubmedIds.add("1234567");
            doc.isIsoform = i % 10 == 0;
            doc.proteomes.add("UP000000000");
            doc.apApu.add("Search All");
            doc.apApuEv.add("Search All");
            doc.apAsEv.add("Search All");
            doc.apRf.add("Search All");
            doc.apRfEv.add("Search All");
            doc.seqCautionFrameshift.add("Search All");
            doc.seqCautionErTerm.add("Search All");
            doc.seqCautionErTran.add("Search All");
            doc.seqCautionMisc.add("Search All");
            doc.seqCautionMiscEv.add("Search All");
            doc.rcPlasmid.add("Search All");
            doc.rcTransposon.add("Search All");
            doc.rcStrain.add("Search All");
            List<String> goAssertionCodes =
                    GoEvidences.INSTANCE.getEvidences().stream()
                            .filter(this::getManualEvidenceGroup)
                            .flatMap(this::getEvidenceCodes)
                            .map(String::toLowerCase)
                            .collect(Collectors.toList());

            goAssertionCodes.addAll(
                    Arrays.asList(
                            "rca", "nd", "ibd", "ikr", "ird", "unknown")); // TODO: is it correct?

            goAssertionCodes.forEach(
                    code ->
                            doc.goWithEvidenceMaps.put(
                                    "go_" + code, Collections.singleton("Search All")));
            Arrays.stream(CommentType.values())
                    .forEach(
                            type -> {
                                String typeName = type.name().toLowerCase();
                                doc.commentEvMap.put(
                                        "ccev_" + typeName, Collections.singleton("Search All"));
                            });
            doc.commentMap.put("cc_unknown", Collections.singleton("Search All"));
            cloudSolrClient.addBean(SolrCollection.uniprot.name(), doc);
            cloudSolrClient.commit(SolrCollection.uniprot.name());
        }
    }

    @Test
    void testUniProtKBToUniProtKBMapping() throws Exception {
        // when
        IdMappingJob job =
                getJobOperation()
                        .createAndPutJobInCache(
                                UNIPROTKB_AC_ID_STR, UNIPROTKB_STR, "Q00001,Q00002");
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_RESULT_PATH, job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(2)))
                .andExpect(jsonPath("$.results.*.from", contains("Q00001", "Q00002")))
                .andExpect(
                        jsonPath("$.results.*.to.primaryAccession", contains("Q00001", "Q00002")));
    }

    @Test
    void testIdMappingWithSuccess() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                mockMvc.perform(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("facets", "reviewed,proteins_with")
                                .param("query", "reviewed:true")
                                .param("fields", "accession,sequence")
                                .param("sort", "accession desc")
                                .param("size", "6"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.facets.size()", is(2)))
                .andExpect(jsonPath("$.facets.*.name", contains("reviewed", "proteins_with")))
                .andExpect(jsonPath("$.facets[0].values.size()", is(1)))
                .andExpect(jsonPath("$.facets[0].values.*.value", contains("true")))
                .andExpect(
                        jsonPath("$.facets[0].values.*.label", contains("Reviewed (Swiss-Prot)")))
                .andExpect(jsonPath("$.facets[0].values.*.count", contains(10)))
                .andExpect(jsonPath("$.results.size()", is(6)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains(
                                        "Q00020", "Q00018", "Q00016", "Q00014", "Q00012",
                                        "Q00010")))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains(
                                        "Q00020", "Q00018", "Q00016", "Q00014", "Q00012",
                                        "Q00010")))
                .andExpect(jsonPath("$.results.*.to.sequence").exists())
                .andExpect(jsonPath("$.results.*.to.organism").doesNotExist());
    }

    @Test
    void testCanSortMultipleFieldsWithSuccess() throws Exception {
        // when
        IdMappingJob job =
                getJobOperation()
                        .createAndPutJobInCache(
                                UNIPROTKB_AC_ID_STR, UNIPROTKB_STR, "Q00001,Q00002");
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_RESULT_PATH, job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("facets", "proteins_with,reviewed")
                                .param("sort", "gene desc , accession asc"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(2)))
                .andExpect(jsonPath("$.results.*.from", contains("Q00002", "Q00001")))
                .andExpect(
                        jsonPath("$.results.*.to.primaryAccession", contains("Q00002", "Q00001")));
    }

    @Test
    void streamRDFCanReturnSuccess() throws Exception {
        // when
        IdMappingJob job =
                getJobOperation()
                        .createAndPutJobInCache(UNIPROTKB_AC_ID_STR, UNIPROTKB_STR, "Q00001");
        MockHttpServletRequestBuilder requestBuilder =
                get(UNIPROTKB_ID_MAPPING_STREAM_RESULT_PATH, job.getJobId())
                        .header(ACCEPT, UniProtMediaType.RDF_MEDIA_TYPE);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(content().string(startsWith(RDFPrologs.UNIPROT_RDF_PROLOG)))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "    <sample>text</sample>\n"
                                                        + "    <anotherSample>text2</anotherSample>\n"
                                                        + "    <someMore>text3</someMore>\n\n"
                                                        + "</rdf:RDF>")));
    }

    @Test
    void testGetResultsInTSV() throws Exception {
        // when
        MediaType mediaType = UniProtMediaType.TSV_MEDIA_TYPE;
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdMappingResultPath(), job.getJobId()).header(ACCEPT, mediaType);

        ResultActions response = getMockMvc().perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                .andExpect(content().contentTypeCompatibleWith(mediaType))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "From\tEntry\tEntry Name\tReviewed\tProtein names\tGene Names\tOrganism\tLength")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Q00001\tQ00001\tFGFR2_HUMAN\tunreviewed\tFibroblast growth factor receptor 2, FGFR-2, EC 2.7.10.1 (K-sam, KGFR) (Keratinocyte growth factor receptor) (CD antigen CD332)\tFGFR2 BEK KGFR KSAM; gene 1 gene 1 gene 1\tHomo sapiens (Human)\t821\n"
                                                        + "Q00002\tQ00002\tFGFR2_HUMAN\treviewed\tFibroblast growth factor receptor 2, FGFR-2, EC 2.7.10.1 (K-sam, KGFR) (Keratinocyte growth factor receptor) (CD antigen CD332)\tFGFR2 BEK KGFR KSAM; gene 2 gene 2 gene 2\tHomo sapiens (Human)\t821\n"
                                                        + "Q00003\tQ00003\tFGFR2_HUMAN\tunreviewed\tFibroblast growth factor receptor 2, FGFR-2, EC 2.7.10.1 (K-sam, KGFR) (Keratinocyte growth factor receptor) (CD antigen CD332)\tFGFR2 BEK KGFR KSAM; gene 3 gene 3 gene 3\tHomo sapiens (Human)\t821\n"
                                                        + "Q00004\tQ00004\tFGFR2_HUMAN\treviewed\tFibroblast growth factor receptor 2, FGFR-2, EC 2.7.10.1 (K-sam, KGFR) (Keratinocyte growth factor receptor) (CD antigen CD332)\tFGFR2 BEK KGFR KSAM; gene 4 gene 4 gene 4\tHomo sapiens (Human)\t821\n"
                                                        + "Q00005\tQ00005\tFGFR2_HUMAN\tunreviewed\tFibroblast growth factor receptor 2, FGFR-2, EC 2.7.10.1 (K-sam, KGFR) (Keratinocyte growth factor receptor) (CD antigen CD332)\tFGFR2 BEK KGFR KSAM; gene 5 gene 5 gene 5\tHomo sapiens (Human)\t821\n")));
    }

    @Override
    protected String getDefaultSearchQuery() {
        return "FGF1"; // geneName
    }

    // TODO: remove duplicated code with UniprotIT
    private List<Comment> createAllComments() {
        List<Comment> comments = new ArrayList<>();
        comments.add(AlternativeProductsCommentTest.getAlternativeProductsComment());
        comments.add(BPCPCommentTest.getBpcpComment());
        comments.add(CatalyticActivityCommentTest.getCatalyticActivityComment());
        comments.add(CofactorCommentTest.getCofactorComment());
        comments.add(DiseaseCommentTest.getDiseaseComment());
        comments.add(FreeTextCommentTest.getFreeTextComment());
        comments.add(FreeTextCommentTest.getFreeTextComment2());
        comments.add(InteractionCommentTest.getInteractionComment());
        comments.add(MassSpectrometryCommentTest.getMassSpectrometryComment());
        comments.add(RnaEditingCommentTest.getRnaEditingComment());
        comments.add(SequenceCautionCommentTest.getSequenceCautionComment());
        comments.add(SubcellularLocationCommentTest.getSubcellularLocationComment());
        comments.add(WebResourceCommentTest.getWebResourceComment());
        List<Comment> freeTextComments =
                Arrays.stream(CommentType.values())
                        .filter(FreeTextCommentImpl::isFreeTextCommentType)
                        .map(FreeTextCommentTest::getFreeTextComment)
                        .collect(Collectors.toList());

        FreeTextComment similarityFamily =
                new FreeTextCommentBuilder()
                        .commentType(CommentType.SIMILARITY)
                        .textsAdd(
                                new EvidencedValueBuilder()
                                        .value("Belongs to the NSMF family")
                                        .build())
                        .build();
        freeTextComments.add(similarityFamily);

        comments.addAll(freeTextComments);
        return comments;
    }

    private boolean getManualEvidenceGroup(EvidenceGroup evidenceGroup) {
        return evidenceGroup.getGroupName().equalsIgnoreCase("Manual assertions");
    }

    private Stream<String> getEvidenceCodes(EvidenceGroup evidenceGroup) {
        return evidenceGroup.getItems().stream().map(EvidenceItem::getCode);
    }

    private List<UniProtKBCrossReference> createDatabases() {
        List<UniProtKBCrossReference> xrefs =
                UniProtDatabaseTypes.INSTANCE.getAllDbTypes().stream()
                        .map(UniProtDatabaseDetail::getName)
                        .map(UniProtKBCrossReferenceTest::getUniProtDBCrossReference)
                        .collect(Collectors.toList());

        xrefs.add(UniProtKBCrossReferenceTest.getUniProtDBGOCrossReferences("C", "IDA"));
        xrefs.add(UniProtKBCrossReferenceTest.getUniProtDBGOCrossReferences("F", "IDA"));
        xrefs.add(UniProtKBCrossReferenceTest.getUniProtDBGOCrossReferences("P", "IDA"));
        return xrefs;
    }

    private List<UniProtKBFeature> getFeatures() {
        List<UniProtKBFeature> features =
                Arrays.stream(UniprotKBFeatureType.values())
                        .map(FeatureTest::getFeature)
                        .collect(Collectors.toList());
        return features;
    }
}
