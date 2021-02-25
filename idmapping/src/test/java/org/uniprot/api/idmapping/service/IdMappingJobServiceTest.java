package org.uniprot.api.idmapping.service;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.Returns;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClientException;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.idmapping.controller.DataStoreTestConfig;
import org.uniprot.api.idmapping.controller.request.IdMappingBasicRequest;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.controller.response.JobSubmitResponse;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.service.impl.IdMappingJobServiceImpl;

/**
 * @author sahmad
 * @created 24/02/2021
 */
@ActiveProfiles(profiles = "offline")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {IdMappingJobServiceImpl.class, TestConfig.class, DataStoreTestConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdMappingJobServiceTest {
    @Autowired private IdMappingJobServiceImpl jobService;
    @Autowired private IdMappingPIRService pirService;
    @Autowired private IdMappingJobCacheService cacheService;

    @Test
    void testSubmitJob()
            throws InvalidKeySpecException, NoSuchAlgorithmException, InterruptedException {
        IdMappingBasicRequest request = createIdMappingRequest();
        JobSubmitResponse response = this.jobService.submitJob(request);
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getJobId());
    }

    @Test
    void testFinishedJob()
            throws InvalidKeySpecException, NoSuchAlgorithmException, InterruptedException {
        // when
        IdMappingBasicRequest request = createIdMappingRequest();
        Mockito.when(this.pirService.mapIds(request))
                .thenReturn(
                        IdMappingResult.builder()
                                .mappedId(new IdMappingStringPair("from", "to"))
                                .build());

        JobSubmitResponse submitResponse = this.jobService.submitJob(request);
        Assertions.assertNotNull(submitResponse);
        Assertions.assertNotNull(submitResponse.getJobId());
        // then
        Thread.sleep(1000); // delay to make sure that job is running
        String jobId = submitResponse.getJobId();
        IdMappingJob submittedJob = this.cacheService.getJobAsResource(jobId);
        Assertions.assertNotNull(submittedJob);
        Assertions.assertEquals(jobId, submittedJob.getJobId());
        Assertions.assertEquals(JobStatus.FINISHED, submittedJob.getJobStatus());
        Assertions.assertNull(submittedJob.getErrorMessage());
        Assertions.assertEquals(request, submittedJob.getIdMappingRequest());
        Assertions.assertNotNull(submittedJob.getIdMappingResult());
        Assertions.assertNotNull(submittedJob.getCreated());
        Assertions.assertNotNull(submittedJob.getUpdated());
        Assertions.assertNotEquals(submittedJob.getCreated(), submittedJob.getUpdated());
    }

    @Test
    void testRunningJob()
            throws InvalidKeySpecException, NoSuchAlgorithmException, InterruptedException {
        // when
        IdMappingBasicRequest request = createIdMappingRequest();
        Mockito.doAnswer(
                        new AnswersWithDelay(
                                1500,
                                new Returns(
                                        IdMappingResult.builder()
                                                .mappedId(new IdMappingStringPair("from", "to"))
                                                .build())))
                .when(this.pirService)
                .mapIds(request);

        JobSubmitResponse submitResponse = this.jobService.submitJob(request);
        Assertions.assertNotNull(submitResponse);
        Assertions.assertNotNull(submitResponse.getJobId());
        // then
        Thread.sleep(500); // to make sure that task is picked to run
        String jobId = submitResponse.getJobId();
        IdMappingJob submittedJob = this.cacheService.getJobAsResource(jobId);
        Assertions.assertNotNull(submittedJob);
        Assertions.assertEquals(jobId, submittedJob.getJobId());
        Assertions.assertEquals(JobStatus.RUNNING, submittedJob.getJobStatus());
        Assertions.assertNull(submittedJob.getErrorMessage());
        Assertions.assertEquals(request, submittedJob.getIdMappingRequest());
        Assertions.assertNull(submittedJob.getIdMappingResult());
        Assertions.assertNotNull(submittedJob.getCreated());
        Assertions.assertNotNull(submittedJob.getUpdated());
    }

    @Test
    void testErroredJob()
            throws InvalidKeySpecException, NoSuchAlgorithmException, InterruptedException {
        // when
        IdMappingBasicRequest request = createIdMappingRequest();
        String errorMsg = "Error during rest call";
        Mockito.when(this.pirService.mapIds(request)).thenThrow(new RestClientException(errorMsg));

        JobSubmitResponse submitResponse = this.jobService.submitJob(request);
        Assertions.assertNotNull(submitResponse);
        Assertions.assertNotNull(submitResponse.getJobId());
        // then
        Thread.sleep(1000); // delay to make sure that thread is picked to run
        String jobId = submitResponse.getJobId();
        IdMappingJob submittedJob = this.cacheService.getJobAsResource(jobId);
        Assertions.assertNotNull(submittedJob);
        Assertions.assertEquals(jobId, submittedJob.getJobId());
        Assertions.assertEquals(JobStatus.ERROR, submittedJob.getJobStatus());
        Assertions.assertNotNull(submittedJob.getErrorMessage());
        Assertions.assertEquals(errorMsg, submittedJob.getErrorMessage());
        Assertions.assertEquals(request, submittedJob.getIdMappingRequest());
        Assertions.assertNull(submittedJob.getIdMappingResult());
        Assertions.assertNotNull(submittedJob.getCreated());
        Assertions.assertNotNull(submittedJob.getUpdated());
    }

    @Test
    void testGetUnknownJob() {
        Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> this.cacheService.getJobAsResource("some random id"));
    }

    private IdMappingBasicRequest createIdMappingRequest() {
        String random = UUID.randomUUID().toString();
        IdMappingBasicRequest request = new IdMappingBasicRequest();
        request.setFrom("from" + random);
        request.setTo("to" + random);
        request.setIds("ids" + random);
        return request;
    }
}