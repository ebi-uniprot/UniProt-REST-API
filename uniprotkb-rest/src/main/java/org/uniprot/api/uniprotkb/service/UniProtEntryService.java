package org.uniprot.api.uniprotkb.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.rest.output.converter.OutputFieldsParser;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.api.rest.service.StoreStreamerSearchService;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.rest.service.query.config.UniProtSolrQueryConfig;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBSearchRequest;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBStreamRequest;
import org.uniprot.api.uniprotkb.repository.search.impl.UniProtTermsConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.returnfield.model.ReturnField;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.SolrQueryUtil;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

@Service
@Import(UniProtSolrQueryConfig.class)
public class UniProtEntryService
        extends StoreStreamerSearchService<UniProtDocument, UniProtKBEntry> {
    public static final String ACCESSION = "accession_id";
    private final UniProtEntryQueryResultsConverter resultsConverter;
    private final SolrQueryConfig solrQueryConfig;
    private final UniProtTermsConfig uniProtTermsConfig;
    private final UniprotQueryRepository repository;
    private final SearchFieldConfig searchFieldConfig;
    private final ReturnFieldConfig returnFieldConfig;
    private final QueryProcessor queryProcessor;
    private final RDFStreamer uniProtRDFStreamer;

    public UniProtEntryService(
            UniprotQueryRepository repository,
            UniProtKBFacetConfig uniprotKBFacetConfig,
            UniProtTermsConfig uniProtTermsConfig,
            UniProtSolrSortClause uniProtSolrSortClause,
            SolrQueryConfig uniProtKBSolrQueryConf,
            UniProtKBStoreClient entryStore,
            StoreStreamer<UniProtKBEntry> uniProtEntryStoreStreamer,
            TaxonomyService taxService,
            FacetTupleStreamTemplate facetTupleStreamTemplate,
            QueryProcessor uniProtKBQueryProcessor,
            SearchFieldConfig uniProtKBSearchFieldConfig,
            RDFStreamer uniProtRDFStreamer) {
        super(
                repository,
                uniprotKBFacetConfig,
                uniProtSolrSortClause,
                uniProtEntryStoreStreamer,
                uniProtKBSolrQueryConf,
                facetTupleStreamTemplate);
        this.repository = repository;
        this.uniProtTermsConfig = uniProtTermsConfig;
        this.solrQueryConfig = uniProtKBSolrQueryConf;
        this.resultsConverter = new UniProtEntryQueryResultsConverter(entryStore, taxService);
        this.searchFieldConfig = uniProtKBSearchFieldConfig;
        this.returnFieldConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB);
        this.queryProcessor = uniProtKBQueryProcessor;
        this.uniProtRDFStreamer = uniProtRDFStreamer;
    }

    @Override
    public QueryResult<UniProtKBEntry> search(SearchRequest request) {

        SolrRequest solrRequest = createSearchSolrRequest(request);

        QueryResult<UniProtDocument> results =
                repository.searchPage(solrRequest, request.getCursor());
        List<ReturnField> fields = OutputFieldsParser.parse(request.getFields(), returnFieldConfig);
        return resultsConverter.convertQueryResult(results, fields);
    }

    @Override
    public UniProtKBEntry findByUniqueId(String accession) {
        return findByUniqueId(accession, null);
    }

    @Override
    protected SearchFieldItem getIdField() {
        return searchFieldConfig.getSearchFieldItemByName(ACCESSION);
    }

    @Override
    public UniProtKBEntry findByUniqueId(String accession, String fields) {
        try {
            List<ReturnField> fieldList = OutputFieldsParser.parse(fields, returnFieldConfig);
            SolrRequest solrRequest =
                    SolrRequest.builder()
                            .query(ACCESSION + ":" + accession.toUpperCase())
                            .rows(NumberUtils.INTEGER_ONE)
                            .build();
            Optional<UniProtDocument> optionalDoc = repository.getEntry(solrRequest);
            Optional<UniProtKBEntry> optionalUniProtEntry =
                    optionalDoc
                            .map(doc -> resultsConverter.convertDoc(doc, fieldList))
                            .orElseThrow(() -> new ResourceNotFoundException("{search.not.found}"));

            return optionalUniProtEntry.orElseThrow(
                    () -> new ResourceNotFoundException("{search.not.found}"));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get accession for: [" + accession + "]";
            throw new ServiceException(message, e);
        }
    }

    public Stream<String> streamRDF(UniProtKBStreamRequest streamRequest) {
        SolrRequest solrRequest =
                createSolrRequestBuilder(streamRequest, solrSortClause, solrQueryConfig).build();
        return this.uniProtRDFStreamer.idsToRDFStoreStream(solrRequest);
    }

    @Override
    protected QueryProcessor getQueryProcessor() {
        return queryProcessor;
    }

    @Override
    public SolrRequest createDownloadSolrRequest(StreamRequest request) {
        UniProtKBStreamRequest uniProtRequest = (UniProtKBStreamRequest) request;
        SolrRequest solrRequest = super.createDownloadSolrRequest(request);
        if (needsToFilterIsoform(uniProtRequest.getQuery(), uniProtRequest.isIncludeIsoform())) {
            addIsoformFilter(solrRequest);
        }
        return solrRequest;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPROTKB;
    }

    @Override
    protected String getSolrIdField() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB)
                .getSearchFieldItemByName(ACCESSION)
                .getFieldName();
    }

    @Override
    public SolrRequest createSearchSolrRequest(SearchRequest request) {

        UniProtKBSearchRequest uniProtRequest = (UniProtKBSearchRequest) request;

        if (needToFilterActiveEntries(uniProtRequest)) {
            uniProtRequest.setQuery(getQueryFieldName("active") + ":" + true);
        }

        // fill the common params from the basic service class
        SolrRequest solrRequest = super.createSearchSolrRequest(uniProtRequest);

        // uniprotkb related stuff
        solrRequest.setQueryConfig(solrQueryConfig);

        if (needsToFilterIsoform(uniProtRequest.getQuery(), uniProtRequest.isIncludeIsoform())) {
            addIsoformFilter(solrRequest);
        }

        if (uniProtRequest.isShowMatchedFields()) {
            solrRequest.setTermQuery(uniProtRequest.getQuery());
            List<String> termFields = new ArrayList<>(uniProtTermsConfig.getFields());
            solrRequest.setTermFields(termFields);
        }

        return solrRequest;
    }

    private void addIsoformFilter(SolrRequest solrRequest) {
        List<String> queries = new ArrayList<>(solrRequest.getFilterQueries());
        queries.add(getQueryFieldName("is_isoform") + ":" + false);
        solrRequest.setFilterQueries(queries);
    }

    /**
     * This method verify if we need to add isoform filter query to remove isoform entries
     *
     * <p>if does not have id fields (we can not filter isoforms when querying for IDS) AND has
     * includeIsoform params in the request URL Then we analyze the includeIsoform request
     * parameter. IMPORTANT: Implementing this way, query search has precedence over isoform request
     * parameter
     *
     * @return true if we need to add isoform filter query
     */
    private boolean needsToFilterIsoform(String query, boolean isIncludeIsoform) {
        boolean hasIdFieldTerms =
                SolrQueryUtil.hasFieldTerms(
                        query,
                        getQueryFieldName(ACCESSION),
                        getQueryFieldName("id"),
                        getQueryFieldName("is_isoform"));

        if (!hasIdFieldTerms) {
            return !isIncludeIsoform;
        } else {
            return false;
        }
    }

    private boolean needToFilterActiveEntries(UniProtKBSearchRequest uniProtRequest) {
        return "*".equals(uniProtRequest.getQuery().trim())
                || "*:*".equals(uniProtRequest.getQuery().trim())
                || SolrQueryUtil.hasNegativeTerm(uniProtRequest.getQuery());
    }

    private String getQueryFieldName(String active) {
        return searchFieldConfig.getSearchFieldItemByName(active).getFieldName();
    }
}
