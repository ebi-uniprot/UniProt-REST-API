package org.uniprot.api.common.repository.solrstream;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Represents a request object containing the details to create a Solr streaming expressions for a
 * facets function call and/or search function
 *
 * @author sahmad
 */
@Getter
public class SolrStreamFacetRequest {
    private static final Integer BUCKET_SIZE = 1000;
    private static final String BUCKET_SORTS = "count(*) desc";
    private static final String METRICS = "count(*)";
    private String query;
    private List<String> facets;
    private String bucketSorts; // comma separated list of sorts
    private String metrics; // comma separated list of metrics to compute for buckets
    private Integer bucketSizeLimit; // the number of facets/buckets
    private boolean searchAccession;
    // fields related to search function. we need this when user wants to filter by facet value(s)
    private String searchFieldList = "accession_id";
    private String searchSort = "accession_id asc";
    private String requestHandler = "/export";

    @Builder
    SolrStreamFacetRequest(String query, List<String> facets, boolean searchAccession) {
        this.query = query;
        this.facets = facets;
        this.searchAccession = searchAccession;
        this.bucketSizeLimit = BUCKET_SIZE;
        this.bucketSorts = BUCKET_SORTS;
        this.metrics = METRICS;
    }
}
