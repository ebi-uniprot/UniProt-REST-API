package org.uniprot.api.idmapping.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.idmapping.controller.IdMappingJobController.IDMAPPING_PATH;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.idmapping.controller.request.IdMappingBasicRequest;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.controller.response.JobStatusResponse;
import org.uniprot.api.idmapping.controller.response.JobSubmitResponse;
import org.uniprot.api.idmapping.service.IdMappingJobService;

import javax.validation.Valid;

/**
 * @author sahmad
 * @created 22/02/2021
 */
@RestController
@RequestMapping(IDMAPPING_PATH)
public class IdMappingJobController {
    static final String IDMAPPING_PATH = "/idmapping";
    private final IdMappingJobService idMappingJobService;

    @Autowired
    public IdMappingJobController(IdMappingJobService idMappingJobService) {
        this.idMappingJobService = idMappingJobService;
    }

    @PostMapping(
            value = "/run",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<JobSubmitResponse> submitJob(@Valid IdMappingBasicRequest request)
            throws InvalidKeySpecException, NoSuchAlgorithmException, InterruptedException {
        JobSubmitResponse response = this.idMappingJobService.submitJob(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping(
            value = "/status/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<JobStatusResponse> getStatus(@PathVariable String jobId) {
        return idMappingJobService.getStatus(jobId);
    }
}
