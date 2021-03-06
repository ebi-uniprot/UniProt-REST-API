package org.uniprot.api.help.centre.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.help.centre.model.HelpCentreEntry;
import org.uniprot.api.help.centre.request.HelpCentreSearchRequest;
import org.uniprot.api.help.centre.service.HelpCentreService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author lgonzales
 * @since 07/07/2021
 */
@Tag(name = "help", description = "UniProt Help centre API")
@RestController
@Validated
@RequestMapping("/help")
public class HelpCentreController extends BasicSearchController<HelpCentreEntry> {

    private static final String HELP_CENTRE_ID_REGEX = "(?!^[0-9]+$)^.+$";
    private final HelpCentreService service;

    protected HelpCentreController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<HelpCentreEntry> converterContextFactory,
            HelpCentreService service) {
        super(
                eventPublisher,
                converterContextFactory,
                null,
                MessageConverterContextFactory.Resource.HELP);
        this.service = service;
    }

    @Operation(
            summary = "Get Help Centre Page by Id.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = HelpCentreEntry.class)),
                            @Content(mediaType = MARKDOWN_MEDIA_TYPE_VALUE)
                        })
            })
    @GetMapping(
            value = "/{id}",
            produces = {APPLICATION_JSON_VALUE, MARKDOWN_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<HelpCentreEntry>> getByHelpCentrePageId(
            @Parameter(description = "Help centre page id to find")
                    @Pattern(
                            regexp = HELP_CENTRE_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.helpcentre.invalid.id}")
                    @PathVariable("id")
                    String id,
            @Parameter(description = "Comma separated list of fields to be returned in response")
                    @ValidReturnFields(uniProtDataType = UniProtDataType.HELP)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {

        HelpCentreEntry entry = service.findByUniqueId(id);
        return super.getEntityResponse(entry, fields, request);
    }

    @Operation(
            summary = "Search Help pages by given Lucene search query.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            HelpCentreEntry.class)))
                        })
            })
    @GetMapping(
            value = "/search",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<HelpCentreEntry>> search(
            @Valid @ModelAttribute HelpCentreSearchRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<HelpCentreEntry> results = service.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @Override
    protected String getEntityId(HelpCentreEntry entity) {
        return entity.getId();
    }

    @Override
    protected Optional<String> getEntityRedirectId(HelpCentreEntry entity) {
        return Optional.empty();
    }
}
