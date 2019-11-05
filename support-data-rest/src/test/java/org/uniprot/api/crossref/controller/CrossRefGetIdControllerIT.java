package org.uniprot.api.crossref.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.io.IOException;
import java.util.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.DataStoreTestConfig;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.crossref.repository.CrossRefRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.support_data.SupportDataApplication;
import org.uniprot.core.crossref.CrossRefEntry;
import org.uniprot.core.crossref.CrossRefEntryBuilder;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.dbxref.CrossRefDocument;
import org.uniprot.store.search.field.CrossRefField;

import com.fasterxml.jackson.databind.ObjectMapper;

@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(CrossRefController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            CrossRefGetIdControllerIT.CrossRefGetIdParameterResolver.class,
            CrossRefGetIdControllerIT.CrossRefGetIdContentTypeParamResolver.class
        })
public class CrossRefGetIdControllerIT extends AbstractGetByIdControllerIT {

    private static final String ACCESSION = "DB-0104";

    @Autowired private CrossRefRepository repository;

    @Override
    protected String getIdRequestPath() {
        return "/xref/";
    }

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.CROSSREF;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.crossref;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected void saveEntry() {

        CrossRefEntryBuilder entryBuilder = new CrossRefEntryBuilder();
        CrossRefEntry crossRefEntry =
                entryBuilder
                        .accession(ACCESSION)
                        .abbrev("TIGRFAMs")
                        .name("TIGRFAMs; a protein family database")
                        .pubMedId("17151080")
                        .doiId("10.1093/nar/gkl1043")
                        .linkType("Explicit")
                        .server("http://tigrfams.jcvi.org/cgi-bin/index.cgi")
                        .dbUrl("http://tigrfams.jcvi.org/cgi-bin/HmmReportPage.cgi?acc=%s")
                        .category("Family and domain databases")
                        .reviewedProteinCount(10L)
                        .unreviewedProteinCount(5L)
                        .build();

        CrossRefDocument document =
                CrossRefDocument.builder()
                        .accession(crossRefEntry.getAccession())
                        .abbrev(crossRefEntry.getAbbrev())
                        .name(crossRefEntry.getName())
                        .pubMedId(crossRefEntry.getPubMedId())
                        .doiId(crossRefEntry.getDoiId())
                        .linkType(crossRefEntry.getLinkType())
                        .server(crossRefEntry.getServer())
                        .dbUrl(crossRefEntry.getDbUrl())
                        .category(crossRefEntry.getCategory())
                        .reviewedProteinCount(crossRefEntry.getReviewedProteinCount())
                        .unreviewedProteinCount(crossRefEntry.getUnreviewedProteinCount())
                        .build();

        this.getStoreManager().saveDocs(DataStoreManager.StoreType.CROSSREF, document);
    }

    static class CrossRefGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION)
                    .resultMatcher(jsonPath("$.linkType", is("Explicit")))
                    .resultMatcher(
                            jsonPath("$.server", is("http://tigrfams.jcvi.org/cgi-bin/index.cgi")))
                    .resultMatcher(jsonPath("$.unreviewedProteinCount", is(5)))
                    .resultMatcher(jsonPath("$.name", is("TIGRFAMs; a protein family database")))
                    .resultMatcher(
                            jsonPath(
                                    "$.dbUrl",
                                    is(
                                            "http://tigrfams.jcvi.org/cgi-bin/HmmReportPage.cgi?acc=%s")))
                    .resultMatcher(jsonPath("$.pubMedId", is("17151080")))
                    .resultMatcher(jsonPath("$.accession", is(ACCESSION)))
                    .resultMatcher(jsonPath("$.abbrev", is("TIGRFAMs")))
                    .resultMatcher(jsonPath("$.reviewedProteinCount", is(10)))
                    .resultMatcher(jsonPath("$.category", is("Family and domain databases")))
                    .resultMatcher(jsonPath("$.doiId", is("10.1093/nar/gkl1043")))
                    .build();
        }

        @Override
        public GetIdParameter invalidIdParameter() {
            return GetIdParameter.builder()
                    .id("INVALID")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "The cross ref id value should be in the form of DB-XXXX")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("DB-0000")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION)
                    .fields("accession,category,unreviewed_protein_count")
                    .resultMatcher(jsonPath("$.accession", is(ACCESSION)))
                    .resultMatcher(jsonPath("$.category", is("Family and domain databases")))
                    .resultMatcher(jsonPath("$.unreviewedProteinCount", is(5)))
                    .resultMatcher(jsonPath("$.name").doesNotExist())
                    .resultMatcher(jsonPath("$.reviewedProteinCount").doesNotExist())
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION)
                    .fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }

        @Override
        public GetIdParameter withValidResponseFieldsOrderParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION)
                    .resultMatcher(
                            result -> {
                                String contentAsString = result.getResponse().getContentAsString();
                                try {
                                    Map<String, Object> responseMap =
                                            new ObjectMapper()
                                                    .readValue(
                                                            contentAsString, LinkedHashMap.class);
                                    List<String> actualList = new ArrayList<>(responseMap.keySet());
                                    List<String> expectedList = getExpectedFieldsOrder();
                                    Assertions.assertEquals(expectedList.size(), actualList.size());
                                    Assertions.assertEquals(expectedList, actualList);
                                } catch (IOException e) {
                                    Assertions.fail(e.getMessage());
                                }
                            })
                    .build();
        }

        private List<String> getExpectedFieldsOrder() {
            List<String> jsonFieldsOrder = new LinkedList<>();
            jsonFieldsOrder.add(CrossRefField.ResultFields.name.getJavaFieldName());
            jsonFieldsOrder.add(CrossRefField.ResultFields.accession.getJavaFieldName());
            jsonFieldsOrder.add(CrossRefField.ResultFields.abbrev.getJavaFieldName());
            jsonFieldsOrder.add(CrossRefField.ResultFields.pub_med_id.getJavaFieldName());
            jsonFieldsOrder.add(CrossRefField.ResultFields.doi_id.getJavaFieldName());
            jsonFieldsOrder.add(CrossRefField.ResultFields.link_type.getJavaFieldName());
            jsonFieldsOrder.add(CrossRefField.ResultFields.server.getJavaFieldName());
            jsonFieldsOrder.add(CrossRefField.ResultFields.db_url.getJavaFieldName());
            jsonFieldsOrder.add(CrossRefField.ResultFields.category.getJavaFieldName());
            jsonFieldsOrder.add(
                    CrossRefField.ResultFields.reviewed_protein_count.getJavaFieldName());
            jsonFieldsOrder.add(
                    CrossRefField.ResultFields.unreviewed_protein_count.getJavaFieldName());

            return jsonFieldsOrder;
        }
    }

    static class CrossRefGetIdContentTypeParamResolver
            extends AbstractGetIdContentTypeParamResolver {

        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(ACCESSION)
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.linkType", is("Explicit")))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.server",
                                                    is(
                                                            "http://tigrfams.jcvi.org/cgi-bin/index.cgi")))
                                    .resultMatcher(jsonPath("$.unreviewedProteinCount", is(5)))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.name",
                                                    is("TIGRFAMs; a protein family database")))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.dbUrl",
                                                    is(
                                                            "http://tigrfams.jcvi.org/cgi-bin/HmmReportPage.cgi?acc=%s")))
                                    .resultMatcher(jsonPath("$.pubMedId", is("17151080")))
                                    .resultMatcher(jsonPath("$.accession", is(ACCESSION)))
                                    .resultMatcher(jsonPath("$.abbrev", is("TIGRFAMs")))
                                    .resultMatcher(jsonPath("$.reviewedProteinCount", is(10)))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.category",
                                                    is("Family and domain databases")))
                                    .resultMatcher(jsonPath("$.doiId", is("10.1093/nar/gkl1043")))
                                    .build())
                    .build();
        }

        @Override
        public GetIdContentTypeParam idBadRequestContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id("INVALID")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The cross ref id value should be in the form of DB-XXXX")))
                                    .build())
                    .build();
        }
    }
}
