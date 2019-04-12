package uk.ac.ebi.uniprot.api.uniprotkb.controller;

import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import uk.ac.ebi.uniprot.api.common.repository.store.StoreStreamer;
import uk.ac.ebi.uniprot.api.common.repository.store.TupleStreamTemplate;
import uk.ac.ebi.uniprot.api.rest.respository.RepositoryConfig;
import uk.ac.ebi.uniprot.api.uniprotkb.configuration.UniprotKBConfig;
import uk.ac.ebi.uniprot.api.uniprotkb.controller.UniprotKBController;
import uk.ac.ebi.uniprot.api.uniprotkb.output.MessageConverterConfig;
import uk.ac.ebi.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import uk.ac.ebi.uniprot.api.uniprotkb.repository.search.impl.UniprotFacetConfig;
import uk.ac.ebi.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import uk.ac.ebi.uniprot.api.uniprotkb.repository.store.ResultsConfig;
import uk.ac.ebi.uniprot.api.uniprotkb.repository.store.UniProtStoreConfig;
import uk.ac.ebi.uniprot.api.uniprotkb.service.UniProtEntryService;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.indexer.uniprot.mockers.UniProtEntryMocker;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.FF_MEDIA_TYPE;
import static uk.ac.ebi.uniprot.api.uniprotkb.controller.UniprotKBController.UNIPROTKB_RESOURCE;

/**
 * Created 21/09/18
 *
 * @author Edd
 */
@RunWith(SpringRunner.class)
@WebMvcTest({UniprotKBController.class})
@Import({DataStoreTestConfig.class, RepositoryConfig.class, UniprotFacetConfig.class, UniProtEntryService.class, UniprotQueryRepository.class,
         UniProtStoreConfig.class, ResultsConfig.class, MessageConverterConfig.class, UniprotKBConfig.class})
@AutoConfigureWebClient
public class UniProtKBDownloadTest {
    private static final String DOWNLOAD_RESOURCE = UNIPROTKB_RESOURCE + "/download/";
    private static final String QUERY = "query";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStoreManager storeManager;

    @MockBean
    private TupleStreamTemplate tupleStreamTemplate;

    @MockBean
    private StoreStreamer<UniProtEntry> uniProtEntryStoreStreamer;

    @Before
    public void setUp() {
        when(tupleStreamTemplate.create(any(),any())).thenReturn(mock(TupleStream.class));
    }

    @Test
    public void canReachDownloadEndpoint() throws Exception {
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        mockStreamerResponseOf(entry);

        ResultActions response = mockMvc.perform(
                get(DOWNLOAD_RESOURCE)
                        .header(ACCEPT, FF_MEDIA_TYPE)
                        .param(QUERY, accessionQuery(acc)));

        response.andExpect(
                request().asyncStarted())
                .andDo(MvcResult::getAsyncResult)
                .andDo(print())
                .andExpect(content().contentType(FF_MEDIA_TYPE))
                .andExpect(content().string(containsString("AC   Q8DIA7;")))
                .andExpect(header().stringValues(VARY, ACCEPT, ACCEPT_ENCODING))
                .andExpect(header().exists(CONTENT_DISPOSITION));
    }

    private String accessionQuery(String acc) {
        return "accession:" + acc;
    }

    private void mockStreamerResponseOf(UniProtEntry... entries) {
        when(uniProtEntryStoreStreamer.idsToStoreStream(any()))
                .thenReturn(Stream.of(entries));
    }
}