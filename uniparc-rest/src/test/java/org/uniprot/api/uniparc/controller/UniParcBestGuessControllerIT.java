package org.uniprot.api.uniparc.controller;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniparc.UniParcDocumentConverter;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.uniprot.api.uniparc.controller.UniParcControllerITUtils.createEntry;
import static org.uniprot.api.uniparc.controller.UniParcControllerITUtils.getXref;

/**
 * @author lgonzales
 * @since 17/08/2020
 */
@Slf4j
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
        })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniParcBestGuessControllerIT extends AbstractStreamControllerIT {

    private static final String BEST_GUESS_PATH = "/uniparc/bestguess";

    private final UniParcDocumentConverter documentConverter =
            new UniParcDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo());
    @Autowired UniProtStoreClient<UniParcEntry> storeClient;
    @Autowired private MockMvc mockMvc;
    @Autowired private SolrClient solrClient;

    @BeforeAll
    void initUniParcBestGuessDataStore() throws IOException, SolrServerException {
        saveEntries();

        // for the following tests, ensure the number of hits
        // for each query is less than the maximum number allowed
        // to be streamed (configured in {@link
        // org.uniprot.api.common.repository.store.StreamerConfigProperties})
        long queryHits = 100L;
        QueryResponse response = mock(QueryResponse.class);
        SolrDocumentList results = mock(SolrDocumentList.class);
        when(results.getNumFound()).thenReturn(queryHits);
        when(response.getResults()).thenReturn(results);
        when(solrClient.query(anyString(), any())).thenReturn(response);
    }

    @Test
    void bestGuessCanReturnSuccessSwissProt() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(BEST_GUESS_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "content:*");

        mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.uniParcId", is("UPI0000183A10")))
                .andExpect(jsonPath("$.uniParcCrossReferences.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.uniParcCrossReferences[0].database", is("UniProtKB/Swiss-Prot")))
                .andExpect(jsonPath("$.uniParcCrossReferences[0].id", is("swissProt0")));
    }

    @Test
    void bestGuessCanReturnSuccessTrembl() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(BEST_GUESS_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "upi:UPI0000183A11 OR upi:UPI0000183A12");

        mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.uniParcId", is("UPI0000183A12")))
                .andExpect(jsonPath("$.uniParcCrossReferences.size()", is(1)))
                .andExpect(jsonPath("$.uniParcCrossReferences[0].database", is("UniProtKB/TrEMBL")))
                .andExpect(jsonPath("$.uniParcCrossReferences[0].id", is("trembl2")));
    }

    @Test
    void bestGuessCanReturnIsoForm() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(BEST_GUESS_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param(
                                "query",
                                "upi:UPI0000183A11 OR upi:UPI0000183A12 OR upi:UPI0000183A13");

        mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.uniParcId", is("UPI0000183A13")))
                .andExpect(jsonPath("$.uniParcCrossReferences.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.uniParcCrossReferences[0].database",
                                is("UniProtKB/Swiss-Prot protein isoforms")))
                .andExpect(jsonPath("$.uniParcCrossReferences[0].id", is("isoform3")));
    }

    @Test
    void bestGuessCanReturnSuccessFilteringTaxonomy() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(BEST_GUESS_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "taxonomy_id:9609");

        mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.uniParcId", is("UPI0000183A13")))
                .andExpect(jsonPath("$.uniParcCrossReferences.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.uniParcCrossReferences[0].database",
                                is("UniProtKB/Swiss-Prot protein isoforms")))
                .andExpect(jsonPath("$.uniParcCrossReferences[0].id", is("isoform3")));
    }

    @Test
    void bestGuessCanReturnSuccessWithFields() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(BEST_GUESS_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "database:embl-cds")
                        .param("fields", "upi,sequence");

        mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.uniParcId", is("UPI0000183A10")))
                .andExpect(jsonPath("$.sequence").exists())
                .andExpect(jsonPath("$.uniParcCrossReferences").doesNotExist());
    }

    @Test
    void bestGuessReturnBadRequest() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(BEST_GUESS_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "invalid_query:9607")
                        .param("fields", "invalid");

        mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(jsonPath("$.messages.size()", is(2)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Invalid fields parameter value 'invalid'",
                                        "'invalid_query' is not a valid search field")));
    }

    @Test
    void bestGuessReturnBadRequestIfFoundDuplicatedEntries() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(BEST_GUESS_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "taxonomy_id:9607");

        mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. More than one Best Guess found {UPI0000183A10:trembl0;UPI0000183A12:trembl2;UPI0000183A13:trembl3}. Review your query and/or contact us.")));
    }

    private void saveEntries() throws IOException, SolrServerException {
        UniParcEntryConverter converter = new UniParcEntryConverter();

        // SWISSPROT
        List<UniParcCrossReference> crossReferences = new ArrayList<>();
        crossReferences.add(getXref(UniParcDatabase.SWISSPROT, "swissProt0", 9606, true));
        crossReferences.add(getXref(UniParcDatabase.TREMBL, "trembl0", 9607, true));
        crossReferences.add(
                getXref(UniParcDatabase.SWISSPROT_VARSPLIC, "isoformInactive0", 9608, false));
        crossReferences.add(getXref(UniParcDatabase.EMBL, "EMBL0", 9609, true));

        UniParcEntry entry = createEntry("UPI0000183A10", 20, crossReferences);
        UniParcDocument doc = documentConverter.convert(converter.toXml(entry));
        cloudSolrClient.addBean(SolrCollection.uniparc.name(), doc);
        storeClient.saveEntry(entry);

        // SWISSPROT - SMALLER SEQUENCE
        crossReferences = new ArrayList<>();
        crossReferences.add(getXref(UniParcDatabase.SWISSPROT, "swissProt1", 9606, true));
        crossReferences.add(getXref(UniParcDatabase.TREMBL, "trembl1", 9607, true));
        crossReferences.add(
                getXref(UniParcDatabase.SWISSPROT_VARSPLIC, "isoformInactive1", 9608, false));
        crossReferences.add(getXref(UniParcDatabase.EMBL, "EMBL1", 9609, true));

        entry = createEntry("UPI0000183A11", 19, crossReferences);
        doc = documentConverter.convert(converter.toXml(entry));
        cloudSolrClient.addBean(SolrCollection.uniparc.name(), doc);
        storeClient.saveEntry(entry);

        // TREMBL
        crossReferences = new ArrayList<>();
        crossReferences.add(getXref(UniParcDatabase.TREMBL, "trembl2", 9607, true));
        crossReferences.add(getXref(UniParcDatabase.TREMBL, "inactive2", 9608, false));
        crossReferences.add(getXref(UniParcDatabase.EMBL, "EMBL2", 9609, true));

        entry = createEntry("UPI0000183A12", 20, crossReferences);
        doc = documentConverter.convert(converter.toXml(entry));
        cloudSolrClient.addBean(SolrCollection.uniparc.name(), doc);
        storeClient.saveEntry(entry);

        // ISOFORM
        crossReferences = new ArrayList<>();
        crossReferences.add(getXref(UniParcDatabase.SWISSPROT_VARSPLIC, "isoform3", 9609, true));
        crossReferences.add(getXref(UniParcDatabase.TREMBL, "trembl3", 9607, true));
        crossReferences.add(getXref(UniParcDatabase.SWISSPROT, "swissProtInactive3", 9608, false));
        crossReferences.add(getXref(UniParcDatabase.EMBL, "EMBL3", 9610, true));
        entry = createEntry("UPI0000183A13", 20, crossReferences);
        doc = documentConverter.convert(converter.toXml(entry));
        cloudSolrClient.addBean(SolrCollection.uniparc.name(), doc);
        storeClient.saveEntry(entry);

        cloudSolrClient.commit(SolrCollection.uniparc.name());
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return Collections.singletonList(SolrCollection.uniparc);
    }
}
