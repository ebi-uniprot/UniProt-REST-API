package org.uniprot.api.uniparc.request;

import org.uniprot.store.search.field.validator.FieldRegexConstants;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sahmad
 * @created 12/08/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniParcGetByAccessionRequest extends UniParcGetByIdRequest {
    @Pattern(
            regexp = FieldRegexConstants.UNIPROTKB_ACCESSION_REGEX,
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.invalid.accession.value}")
    @NotNull(message = "{search.required}")
    @Parameter(description = "Unique identifier for the UniProt entry")
    private String accession;
}
