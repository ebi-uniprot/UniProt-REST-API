package uk.ac.ebi.uniprot.disease;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.uniprot.api.DataStoreTestConfig;
import uk.ac.ebi.uniprot.api.disease.DiseaseController;
import uk.ac.ebi.uniprot.api.rest.controller.AbstractSearchWithFacetControllerIT;
import uk.ac.ebi.uniprot.api.rest.controller.SaveScenario;
import uk.ac.ebi.uniprot.api.rest.controller.param.ContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.SearchContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.SearchParameter;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.support_data.SupportDataApplication;
import uk.ac.ebi.uniprot.cv.disease.CrossReference;
import uk.ac.ebi.uniprot.cv.disease.Disease;
import uk.ac.ebi.uniprot.cv.keyword.Keyword;
import uk.ac.ebi.uniprot.cv.keyword.impl.KeywordImpl;
import uk.ac.ebi.uniprot.domain.builder.DiseaseBuilder;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.json.parser.disease.DiseaseJsonConfig;
import uk.ac.ebi.uniprot.json.parser.taxonomy.TaxonomyJsonConfig;
import uk.ac.ebi.uniprot.search.document.disease.DiseaseDocument;
import uk.ac.ebi.uniprot.search.field.DiseaseField;
import uk.ac.ebi.uniprot.search.field.SearchField;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(DiseaseController.class)
@ExtendWith(value = {SpringExtension.class, DiseaseSearchControllerIT.DiseaseSearchContentTypeParamResolver.class,
        DiseaseSearchControllerIT.DiseaseSearchParameterResolver.class})
public class DiseaseSearchControllerIT extends AbstractSearchWithFacetControllerIT {

    private static String SEARCH_ACCESSION1 = "DI-" + ThreadLocalRandom.current().nextLong(10000, 99999);
    private static String SEARCH_ACCESSION2 = "DI-" + ThreadLocalRandom.current().nextLong(10000, 99999);
    private static List<String> SORTED_ACCESSIONS = new ArrayList<>(Arrays.asList(SEARCH_ACCESSION1, SEARCH_ACCESSION2));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStoreManager storeManager;

    @Override
    protected void cleanEntries() {
        this.storeManager.cleanSolr(DataStoreManager.StoreType.DISEASE);
    }

    @Override
    protected MockMvc getMockMvc() {
        return mockMvc;
    }

    @Override
    protected String getSearchRequestPath() {
        return "/disease/search";
    }

    @Override
    protected int getDefaultPageSize() {
        return 25;
    }

    @Override
    protected List<SearchField> getAllSearchFields() {
        return Arrays.asList(DiseaseField.Search.values());
    }

    @Override
    protected String getFieldValueForValidatedField(SearchField searchField) {
        String value = "";
        if("accession".equalsIgnoreCase(searchField.getName())){
            return SEARCH_ACCESSION1;
        }
        return value;
    }

    @Override
    protected List<String> getAllSortFields() {
        return Arrays.stream(DiseaseField.Sort.values())
                .map(DiseaseField.Sort::name)
                .collect(Collectors.toList());
    }

    @Override
    protected List<String> getAllFacetFields() {
        return new ArrayList<>();
    }

    @Override
    protected List<String> getAllReturnedFields() {
        return Arrays.stream(DiseaseField.ResultFields.values())
                .map(DiseaseField.ResultFields::name)
                .collect(Collectors.toList());
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        LongStream.rangeClosed(1, numberOfEntries).forEach(i -> saveEntry(i));
    }

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        saveEntry(SEARCH_ACCESSION1, 10);
        saveEntry(SEARCH_ACCESSION2, 20);
    }

    private void saveEntry(long suffix) {

        String accPrefix = "DI-";
        long num = ThreadLocalRandom.current().nextLong(10000, 99999);
        String accession = accPrefix + num;
        saveEntry(accession, suffix);

    }

    private void saveEntry(String accession, long suffix) {
        DiseaseBuilder diseaseBuilder = new DiseaseBuilder();
        Keyword keyword = new KeywordImpl("Mental retardation" + suffix, "KW-0991" + suffix);
        CrossReference xref1 = new CrossReference("MIM" + suffix, "617140" + suffix, Arrays.asList("phenotype" + suffix));
        CrossReference xref2 = new CrossReference("MedGen" + suffix, "CN238690" + suffix);
        CrossReference xref3 = new CrossReference("MeSH" + suffix, "D000015" + suffix);
        CrossReference xref4 = new CrossReference("MeSH" + suffix, "D008607" + suffix);
        Disease diseaseEntry = diseaseBuilder.id("ZTTK syndrome" + suffix)
                .accession(accession)
                .acronym("ZTTKS" + suffix)
                .definition("An autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.")
                .alternativeNames(Arrays.asList("Zhu-Tokita-Takenouchi-Kim syndrome", "ZTTK multiple congenital anomalies-mental retardation syndrome"))
                .crossReferences(Arrays.asList(xref1, xref2, xref3, xref4))
                .keywords(keyword)
                .reviewedProteinCount(suffix)
                .unreviewedProteinCount(suffix)
                .build();

        List<String> kwIds;
        if (diseaseEntry.getKeywords() != null) {
            kwIds = diseaseEntry.getKeywords().stream().map(kw -> kw.getId()).collect(Collectors.toList());
        } else {
            kwIds = new ArrayList<>();
        }
        // name is a combination of id, acronym, definition, synonyms, keywords
        List<String> name = Stream.concat(Stream.concat(Stream.of(diseaseEntry.getId(), diseaseEntry.getAcronym(), diseaseEntry.getDefinition()),
                kwIds.stream()),
                diseaseEntry.getAlternativeNames().stream())
                .collect(Collectors.toList());
        // content is name + accession
        List<String> content = new ArrayList<>();
        content.addAll(name);
        content.add(diseaseEntry.getAccession());
        DiseaseDocument document = DiseaseDocument.builder()
                .accession(accession)
                .name(name)
                .content(content)
                .diseaseObj(getDiseaseBinary(diseaseEntry))
                .build();


        this.storeManager.saveDocs(DataStoreManager.StoreType.DISEASE, document);
    }

    private ByteBuffer getDiseaseBinary(Disease entry) {
        try {
            return ByteBuffer.wrap(DiseaseJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse Disease entry to binary json: ", e);
        }
    }

    @Override
    @Test
    protected void searchFacetsWithIncorrectValuesReturnBadRequest() {
        // do nothing.. disease doesn't have any facets
    }

    @Override
    @Test
    protected void searchCanSearchWithAllAvailableFacetsFields(){
        // do nothing.. disease doesn't have any facets
    }

    static class DiseaseSearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("accession:" + SEARCH_ACCESSION1))
                    .resultMatcher(jsonPath("$.results.*.accession", contains(SEARCH_ACCESSION1)))
                    .resultMatcher(jsonPath("$.results.length()", is(1)))
                    .build();
        }

        @Override
        protected SearchParameter searchCanReturnNotFoundParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("accession:DI-00000"))
                    .resultMatcher(jsonPath("$.results.size()", is(0)))
                    .build();
        }

        @Override
        protected SearchParameter searchAllowWildcardQueryAllDocumentsParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("accession:*"))
                    .resultMatcher(jsonPath("$.results.*.accession", containsInAnyOrder(SEARCH_ACCESSION1, SEARCH_ACCESSION2)))
                    .resultMatcher(jsonPath("$.results.size()", is(2)))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("name:[1 TO 10]"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("'name' filter type 'range' is invalid. Expected 'term' filter type")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("accession:[INVALID to INVALID123] OR name:123"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", containsInAnyOrder(
                            "query parameter has an invalid syntax")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            Collections.sort(SORTED_ACCESSIONS);
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("accession asc"))
                    .resultMatcher(jsonPath("$.results.*.accession", contains(SORTED_ACCESSIONS.get(0), SORTED_ACCESSIONS.get(1))))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("fields", Collections.singletonList("accession,id"))
                    .resultMatcher(jsonPath("$.results.*.accession", containsInAnyOrder(SEARCH_ACCESSION1, SEARCH_ACCESSION2)))
                    .resultMatcher(jsonPath("$.results.*.reviewedProteinCount").doesNotExist())
                    .resultMatcher(jsonPath("$.results.*.id", notNullValue()))
                    .build();
        }

        @Override // duplicate test to satisfy the parent test
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("facets", Collections.singletonList("reviewed"))
                    .queryParam("fields", Collections.singletonList("accession,id"))
                    .resultMatcher(jsonPath("$.results.*.accession", containsInAnyOrder(SEARCH_ACCESSION1, SEARCH_ACCESSION2)))
                    .resultMatcher(jsonPath("$.results.*.reviewedProteinCount").doesNotExist())
                    .resultMatcher(jsonPath("$.results.*.id", notNullValue()))
                    .build();
        }
    }


    static class DiseaseSearchContentTypeParamResolver extends AbstractSearchContentTypeParamResolver {

        @Override
        protected SearchContentTypeParam searchSuccessContentTypesParam() {
            String fmtStr = "format-version: 1.2";
            String defaultNSStr = "default-namespace: uniprot:diseases";
            String termStr = "name: ZTTK syndrome20\n" +
                    "def: \"An autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.\" []\n" +
                    "synonym: \"Zhu-Tokita-Takenouchi-Kim syndrome\" [UniProt]\n" +
                    "synonym: \"ZTTK multiple congenital anomalies-mental retardation syndrome\" [UniProt]\n" +
                    "xref: MedGen20:CN23869020\n" +
                    "xref: MeSH20:D00001520\n" +
                    "xref: MeSH20:D00860720\n" +
                    "xref: MIM20:61714020 \"phenotype20\"";

            return SearchContentTypeParam.builder()
                    .query("accession:" + SEARCH_ACCESSION1 + " OR accession:" + SEARCH_ACCESSION2)
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(MediaType.APPLICATION_JSON)
                            .resultMatcher(jsonPath("$.results.*.accession", containsInAnyOrder(SEARCH_ACCESSION1, SEARCH_ACCESSION2)))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                            .resultMatcher(content().string(containsString("ZTTK syndrome10")))
                            .resultMatcher(content().string(containsString("ZTTK syndrome20")))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                            .resultMatcher(content().string(containsString("Name\tDisease ID\tMnemonic\tDescription")))
                            .resultMatcher(content().string(containsString("ZTTK syndrome20\t" + SEARCH_ACCESSION2 + "\tZTTKS20\tAn autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.")))
                            .resultMatcher(content().string(containsString("ZTTK syndrome10\t" + SEARCH_ACCESSION1 + "\tZTTKS10\tAn autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.")))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                            .resultMatcher(content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.OBO_MEDIA_TYPE)
                            .resultMatcher(content().contentType(UniProtMediaType.OBO_MEDIA_TYPE))
                            .resultMatcher(content().string(containsString(fmtStr)))
                            .resultMatcher(content().string(containsString(defaultNSStr)))
                            .resultMatcher(content().string(containsString(termStr)))
                            .build())
                    .build();
        }

        @Override
        protected SearchContentTypeParam searchBadRequestContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("random_field:invalid")
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(MediaType.APPLICATION_JSON)
                            .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                            .resultMatcher(jsonPath("$.messages.*", contains("'random_field' is not a valid search field")))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                            .resultMatcher(content().string(isEmptyString()))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                            .resultMatcher(content().string(isEmptyString()))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                            .resultMatcher(content().string(isEmptyString()))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.OBO_MEDIA_TYPE)
                            .resultMatcher(content().string(isEmptyString()))
                            .build())
                    .build();
        }
    }
}
