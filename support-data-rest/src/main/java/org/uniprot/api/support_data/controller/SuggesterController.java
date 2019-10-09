package org.uniprot.api.support_data.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.suggester.Suggestions;
import org.uniprot.api.suggester.service.SuggesterService;

/**
 * Controller for the suggestion service.
 *
 * <p>Created 18/07/18
 *
 * @author Edd
 */
@RestController
public class SuggesterController {
    private final SuggesterService suggesterService;

    @Autowired
    public SuggesterController(SuggesterService suggesterService) {
        this.suggesterService = suggesterService;
    }

    @GetMapping(
            value = "/suggester",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<Suggestions> suggester(
            @RequestParam(value = "dict", required = true) String dict,
            @RequestParam(value = "query", required = true) String query) {

        return new ResponseEntity<>(suggesterService.findSuggestions(dict, query), HttpStatus.OK);
    }
}
