package org.uniprot.api.idmapping.controller.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import org.uniprot.api.rest.validation.ValidIdType;

import javax.validation.constraints.NotNull;

/**
 * Created 16/02/2021
 *
 * @author sahmad
 */
@Data
@ValidFromAndTo
public class IdMappingJobRequest {
    @NotNull(message = "{search.required}")
    @Parameter(description = "Name of the from type")
    @ValidIdType(message = "{idmapping.invalid.from}")
    private String from;

    @NotNull(message = "{search.required}")
    @Parameter(description = "Name of the to type")
    @ValidIdType(message = "{idmapping.invalid.to}")
    private String to;

    @NotNull(message = "{search.required}")
    @Parameter(description = "Comma separated list of ids")
    private String ids; // TODO add validation like length, regex

    @Parameter(description = "Value of the taxon Id")
    private String taxId;
}
