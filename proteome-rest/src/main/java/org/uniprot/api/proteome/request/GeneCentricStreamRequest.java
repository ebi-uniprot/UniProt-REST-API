package org.uniprot.api.proteome.request;

import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.rest.request.StreamRequest;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 29/10/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GeneCentricStreamRequest extends GeneCentricBasicRequest implements StreamRequest {

    @Parameter(
            description =
                    "Add content disposition attachment to response header, this way it can be downloaded as a file in the browser.")
    @Pattern(
            regexp = "^true|false$",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{stream.invalid.download}")
    private String download;
}
