package org.uniprot.api.rest.output.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetItem;
import org.uniprot.api.common.repository.search.term.TermInfo;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.json.parser.uniprot.UniProtKBEntryIT;
import org.uniprot.core.json.parser.uniprot.UniprotKBJsonConfig;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

/**
 * @author lgonzales
 * @since 2020-04-02
 */
@Slf4j
class JsonMessageConverterTest {

    private static JsonMessageConverter<UniProtKBEntry> jsonMessageConverter;

    @BeforeAll
    static void init() {
        ReturnFieldConfig returnFieldConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB);
        ObjectMapper objectMapper = UniprotKBJsonConfig.getInstance().getSimpleObjectMapper();
        jsonMessageConverter =
                new JsonMessageConverter<>(objectMapper, UniProtKBEntry.class, returnFieldConfig);
    }

    @Test
    void beforeEntityOnlyReturnEmptyOutput() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder().entityOnly(true).build();
        log.debug("------- BEGIN: beforeEntityOnlyReturnEmptyOutput");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString("UTF-8");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void beforeCanPrintFacet() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .facets(Collections.singleton(getFacet()))
                        .build();
        log.debug("------- BEGIN: beforeCanPrintFacet");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString("UTF-8");
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.facets")));
        assertEquals(resultJson.read(JsonPath.compile("$.facets.size()")), new Integer(1));
        assertEquals("My Facet", resultJson.read(JsonPath.compile("$.facets[0].label")));

        assertNotNull(resultJson.read(JsonPath.compile("$.results")));
        assertEquals(resultJson.read(JsonPath.compile("$.results.size()")), new Integer(0));

        assertThrows(
                PathNotFoundException.class,
                () -> resultJson.read(JsonPath.compile("$.matchedFields")));
    }

    @Test
    void beforeCanPrintMatchedFields() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .matchedFields(Collections.singleton(getMatchedField()))
                        .build();
        log.debug("------- BEGIN: beforeCanPrintMatchedFields");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString("UTF-8");
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.matchedFields")));
        assertEquals(resultJson.read(JsonPath.compile("$.matchedFields.size()")), new Integer(1));
        assertEquals("fieldName", resultJson.read(JsonPath.compile("$.matchedFields[0].name")));

        assertNotNull(resultJson.read(JsonPath.compile("$.results")));
        assertEquals(resultJson.read(JsonPath.compile("$.results.size()")), new Integer(0));

        assertThrows(
                PathNotFoundException.class, () -> resultJson.read(JsonPath.compile("$.facets")));
    }

    @Test
    void writeCanWriteEntity() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder().entityOnly(true).build();
        log.debug("------- BEGIN: writeCanWriteEntity");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        writeEntity(outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString("UTF-8");
        log.debug(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.primaryAccession")));
        assertEquals("P00001", resultJson.read(JsonPath.compile("$.primaryAccession")));
    }

    @Test
    void writeCanWriteTenEntity() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder().build();
        log.debug("------- BEGIN: 10 writeCanTenWriteEntity");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString("UTF-8");
        log.debug(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.results")));
        assertEquals(resultJson.read(JsonPath.compile("$.results.size()")), new Integer(10));
    }

    @Test
    void writeCanWriteEntityWithPathOnlyReturnField() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .entityOnly(true)
                        .fields("accession,organism_name,gene_primary,gene_synonym")
                        .build();
        log.debug("------- BEGIN: writeCanWriteEntityWithPathOnlyReturnField");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        writeEntity(outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString("UTF-8");
        log.debug(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);

        assertNotNull(resultJson.read(JsonPath.compile("$.primaryAccession")));
        assertEquals("P00001", resultJson.read(JsonPath.compile("$.primaryAccession")));
        assertEquals(
                "scientific name", resultJson.read(JsonPath.compile("$.organism.scientificName")));
        assertEquals("some Gene", resultJson.read(JsonPath.compile("$.genes[0].geneName.value")));
        assertEquals("some Syn", resultJson.read(JsonPath.compile("$.genes[0].synonyms[0].value")));

        assertThrows(
                PathNotFoundException.class,
                () -> resultJson.read(JsonPath.compile("$.secondaryAccessions")));
    }

    @Test
    void writeCanWriteEntityWithFilteredPathReturnField() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .entityOnly(true)
                        .fields("accession,cc_function")
                        .build();
        log.debug("------- BEGIN: writeCanWriteEntityWithFilteredPathReturnField");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        writeEntity(outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString("UTF-8");
        log.debug(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.primaryAccession")));
        assertEquals("P00001", resultJson.read(JsonPath.compile("$.primaryAccession")));
        assertEquals("FUNCTION", resultJson.read(JsonPath.compile("$.comments[0].commentType")));

        assertThrows(
                PathNotFoundException.class,
                () -> resultJson.read(JsonPath.compile("$.secondaryAccessions")));
    }

    @Test
    void writeCanWriteEntityWithFilteredPathWithOrLogicReturnField() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .entityOnly(true)
                        .fields("organism_name,cc_rna_editing,cc_polymorphism")
                        .build();
        log.debug("------- BEGIN: writeCanWriteEntityWithFilteredPathWithOrLogicReturnField");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        writeBefore(messageContext, outputStream);
        writeEntity(outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString("UTF-8");
        log.debug(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.primaryAccession")));
        assertEquals(
                "P00001",
                resultJson.read(JsonPath.compile("$.primaryAccession"))); // required field
        assertEquals(
                "scientific name", resultJson.read(JsonPath.compile("$.organism.scientificName")));
        assertEquals("RNA EDITING", resultJson.read(JsonPath.compile("$.comments[0].commentType")));
        assertEquals(
                "POLYMORPHISM", resultJson.read(JsonPath.compile("$.comments[1].commentType")));

        assertThrows(
                PathNotFoundException.class,
                () -> resultJson.read(JsonPath.compile("$.secondaryAccessions")));
    }

    @Test
    void writeCanWriteEntitiesWithFilteredPathAndCanPrintEntitySeparator() throws IOException {
        List<UniProtKBEntry> entities = new ArrayList<>();
        entities.add(getEntity());
        entities.add(getEntity());
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder().fields("accession").build();
        log.debug("------- BEGIN: writeCanWriteEntitiesWithFilteredPathAndCanPrintEntitySeparator");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        jsonMessageConverter.writeEntities(
                entities.stream(), outputStream, Instant.now(), new AtomicInteger(0));
        writeAfter(messageContext, outputStream);
        String result = outputStream.toString("UTF-8");
        log.debug(result);
        assertEquals(
                "{\"results\":[{\"primaryAccession\":\"P00001\"},{\"primaryAccession\":\"P00001\"}]}",
                result);
    }

    @Test
    void writeCanWriteOkayAndFailedEntities() throws IOException {
        List<UniProtKBEntry> entities = new ArrayList<>();
        entities.add(getEntity());
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .fields("accession")
                        .failedIds(List.of("id1"))
                        .build();
        log.debug("------- BEGIN: writeCanWriteOkayAndFailedEntities");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        jsonMessageConverter.writeEntities(
                entities.stream(), outputStream, Instant.now(), new AtomicInteger(0));
        writeAfter(messageContext, outputStream);
        String result = outputStream.toString("UTF-8");
        log.debug(result);
        assertEquals(
                "{\"results\":[{\"primaryAccession\":\"P00001\"}],\"failedIds\":[\"id1\"]}",
                result);
    }

    @Test
    void writeCanWriteOnlyFailedEntities() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .failedIds(List.of("id1", "id2"))
                        .build();
        log.debug("------- BEGIN: writeCanWriteOnlyFailedEntities");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        jsonMessageConverter.writeEntities(
                Stream.empty(), outputStream, Instant.now(), new AtomicInteger(0));
        writeAfter(messageContext, outputStream);
        String result = outputStream.toString("UTF-8");
        log.debug(result);
        assertEquals("{\"results\":[],\"failedIds\":[\"id1\",\"id2\"]}", result);
    }

    @Test
    void writeCanWriteTenEntitiesWithFilteredPathWithOrLogicReturnField() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .fields("organism_name,cc_rna_editing,cc_polymorphism")
                        .build();
        log.debug(
                "------- BEGIN: 10 writeCanWriteTenEntitiesWithFilteredPathWithOrLogicReturnField");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        writeBefore(messageContext, outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString("UTF-8");
        log.debug(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.results")));
        assertEquals(resultJson.read(JsonPath.compile("$.results.size()")), new Integer(10));
    }

    private void writeBefore(
            MessageConverterContext<UniProtKBEntry> messageContext,
            ByteArrayOutputStream outputStream)
            throws IOException {
        long start = System.currentTimeMillis();
        jsonMessageConverter.before(messageContext, outputStream);
        long end = System.currentTimeMillis();
        log.debug("DEBUG: Before " + (end - start) + " MilliSeconds");
    }

    private void writeEntity(OutputStream outputStream) throws IOException {
        long start = System.currentTimeMillis();
        jsonMessageConverter.writeEntity(getEntity(), outputStream);
        long end = System.currentTimeMillis();
        log.debug("DEBUG: Write " + (end - start) + " MilliSeconds");
    }

    private void writeAfter(
            MessageConverterContext<UniProtKBEntry> messageContext,
            ByteArrayOutputStream outputStream)
            throws IOException {
        long start = System.currentTimeMillis();
        jsonMessageConverter.after(messageContext, outputStream);
        jsonMessageConverter.cleanUp();
        long end = System.currentTimeMillis();
        log.debug("DEBUG: After " + (end - start) + " MilliSeconds");
    }

    private UniProtKBEntry getEntity() {
        return UniProtKBEntryIT.getCompleteColumnsUniProtEntry();
    }

    private Facet getFacet() {
        FacetItem item =
                FacetItem.builder().label("Item label").count(10L).value("item_value").build();

        return Facet.builder()
                .name("my_facet")
                .label("My Facet")
                .allowMultipleSelection(true)
                .values(Collections.singletonList(item))
                .build();
    }

    private TermInfo getMatchedField() {
        return TermInfo.builder().hits(10).name("fieldName").build();
    }
}
