package org.uniprot.api.support.data.configure.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import junit.framework.AssertionFailedError;

import org.junit.jupiter.api.Test;
import org.uniprot.api.support.data.configure.response.AdvancedSearchTerm;
import org.uniprot.api.support.data.configure.response.UniParcDatabaseDetail;
import org.uniprot.api.support.data.configure.response.UniProtReturnField;

/**
 * @author lgonzales
 * @since 27/05/2020
 */
class UniParcConfigureServiceTest {

    @Test
    void getResultFields() {
        UniParcConfigureService service = new UniParcConfigureService();
        List<UniProtReturnField> resultGroups = service.getResultFields();

        assertNotNull(resultGroups);
        assertEquals(5, resultGroups.size());

        assertEquals(6, resultGroups.get(0).getFields().size());
        assertEquals(3, resultGroups.get(1).getFields().size());
        assertEquals(1, resultGroups.get(2).getFields().size());
        assertEquals(2, resultGroups.get(3).getFields().size());
        assertEquals(12, resultGroups.get(4).getFields().size());
    }

    @Test
    void getSearchItems() {
        UniParcConfigureService service = new UniParcConfigureService();
        List<AdvancedSearchTerm> result = service.getSearchItems();
        assertNotNull(result);
        assertEquals(13, result.size());

        AdvancedSearchTerm database =
                result.stream()
                        .filter(term -> term.getTerm().equalsIgnoreCase("database"))
                        .findFirst()
                        .orElseThrow(AssertionFailedError::new);
        assertNotNull(database.getValues());
        assertTrue(database.getValues().size() > 0);

        AdvancedSearchTerm active =
                result.stream()
                        .filter(term -> term.getTerm().equalsIgnoreCase("active"))
                        .findFirst()
                        .orElseThrow(AssertionFailedError::new);
        assertNotNull(active.getValues());
        assertTrue(active.getValues().size() > 0);
    }

    @Test
    void getAllUniParcDatabaseDetails() {
        UniParcConfigureService service = new UniParcConfigureService();
        List<UniParcDatabaseDetail> result = service.getAllUniParcDatabaseDetails();

        assertNotNull(result);
        assertEquals(39, result.size());

        UniParcDatabaseDetail database = result.get(0);
        assertEquals("EG_BACTERIA", database.getName());
        assertEquals("EnsemblBacteria", database.getDisplayName());
        assertTrue(database.isAlive());
        assertEquals("https://www.ensemblgenomes.org/id/%id", database.getUriLink());
    }

    @Test
    void getUniParcEntryDatabaseResultFields() {
        UniParcConfigureService service = new UniParcConfigureService();
        List<UniProtReturnField> result = service.getUniParcDatabaseResultFields();
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Names & Taxonomy", result.get(0).getGroupName());
        assertEquals("Miscellaneous", result.get(1).getGroupName());
        assertEquals("Date of", result.get(2).getGroupName());
    }
}
