package org.uniprot.api.support.data.literature.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.support.data.literature.repository.LiteratureRepository;
import org.uniprot.core.CrossReference;
import org.uniprot.core.citation.CitationDatabase;
import org.uniprot.core.citation.Literature;
import org.uniprot.core.citation.impl.AuthorBuilder;
import org.uniprot.core.citation.impl.LiteratureBuilder;
import org.uniprot.core.citation.impl.PublicationDateBuilder;
import org.uniprot.core.impl.CrossReferenceBuilder;
import org.uniprot.core.json.parser.literature.LiteratureJsonConfig;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.literature.impl.LiteratureEntryBuilder;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.literature.LiteratureDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author lgonzales
 * @since 2019-07-05
 */
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(LiteratureController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            LiteratureGetIdControllerIT.LiteratureGetIdParameterResolver.class,
            LiteratureGetIdControllerIT.LiteratureGetIdContentTypeParamResolver.class
        })
class LiteratureGetIdControllerIT extends AbstractGetByIdControllerIT {

    private static final long PUBMED_ID = 100L;

    @Autowired private LiteratureRepository repository;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.LITERATURE;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.literature;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected void saveEntry() {

        CrossReference<CitationDatabase> pubmed =
                new CrossReferenceBuilder<CitationDatabase>()
                        .database(CitationDatabase.PUBMED)
                        .id(String.valueOf(PUBMED_ID))
                        .build();

        Literature literature =
                new LiteratureBuilder()
                        .citationCrossReferencesAdd(pubmed)
                        .title("The Title")
                        .authorsAdd(new AuthorBuilder("The Author").build())
                        .literatureAbstract("literature abstract")
                        .publicationDate(new PublicationDateBuilder("2019").build())
                        .firstPage("10")
                        .build();

        LiteratureEntry literatureEntry = new LiteratureEntryBuilder().citation(literature).build();

        LiteratureDocument document =
                LiteratureDocument.builder()
                        .id(String.valueOf(PUBMED_ID))
                        .literatureObj(getLiteratureBinary(literatureEntry))
                        .build();

        this.getStoreManager().saveDocs(DataStoreManager.StoreType.LITERATURE, document);
    }

    @Override
    protected String getIdRequestPath() {
        return "/citations/";
    }

    private byte[] getLiteratureBinary(LiteratureEntry entry) {
        try {
            return LiteratureJsonConfig.getInstance()
                    .getFullObjectMapper()
                    .writeValueAsBytes(entry);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse LiteratureEntry to binary json: ", e);
        }
    }

    static class LiteratureGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(String.valueOf(PUBMED_ID))
                    .resultMatcher(jsonPath("$.citation.citationCrossReferences[0].id", is("100")))
                    .resultMatcher(jsonPath("$.citation.authors", contains("The Author")))
                    .resultMatcher(jsonPath("$.citation.title", is("The Title")))
                    .build();
        }

        @Override
        public GetIdParameter invalidIdParameter() {
            return GetIdParameter.builder()
                    .id("INVALID")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "The citation id value should be a PubMedId (number) or start with CI- or start with IND")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("999")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(String.valueOf(PUBMED_ID))
                    .fields("id,title")
                    .resultMatcher(jsonPath("$.citation.id", is("100")))
                    .resultMatcher(jsonPath("$.citation.title", is("The Title")))
                    .resultMatcher(jsonPath("$.citation.authors").doesNotExist())
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder()
                    .id(String.valueOf(PUBMED_ID))
                    .fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }
    }

    static class LiteratureGetIdContentTypeParamResolver
            extends AbstractGetIdContentTypeParamResolver {

        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(String.valueOf(PUBMED_ID))
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.citation.citationCrossReferences[0].id",
                                                    is("100")))
                                    .resultMatcher(
                                            jsonPath("$.citation.authors", contains("The Author")))
                                    .resultMatcher(jsonPath("$.citation.title", is("The Title")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    String.valueOf(PUBMED_ID))))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Citation Id\tTitle\tReference\tAbstract/Summary")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "100\tThe Title\t10(2019)\tliterature abstract")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .build();
        }

        @Override
        public GetIdContentTypeParam idBadRequestContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(null)
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The citation id value should be a PubMedId (number) or start with CI- or start with IND")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .build();
        }
    }
}
