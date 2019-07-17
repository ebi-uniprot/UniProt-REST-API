package uk.ac.ebi.uniprot.crossref.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import uk.ac.ebi.uniprot.api.crossref.controller.CrossRefController;
import uk.ac.ebi.uniprot.api.crossref.service.CrossRefService;
import uk.ac.ebi.uniprot.api.support_data.SupportDataApplication;
import uk.ac.ebi.uniprot.domain.crossref.CrossRefEntry;
import uk.ac.ebi.uniprot.domain.crossref.CrossRefEntryBuilder;
import uk.ac.ebi.uniprot.repository.SolrTestConfig;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes={SolrTestConfig.class, SupportDataApplication.class})
@WebMvcTest(CrossRefController.class)
class CrossRefControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private CrossRefService crossRefService;

    @Test
    void testGetCrossRefByAccession() throws Exception {
        String accession = "DB-1234";
        CrossRefEntry crossRef = createDBXRef();
        Mockito.when(this.crossRefService.findByAccession(accession)).thenReturn(crossRef);

        ResultActions response = this.mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/xref/" + accession)
                        .param("accessionId", accession)
        );

        response.andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$.accession", equalTo(crossRef.getAccession())))
                .andExpect(jsonPath("$.abbrev", equalTo(crossRef.getAbbrev())))
                .andExpect(jsonPath("$.name", equalTo(crossRef.getName())))
                .andExpect(jsonPath("$.pub_med_id", equalTo(crossRef.getPubMedId())))
                .andExpect(jsonPath("$.doi_id", equalTo(crossRef.getDoiId())))
                .andExpect(jsonPath("$.link_type", equalTo(crossRef.getLinkType())))
                .andExpect(jsonPath("$.server", equalTo(crossRef.getServer())))
                .andExpect(jsonPath("$.db_url", equalTo(crossRef.getDbUrl())))
                .andExpect(jsonPath("$.category", equalTo(crossRef.getCategory())))
                .andExpect(jsonPath("$.reviewed_protein_count", equalTo(Integer.valueOf(crossRef.getReviewedProteinCount().toString()))))
                .andExpect(jsonPath("$.unreviewed_protein_count", equalTo(Integer.valueOf(crossRef.getUnreviewedProteinCount().toString()))));
    }

    private CrossRefEntry createDBXRef(){
        String random = UUID.randomUUID().toString();
        String ac = random + "-AC-";
        String ab = random + "-AB-";
        String nm = random + "-NM-";
        String pb = random + "-PB-";
        String di = random + "-DI-";
        String lt = random + "-LT-";
        String sr = random + "-SR-";
        String du = random + "-DU-";
        String ct = random + "-CT-";

        CrossRefEntryBuilder builder = new CrossRefEntryBuilder();
        builder.abbrev(ab).accession(ac).category(ct).dbUrl(du);
        builder.doiId(di).linkType(lt).name(nm).pubMedId(pb).server(sr);
        builder.reviewedProteinCount(2L).unreviewedProteinCount(3L);
        return builder.build();
    }
}
