package uk.ac.ebi.uniprot.uuw.advanced.search.model.request;



import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

/**
 * Search request Entity
 *
 * @author lgonzales
 */
@Data
public class QuerySearchRequest {

    @NotNull(message = "{uk.ac.ebi.uniprot.uuw.advanced.search.required}")
    private String query;

    @PositiveOrZero(message = "{uk.ac.ebi.uniprot.uuw.advanced.search.positive.or.zero}")
    private Long offset;

    @Positive(message = "{uk.ac.ebi.uniprot.uuw.advanced.search.positive}")
    private Integer size;

}
