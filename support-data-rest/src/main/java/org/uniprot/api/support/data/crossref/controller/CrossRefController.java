package org.uniprot.api.support.data.crossref.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.RDF_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.RDF_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.CROSSREF;

import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.support.data.crossref.request.CrossRefSearchRequest;
import org.uniprot.api.support.data.crossref.request.CrossRefStreamRequest;
import org.uniprot.api.support.data.crossref.service.CrossRefService;
import org.uniprot.core.cv.xdb.CrossRefEntry;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/database")
@Validated
@Tag(
        name = "CrossReference",
        description =
                "The cross-references section of UniProtKB entries "
                        + "displays explicit and implicit links to databases such as nucleotide sequence databases, "
                        + "model organism databases and genomics and proteomics resources. A single entry can have "
                        + "cross-references to several dozen different databases and have several hundred individual links. "
                        + "The databases are categorized for easy user perusal and understanding of how the "
                        + "different databases relate to both UniProtKB and to each other")
public class CrossRefController extends BasicSearchController<CrossRefEntry> {
    @Autowired private CrossRefService crossRefService;
    private static final String ACCESSION_REGEX = "DB-(\\d{4})";

    public CrossRefController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<CrossRefEntry> crossrefMessageConverterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(
                eventPublisher,
                crossrefMessageConverterContextFactory,
                downloadTaskExecutor,
                CROSSREF);
    }

    @Operation(
            summary = "Get cross-references by database id.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CrossRefEntry.class)),
                            @Content(mediaType = RDF_MEDIA_TYPE_VALUE)
                        })
            })
    @GetMapping(
            value = "/{id}",
            produces = {APPLICATION_JSON_VALUE, RDF_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<CrossRefEntry>> findByAccession(
            @Parameter(description = "cross-references database id to find")
                    @PathVariable("id")
                    @Pattern(
                            regexp = ACCESSION_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.crossref.invalid.id}")
                    String id,
            @Parameter(description = "Comma separated list of fields to be returned in response")
                    @ValidReturnFields(uniProtDataType = UniProtDataType.CROSSREF)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {

        if (isRDFAccept(request)) {
            String result = this.crossRefService.getRDFXml(id);
            return super.getEntityResponseRDF(result, getAcceptHeader(request), request);
        }

        CrossRefEntry crossRefEntry = this.crossRefService.findByUniqueId(id);

        return super.getEntityResponse(crossRefEntry, fields, request);
    }

    @Operation(
            summary = "Search cross-references by given Lucene search query.",
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
                                                                            CrossRefEntry.class)))
                        })
            })
    @GetMapping(
            value = "/search",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<CrossRefEntry>> search(
            @Valid @ModelAttribute CrossRefSearchRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<CrossRefEntry> results = this.crossRefService.search(searchRequest);

        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @Operation(
            summary = "Download cross-references by given Lucene search query.",
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
                                                                            CrossRefEntry.class))),
                            @Content(mediaType = RDF_MEDIA_TYPE_VALUE)
                        })
            })
    @GetMapping(
            value = "/stream",
            produces = {APPLICATION_JSON_VALUE, RDF_MEDIA_TYPE_VALUE})
    public DeferredResult<ResponseEntity<MessageConverterContext<CrossRefEntry>>> stream(
            @Valid @ModelAttribute CrossRefStreamRequest streamRequest,
            @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                    MediaType contentType,
            HttpServletRequest request) {

        if (contentType.equals(RDF_MEDIA_TYPE)) {
            Stream<String> result = crossRefService.streamRDF(streamRequest);
            return super.streamRDF(result, streamRequest, contentType, request);
        } else {
            Stream<CrossRefEntry> result = crossRefService.stream(streamRequest);
            return super.stream(result, streamRequest, contentType, request);
        }
    }

    @Override
    protected String getEntityId(CrossRefEntry entity) {
        return entity.getId();
    }

    @Override
    protected Optional<String> getEntityRedirectId(CrossRefEntry entity) {
        return Optional.empty();
    }
}
