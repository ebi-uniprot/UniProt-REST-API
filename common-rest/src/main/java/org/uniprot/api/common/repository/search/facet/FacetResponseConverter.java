package org.uniprot.api.common.repository.search.facet;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.IntervalFacet;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.uniprot.core.util.Utils;

/**
 * This interface is responsible to convert QueryResponse facets to a List<Facet> response model.
 *
 * <p>During the conversion it also add configuration label/properties from facet.property
 *
 * @author lgonzales
 */
public class FacetResponseConverter extends FacetConverter<QueryResponse, List<Facet>> {

    private FacetConfig facetConfig;

    public FacetResponseConverter(FacetConfig facetConfig) {
        this.facetConfig = facetConfig;
    }

    @Override
    protected FacetConfig getFacetConfig() {
        return this.facetConfig;
    }

    /**
     * This method is responsible to convert QueryResponse facets to a List<Facet> response model,
     * adding configured labels and properties
     *
     * @param queryResponse Solr query Response
     * @return List of Facet converted and configured.
     */
    @Override
    public List<Facet> convert(QueryResponse queryResponse) {
        List<Facet> facetResult = new ArrayList<>();
        if (Utils.notNullNotEmpty(queryResponse.getFacetFields())) {
            for (FacetField facetField : queryResponse.getFacetFields()) {
                // Iterating over all Query response Facets
                facetResult.add(convertFieldFacets(facetField));
            }
        }
        if (Utils.notNullNotEmpty(queryResponse.getIntervalFacets())) {
            for (IntervalFacet intervalFacet : queryResponse.getIntervalFacets()) {
                facetResult.add(convertIntervalFacets(intervalFacet));
            }
        }
        return facetResult;
    }

    /**
     * This method is responsible to convert Solr Interval facet to a Facet response model, adding
     * configured labels and properties
     *
     * @param intervalFacet interval facet returned from Solr
     * @return converted facet
     */
    private Facet convertIntervalFacets(IntervalFacet intervalFacet) {
        // Iterating over all Query response Interval Facets
        List<FacetItem> values = new ArrayList<>();
        if (Utils.notNullNotEmpty(intervalFacet.getIntervals())) {
            for (IntervalFacet.Count count : intervalFacet.getIntervals()) {
                // Iterating over all query response interval facet items
                if (count != null) {
                    String queryTerm = count.getKey().replace(",", " TO ");
                    // Adding add Facet Item to facet item list
                    values.add(
                            FacetItem.builder()
                                    .value(queryTerm)
                                    .label(
                                            getIntervalFacetItemLabel(
                                                    intervalFacet.getField(), count.getKey()))
                                    .count((long) count.getCount())
                                    .build());
                }
            }
        }
        // return an Interval facet
        return Facet.builder()
                .name(intervalFacet.getField())
                .label(getFacetLabel(intervalFacet.getField()))
                .allowMultipleSelection(allowMultipleSelection(intervalFacet.getField()))
                .values(values)
                .build();
    }

    /**
     * This method is responsible to convert Solr Field facet to a Facet response model, adding
     * configured labels and properties
     *
     * @param facetField Facet Field returned from Solr
     * @return converted field facet
     */
    private Facet convertFieldFacets(FacetField facetField) {
        List<FacetItem> values = new ArrayList<>();
        if (Utils.notNullNotEmpty(facetField.getValues())) {
            for (FacetField.Count count : facetField.getValues()) {
                // Iterating over all query response facet items
                if (count != null) {
                    // Adding add Facet Item to facet item list
                    values.add(
                            FacetItem.builder()
                                    .value(count.getName())
                                    .label(getFacetItemLabel(facetField.getName(), count.getName()))
                                    .count(count.getCount())
                                    .build());
                }
            }
        }
        // build a facet
        return Facet.builder()
                .name(facetField.getName())
                .label(getFacetLabel(facetField.getName()))
                .allowMultipleSelection(allowMultipleSelection(facetField.getName()))
                .values(values)
                .build();
    }
}
