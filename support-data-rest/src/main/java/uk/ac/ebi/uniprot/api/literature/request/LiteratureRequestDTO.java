package uk.ac.ebi.uniprot.api.literature.request;

import lombok.Data;
import uk.ac.ebi.uniprot.api.rest.request.SearchRequest;
import uk.ac.ebi.uniprot.api.rest.validation.ValidReturnFields;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrQueryFields;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrSortFields;
import uk.ac.ebi.uniprot.search.field.LiteratureField;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Data
public class LiteratureRequestDTO implements SearchRequest {

    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")

    @ValidSolrQueryFields(fieldValidatorClazz = LiteratureField.Search.class, messagePrefix = "search.literature")
    private String query;

    @ValidSolrSortFields(sortFieldEnumClazz = LiteratureField.Sort.class)
    private String sort;

    private String cursor;


    @ValidReturnFields(fieldValidatorClazz = LiteratureField.ResultFields.class)
    private String fields;

    @Positive(message = "{search.positive}")
    private int size = DEFAULT_RESULTS_SIZE;

    @Override
    public String getFacets() {
        return "";
    }
}