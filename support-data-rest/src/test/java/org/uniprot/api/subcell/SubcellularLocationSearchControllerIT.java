package org.uniprot.api.subcell;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.DataStoreTestConfig;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractSearchControllerIT;
import org.uniprot.api.rest.controller.SaveScenario;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.support_data.SupportDataApplication;
import org.uniprot.core.cv.subcell.SubcellLocationCategory;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
import org.uniprot.core.cv.subcell.impl.SubcellularLocationEntryImpl;
import org.uniprot.core.json.parser.subcell.SubcellularLocationJsonConfig;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.subcell.SubcellularLocationDocument;
import org.uniprot.store.search.field.SubcellularLocationField;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author lgonzales
 * @since 2019-07-05
 */
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(SubcellularLocationController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            SubcellularLocationSearchControllerIT.SubcellularLocationSearchContentTypeParamResolver
                    .class,
            SubcellularLocationSearchControllerIT.SubcellularLocationSearchParameterResolver.class
        })
public class SubcellularLocationSearchControllerIT extends AbstractSearchControllerIT {

    @Autowired private SubcellularLocationRepository repository;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.SUBCELLULAR_LOCATION;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.subcellularlocation;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getSearchRequestPath() {
        return "/subcellularlocation/search";
    }

    @Override
    protected int getDefaultPageSize() {
        return 25;
    }

    @Override
    protected SearchFieldConfig getSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.SUBCELLLOCATION);
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        String value = "";
        switch (searchField) {
            case "id":
                value = "SL-0001";
                break;
        }
        return value;
    }

    @Override
    protected List<String> getAllFacetFields() {
        return new ArrayList<>();
    }

    @Override
    protected List<String> getAllReturnedFields() {
        return Arrays.stream(SubcellularLocationField.ResultFields.values())
                .map(SubcellularLocationField.ResultFields::name)
                .collect(Collectors.toList());
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        LongStream.rangeClosed(1, numberOfEntries).forEach(i -> saveEntry("SL-000" + i));
    }

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        saveEntry("SL-0001");
        saveEntry("SL-0002");
    }

    private void saveEntry(String accession) {
        SubcellularLocationEntryImpl subcellularLocationEntry = new SubcellularLocationEntryImpl();
        subcellularLocationEntry.setId("Name value " + accession);
        subcellularLocationEntry.setAccession(accession);
        subcellularLocationEntry.setCategory(SubcellLocationCategory.LOCATION);
        subcellularLocationEntry.setDefinition("Definition value " + accession);

        SubcellularLocationDocument document =
                SubcellularLocationDocument.builder()
                        .id(accession)
                        .name("Name value " + accession)
                        .category(SubcellLocationCategory.LOCATION.getCategory())
                        .content(Collections.singletonList("Content value " + accession))
                        .subcellularlocationObj(
                                getSubcellularLocationBinary(subcellularLocationEntry))
                        .build();

        getStoreManager().saveDocs(DataStoreManager.StoreType.SUBCELLULAR_LOCATION, document);
    }

    private ByteBuffer getSubcellularLocationBinary(SubcellularLocationEntry entry) {
        try {
            return ByteBuffer.wrap(
                    SubcellularLocationJsonConfig.getInstance()
                            .getFullObjectMapper()
                            .writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Unable to parse SubcellularLocationEntry to binary json: ", e);
        }
    }

    static class SubcellularLocationSearchParameterResolver
            extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("id:SL-0001"))
                    .resultMatcher(jsonPath("$.results.*.id", contains("Name value SL-0001")))
                    .resultMatcher(jsonPath("$.results.*.accession", contains("SL-0001")))
                    .resultMatcher(jsonPath("$.results.*.category", contains("Cellular component")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.definition", contains("Definition value SL-0001")))
                    .build();
        }

        @Override
        protected SearchParameter searchCanReturnNotFoundParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("id:SL-9999"))
                    .resultMatcher(jsonPath("$.results.size()", is(0)))
                    .build();
        }

        @Override
        protected SearchParameter searchAllowWildcardQueryAllDocumentsParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("definition:*"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.id",
                                    contains("Name value SL-0001", "Name value SL-0002")))
                    .resultMatcher(
                            jsonPath("$.results.*.accession", contains("SL-0001", "SL-0002")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.category",
                                    contains("Cellular component", "Cellular component")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.definition",
                                    contains(
                                            "Definition value SL-0001",
                                            "Definition value SL-0002")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("name:[1 TO 10]"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "'name' filter type 'range' is invalid. Expected 'general' filter type")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("id:INVALID"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    containsInAnyOrder(
                                            "The subcellular location id filter value has invalid format. It should match the regular expression 'SL-[0-9]{4}'")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("name desc"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.id",
                                    contains("Name value SL-0002", "Name value SL-0001")))
                    .resultMatcher(
                            jsonPath("$.results.*.accession", contains("SL-0002", "SL-0001")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.category",
                                    contains("Cellular component", "Cellular component")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.definition",
                                    contains(
                                            "Definition value SL-0002",
                                            "Definition value SL-0001")))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("fields", Collections.singletonList("id,category"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.id",
                                    contains("Name value SL-0001", "Name value SL-0002")))
                    .resultMatcher(jsonPath("$.results.*.accession").doesNotExist())
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.category",
                                    contains("Cellular component", "Cellular component")))
                    .resultMatcher(jsonPath("$.results.*.definition").doesNotExist())
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder().build();
        }
    }

    static class SubcellularLocationSearchContentTypeParamResolver
            extends AbstractSearchContentTypeParamResolver {

        @Override
        protected SearchContentTypeParam searchSuccessContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("id:SL-0001 OR id:SL-0002")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.accession",
                                                    containsInAnyOrder("SL-0001", "SL-0002")))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.id",
                                                    containsInAnyOrder(
                                                            "Name value SL-0001",
                                                            "Name value SL-0002")))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.definition",
                                                    containsInAnyOrder(
                                                            "Definition value SL-0001",
                                                            "Definition value SL-0002")))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.category",
                                                    containsInAnyOrder(
                                                            "Cellular component",
                                                            "Cellular component")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString("SL-0001")))
                                    .resultMatcher(content().string(containsString("SL-0002")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Subcellular location ID\tDescription\tCategory\tAlias")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "SL-0001\tDefinition value SL-0001\tCellular component\tName value SL-0001")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "SL-0002\tDefinition value SL-0002\tCellular component\tName value SL-0002")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.OBO_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.OBO_MEDIA_TYPE))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "format-version: 1.2\n")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "default-namespace: uniprot:locations\n")))
                                    .resultMatcher(content().string(containsString("[Term]\n")))
                                    .resultMatcher(
                                            content().string(containsString("id: SL-0001\n")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "name: Name value SL-0001\n")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "def: \"Definition value SL-0001\" []\n")))
                                    .build())
                    .build();
        }

        @Override
        protected SearchContentTypeParam searchBadRequestContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("id:invalid")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The subcellular location id filter value has invalid format. It should match the regular expression 'SL-[0-9]{4}'")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.OBO_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .build();
        }
    }
}
