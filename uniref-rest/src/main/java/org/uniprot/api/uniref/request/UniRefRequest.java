package org.uniprot.api.uniref.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.*;
import org.uniprot.api.uniref.repository.UniRefFacetConfig;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;
import org.uniprot.store.search.field.UniRefField;
import org.uniprot.store.search.field.UniRefResultFields;

import com.google.common.base.Strings;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Data
public class UniRefRequest implements SearchRequest {
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(uniProtDataType = UniProtDataType.uniref, messagePrefix = "search.uniref")
    private String query;

    @ValidSolrSortFields(uniProtDataType = UniProtDataType.uniref)
    private String sort;

    private String cursor;

    @ValidReturnFields(fieldValidatorClazz = UniRefResultFields.class)
    private String fields;

    @ValidFacets(facetConfig = UniRefFacetConfig.class)
    private String facets;

    @Positive(message = "{search.positive}")
    private Integer size;

    public static final String DEFAULT_FIELDS = "id,name,common_taxon,count,created";

    @Override
    public String getFields() {
        if (Strings.isNullOrEmpty(fields)) {
            fields = DEFAULT_FIELDS;
        } else if (!fields.contains(UniRefField.Return.id.name())) {
            String temp = "id," + fields;
            this.fields = temp;
        }
        return fields;
    }
}
