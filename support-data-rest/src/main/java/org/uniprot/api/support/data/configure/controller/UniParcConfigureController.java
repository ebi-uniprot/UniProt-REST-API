package org.uniprot.api.support.data.configure.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.support.data.configure.response.AdvancedSearchTerm.PATH_PREFIX_FOR_AUTOCOMPLETE_SEARCH_FIELDS;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.support.data.configure.response.AdvancedSearchTerm;
import org.uniprot.api.support.data.configure.response.UniParcDatabaseDetail;
import org.uniprot.api.support.data.configure.response.UniProtReturnField;
import org.uniprot.api.support.data.configure.service.UniParcConfigureService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author jluo
 * @date: 20 Jun 2019
 */
@Tag(
        name = "Configuration",
        description = "These services provide configuration data used in the UniProt website")
@RestController
@RequestMapping("/configure/uniparc")
public class UniParcConfigureController {
    private final UniParcConfigureService service;

    public UniParcConfigureController(UniParcConfigureService service) {
        this.service = service;
    }

    @Operation(
            summary = "List of return fields available in the UniParc services.",
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
                                                                            UniProtReturnField
                                                                                    .class)))
                        })
            })
    @GetMapping("/result-fields")
    public List<UniProtReturnField> getResultFields() {
        return service.getResultFields();
    }

    @Operation(
            summary = "List of search fields available in the UniParc services.",
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
                                                                            AdvancedSearchTerm
                                                                                    .class)))
                        })
            })
    @GetMapping("/search-fields")
    public List<AdvancedSearchTerm> getSearchFields() {
        return service.getSearchItems(PATH_PREFIX_FOR_AUTOCOMPLETE_SEARCH_FIELDS);
    }

    @Operation(
            summary = "List of database details available for UniParc entry page.",
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
                                                                            UniParcDatabaseDetail
                                                                                    .class)))
                        })
            })
    @GetMapping("/allDatabases")
    public List<UniParcDatabaseDetail> getUniParcDatabaseDetails() {
        return service.getAllUniParcDatabaseDetails();
    }

    @Operation(
            summary = "List of return fields available in a UniParc entry.",
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
                                                                            UniProtReturnField
                                                                                    .class)))
                        })
            })
    @GetMapping("/entry-result-fields")
    public List<UniProtReturnField> getEntryResultFields() {
        return service.getUniParcEntryResultFields();
    }
}
