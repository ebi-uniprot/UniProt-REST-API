package org.uniprot.api.proteome.controller;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.proteome.ProteomeRestApplication;
import org.uniprot.api.proteome.repository.GeneCentricFacetConfig;
import org.uniprot.api.proteome.repository.GeneCentricQueryRepository;
import org.uniprot.api.rest.controller.AbstractSearchControllerIT;
import org.uniprot.api.rest.controller.SaveScenario;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.core.json.parser.proteome.ProteomeJsonConfig;
import org.uniprot.core.proteome.CanonicalProtein;
import org.uniprot.core.proteome.Protein;
import org.uniprot.core.proteome.builder.CanonicalProteinBuilder;
import org.uniprot.core.proteome.builder.ProteinBuilder;
import org.uniprot.core.uniprot.UniProtEntryType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.proteome.GeneCentricDocument;
import org.uniprot.store.search.document.proteome.GeneCentricDocument.GeneCentricDocumentBuilder;
import org.uniprot.store.search.field.GeneCentricField;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author jluo
 * @date: 17 Jun 2019
 */
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            ProteomeRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(GeneCentricController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            GeneCentricSearchControllerIT.GeneCentricSearchParameterResolver.class,
            GeneCentricSearchControllerIT.GeneCentricSearchContentTypeParamResolver.class
        })
public class GeneCentricSearchControllerIT extends AbstractSearchControllerIT {
    private static final String ACCESSION_PREF = "P00";
    private static final String RELATED_ACCESSION_PREF_1 = "P20";
    private static final String RELATED_ACCESSION_PREF_2 = "P30";
    private static final String UPID = "UP000005640";
    private static final int TAX_ID = 9606;

    @Autowired private GeneCentricQueryRepository repository;

    @Autowired private GeneCentricFacetConfig facetConfig;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.GENECENTRIC;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.genecentric;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getSearchRequestPath() {
        return "/genecentric/search";
    }

    @Override
    protected int getDefaultPageSize() {
        return 25;
    }

    @Override
    protected SearchFieldConfig getSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.genecentric);
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        String value = "";
        switch (searchField) {
            case "accession_id":
            case "accession":
                value = ACCESSION_PREF + 123;
                break;
            case "upid":
                value = UPID;
                break;
            case "organism_id":
                value = "9606";
                break;
            case "length":
                value = "30";
                break;
            case "active":
                value = "true";
                break;
        }
        return value;
    }

    @Override
    protected List<String> getAllFacetFields() {
        return new ArrayList<>(facetConfig.getFacetNames());
    }

    @Override
    protected List<String> getAllReturnedFields() {
        return Arrays.stream(GeneCentricField.ResultFields.values())
                .map(GeneCentricField.ResultFields::name)
                .collect(Collectors.toList());
    }

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        saveEntry(123);
        saveEntry(124);
    }

    private void saveEntry(int i) {
        GeneCentricDocument doc = createDocument(i);
        getStoreManager().saveDocs(DataStoreManager.StoreType.GENECENTRIC, doc);
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        IntStream.rangeClosed(1, numberOfEntries).forEach(this::saveEntry);
    }

    private String getName(String prefix, int i) {
        if (i < 10) {
            return prefix + "00" + i;
        } else if (i < 100) {
            return prefix + "0" + i;
        } else return prefix + i;
    }

    private GeneCentricDocument createDocument(int i) {
        CanonicalProtein protein = createProtein(i);
        return convert(protein);
    }

    private GeneCentricDocument convert(CanonicalProtein protein) {
        GeneCentricDocumentBuilder builder = GeneCentricDocument.builder();
        List<String> accessions = new ArrayList<>();
        accessions.add(protein.getCanonicalProtein().getAccession().getValue());
        protein.getRelatedProteins().stream()
                .map(val -> val.getAccession().getValue())
                .forEach(accessions::add);
        List<String> genes = new ArrayList<>();
        genes.add(protein.getCanonicalProtein().getGeneName());
        protein.getRelatedProteins().stream().map(Protein::getGeneName).forEach(genes::add);

        builder.accession(protein.getCanonicalProtein().getAccession().getValue())
                .accessions(accessions)
                .geneNames(genes)
                .reviewed(
                        protein.getCanonicalProtein().getEntryType() == UniProtEntryType.SWISSPROT)
                .upid(UPID)
                .organismTaxId(TAX_ID);
        builder.geneCentricStored(getBinary(protein));
        return builder.build();
    }

    private CanonicalProtein createProtein(int i) {
        Protein protein =
                new ProteinBuilder()
                        .accession(getName(ACCESSION_PREF, i))
                        .entryType(UniProtEntryType.SWISSPROT)
                        .geneName(getName("gene", i))
                        .geneNameType(org.uniprot.core.proteome.GeneNameType.ENSEMBL)
                        .sequenceLength(324)
                        .build();

        Protein protein2 =
                new ProteinBuilder()
                        .accession(getName(RELATED_ACCESSION_PREF_1, i))
                        .entryType(UniProtEntryType.SWISSPROT)
                        .geneName(getName("agene", i))
                        .geneNameType(org.uniprot.core.proteome.GeneNameType.ENSEMBL)
                        .sequenceLength(334)
                        .build();
        Protein protein3 =
                new ProteinBuilder()
                        .accession(getName(RELATED_ACCESSION_PREF_2, i))
                        .entryType(UniProtEntryType.TREMBL)
                        .geneName(getName("twogene", i))
                        .geneNameType(org.uniprot.core.proteome.GeneNameType.OLN)
                        .sequenceLength(434)
                        .build();
        CanonicalProteinBuilder builder = new CanonicalProteinBuilder();

        return builder.canonicalProtein(protein)
                .relatedProteinsAdd(protein2)
                .relatedProteinsAdd(protein3)
                .build();
    }

    private ByteBuffer getBinary(CanonicalProtein entry) {
        try {
            return ByteBuffer.wrap(
                    ProteomeJsonConfig.getInstance()
                            .getFullObjectMapper()
                            .writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse TaxonomyEntry to binary json: ", e);
        }
    }

    static class GeneCentricSearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("gene:*"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*..accession.value",
                                    contains(
                                            "P00123", "P20123", "P30123", "P00124", "P20124",
                                            "P30124")))
                    .build();
        }

        @Override
        protected SearchParameter searchCanReturnNotFoundParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("upid:UP000004231"))
                    .resultMatcher(jsonPath("$.results.size()", is(0)))
                    .build();
        }

        @Override
        protected SearchParameter searchAllowWildcardQueryAllDocumentsParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("gene:*"))
                    .resultMatcher(
                            jsonPath("$.results.*..accession.value", hasItems("P00123", "P00124")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("gene:[1 TO 10]"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "'gene' filter type 'range' is invalid. Expected 'general' filter type")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam(
                            "query",
                            Collections.singletonList(
                                    "upid:INVALID OR organism_id:INVALID " + "OR reviewed:invalid"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    containsInAnyOrder(
                                            "The 'upid' value has invalid format. It should be a valid Proteome UPID",
                                            "The organism id filter value should be a number",
                                            "The reviewed id filter value should be a true or false")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("accession_id asc"))
                    .resultMatcher(
                            jsonPath("$.results.*..accession.value", hasItems("P00123", "P00124")))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("fields", Collections.singletonList("accession_id"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*..accession.value",
                                    contains(
                                            "P00123", "P20123", "P30123", "P00124", "P20124",
                                            "P30124")))

                    //
                    // .resultMatcher(jsonPath("$.results.*.description",contains("Description231","Description520")))
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("reviewed:true"))
                    .queryParam("facets", Collections.singletonList("reviewed"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*..accession.value",
                                    contains(
                                            "P00123", "P20123", "P30123", "P00124", "P20124",
                                            "P30124")))
                    .build();
        }
    }

    static class GeneCentricSearchContentTypeParamResolver
            extends AbstractSearchContentTypeParamResolver {

        @Override
        protected SearchContentTypeParam searchSuccessContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("organism_id:9606")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*..accession.value",
                                                    hasItems("P00123", "P00124")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(content().string(containsString("P00123")))
                                    .resultMatcher(content().string(containsString("P00124")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString("P00123")))
                                    .resultMatcher(content().string(containsString("P00124")))
                                    .build())
                    //	                    .contentTypeParam(ContentTypeParam.builder()
                    //	                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                    //
                    // .resultMatcher(content().string(containsString("\tOrganism\tOrganism
                    // ID\tProtein count")))
                    //
                    // .resultMatcher(content().string(containsString("UP000005231\tHomo
                    // sapiens\t9606\t0")))
                    //
                    // .resultMatcher(content().string(containsString("UP000005520\tHomo
                    // sapiens\t9606\t0")))
                    //	                            .build())
                    //	                    .contentTypeParam(ContentTypeParam.builder()
                    //	                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                    //
                    // .resultMatcher(content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                    //	                            .build())
                    .build();
        }

        @Override
        protected SearchContentTypeParam searchBadRequestContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("upid:invalid")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The 'upid' value has invalid format. It should be a valid Proteome UPID")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The 'upid' value has invalid format. It should be a valid Proteome UPID")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    //	                    .contentTypeParam(ContentTypeParam.builder()
                    //	                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                    //	                            .resultMatcher(content().string(isEmptyString()))
                    //	                            .build())
                    //	                    .contentTypeParam(ContentTypeParam.builder()
                    //	                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                    //	                            .resultMatcher(content().string(isEmptyString()))
                    //	                            .build())
                    .build();
        }
    }
}
