package org.uniprot.api.idmapping.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.uniprot.api.idmapping.IDMappingREST;

/**
 * @author sahmad
 * @created 22/02/2021
 */
@ActiveProfiles(profiles = "offline")
@ContextConfiguration(classes = {IDMappingREST.class})
@WebMvcTest(IdMappingJobController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdMappingJobControllerIT {
    private static final String JOB_SUBMIT_ENDPOINT =
            IdMappingJobController.IDMAPPING_PATH + "/run";

    @Autowired private MockMvc mockMvc;

    @Test
    void testSubmitJob() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", "ACC")
                                .param("to", "ACC")
                                .param("ids", "Q00001,Q00002"));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", Matchers.notNullValue()));
    }

    @Test
    void testSubmitJobWithInvalidFrom() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", "ACC123")
                                .param("to", "ACC")
                                .param("ids", "Q00001,Q00002"));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.messages[*]",
                                contains("Invalid from value")));
    }

    @Test
    void testSubmitJobWithInvalidTo() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", "ACC")
                                .param("to", "ACC123")
                                .param("ids", "Q00001,Q00002"));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.messages[*]",
                                contains("Invalid to value")));
    }
}
