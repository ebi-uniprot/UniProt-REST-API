package org.uniprot.api.subcell.request;

import lombok.Data;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.search.field.SubcellularLocationField;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
public class SubcellularLocationRequestDTO implements SearchRequest {

    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(fieldValidatorClazz = SubcellularLocationField.Search.class, messagePrefix = "search.subcellularLocation")
    private String query;

    @ValidSolrSortFields(sortFieldEnumClazz = SubcellularLocationField.Sort.class)
    private String sort;

    private String cursor;


    @ValidReturnFields(fieldValidatorClazz = SubcellularLocationField.ResultFields.class)
    private String fields;

    @Positive(message = "{search.positive}")
    private int size = DEFAULT_RESULTS_SIZE;

    @Override
    public String getFacets() {
        return "";
    }
}