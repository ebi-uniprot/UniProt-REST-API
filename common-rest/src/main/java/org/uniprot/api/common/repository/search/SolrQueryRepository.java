package org.uniprot.api.common.repository.search;


import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CursorMarkParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.solr.core.SolrCallback;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.result.Cursor;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetResponseConverter;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.search.term.TermInfo;
import org.uniprot.api.common.repository.search.term.TermInfoConverter;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.SolrCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Solr Basic Repository class to enable the execution of dynamically build queries in a solr collections.
 * <p>
 * It was defined a common QueryResult object in order to be able to
 *
 * @param <T> Returned Solr entity
 * @author lgonzales
 */
public abstract class SolrQueryRepository<T> {
    private static final Integer DEFAULT_PAGE_SIZE = 100;
    private static final Logger LOGGER = LoggerFactory.getLogger(SolrQueryRepository.class);
    private final TermInfoConverter termInfoConverter;
    private final SolrRequestConverter requestConverter;

    private SolrTemplate solrTemplate;
    private SolrCollection collection;
    private Class<T> tClass;
    private FacetResponseConverter facetConverter;

    protected SolrQueryRepository(SolrTemplate solrTemplate, SolrCollection collection, Class<T> tClass, FacetConfig facetConfig, SolrRequestConverter requestConverter) {
        this.solrTemplate = solrTemplate;
        this.collection = collection;
        this.tClass = tClass;
        this.facetConverter = new FacetResponseConverter(facetConfig);
        this.requestConverter = requestConverter;
        this.termInfoConverter = new TermInfoConverter();
    }

    public QueryResult<T> searchPage(SolrRequest request, String cursor, Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        try {
            CursorPage page = CursorPage.of(cursor, pageSize);
            QueryResponse solrResponse = solrTemplate
                    .execute(getSolrCursorCallback(request, page.getCursor(), pageSize));

            List<T> resultList = solrTemplate.convertQueryResponseToBeans(solrResponse, tClass);
            page.setNextCursor(solrResponse.getNextCursorMark());
            page.setTotalElements(solrResponse.getResults().getNumFound());

            List<Facet> facets = new ArrayList<>();
            if (Utils.nonNull(facetConverter)) {
                facets = facetConverter.convert(solrResponse);
            }
            List<TermInfo> termInfos = termInfoConverter.convert(solrResponse);

            return QueryResult.of(resultList, page, facets, termInfos);
        } catch (Throwable e) {
            if (e.getCause() instanceof InvalidRequestException) {
                throw (InvalidRequestException)e.getCause();
            }
            throw new QueryRetrievalException("Unexpected error retrieving data from our Repository", e);
        } finally {
            logSolrQuery(request);
        }
    }

    public Optional<T> getEntry(SolrRequest request) {
        try {
            return solrTemplate.queryForObject(collection.toString(), requestConverter.toQuery(request), tClass);
        } catch (Throwable e) {
            throw new QueryRetrievalException("Error executing solr query", e);
        } finally {
            logSolrQuery(request);
        }
    }

    public Cursor<T> getAll(SolrRequest request) {
        try {
            return solrTemplate.queryForCursor(collection.toString(), requestConverter.toQuery(request), tClass);
        } catch (Throwable e) {
            throw new RuntimeException("Error executing solr query", e);
        } finally {
            logSolrQuery(request);
        }
    }

    private SolrCallback<QueryResponse> getSolrCursorCallback(SolrRequest request, String cursor, Integer pageSize) {
        return solrClient -> {
            SolrQuery solrQuery = requestConverter.toSolrQuery(request);
            if (cursor != null && !cursor.isEmpty()) {
                solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursor);
            } else {
                solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, CursorMarkParams.CURSOR_MARK_START);
            }
            solrQuery.setRows(pageSize);

            return solrClient.query(collection.toString(), solrQuery);
        };
    }

    private void logSolrQuery(SolrRequest request) {
        if (request != null) {
            LOGGER.debug("SolrRequest: {}", request);
        }
    }
}