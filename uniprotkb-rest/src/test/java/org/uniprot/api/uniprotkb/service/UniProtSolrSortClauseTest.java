package org.uniprot.api.uniprotkb.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.uniprot.store.search.field.UniProtField;

/**
 * Unit Test class to validate UniProtSortUtil class behaviour
 *
 * @author lgonzales
 */
class UniProtSolrSortClauseTest {

    @Test
    void testCreateSingleAccessionSortAsc() {
        Sort sort = new UniProtSolrSortClause().createSort("accession asc");
        assertNotNull(sort);

        Iterator<Sort.Order> sortIterator = sort.iterator();
        assertNotNull(sortIterator);

        assertTrue(sortIterator.hasNext());
        Sort.Order order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.accession.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.ASC);

        assertFalse(sortIterator.hasNext());
    }

    @Test
    void testCreateSingleMnemonicSortDescAlsoAddAccessionAsc() {
        Sort sort = new UniProtSolrSortClause().createSort("mnemonic desc");
        assertNotNull(sort);

        Iterator<Sort.Order> sortIterator = sort.iterator();
        assertNotNull(sortIterator);

        assertTrue(sortIterator.hasNext());
        Sort.Order order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.mnemonic.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.DESC);

        assertTrue(sortIterator.hasNext());
        order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.accession.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.ASC);

        assertFalse(sortIterator.hasNext());
    }

    @Test
    void testCreateCompositeAccessionSortAscAndGeneDesc() {
        Sort sort = new UniProtSolrSortClause().createSort("accession desc,gene asc");
        assertNotNull(sort);

        Iterator<Sort.Order> sortIterator = sort.iterator();
        assertNotNull(sortIterator);

        assertTrue(sortIterator.hasNext());
        Sort.Order order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.accession.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.DESC);

        assertTrue(sortIterator.hasNext());
        order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.gene.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.ASC);

        assertFalse(sortIterator.hasNext());
    }

    @Test
    void testCreateCompositeMnemonicSortDescAlsoAddAccessionAsc() {
        Sort sort =
                new UniProtSolrSortClause().createSort("organism asc,mass desc , accession asc");
        assertNotNull(sort);

        Iterator<Sort.Order> sortIterator = sort.iterator();
        assertNotNull(sortIterator);

        assertTrue(sortIterator.hasNext());
        Sort.Order order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.organism.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.ASC);

        assertTrue(sortIterator.hasNext());
        order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.mass.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.DESC);

        assertTrue(sortIterator.hasNext());
        order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.accession.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.ASC);

        assertFalse(sortIterator.hasNext());
    }

    @Test
    void testCreateDefaultSortWithScore() {
        Sort defaultSort = new UniProtSolrSortClause().createDefaultSort(true);
        assertNotNull(defaultSort);

        Iterator<Sort.Order> sortIterator = defaultSort.iterator();
        assertNotNull(sortIterator);

        assertTrue(sortIterator.hasNext());
        Sort.Order order = sortIterator.next();
        assertEquals(order.getProperty(), "score");
        assertEquals(order.getDirection(), Sort.Direction.DESC);

        assertTrue(sortIterator.hasNext());
        order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.annotation_score.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.DESC);

        assertTrue(sortIterator.hasNext());
        order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.accession.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.ASC);

        assertFalse(sortIterator.hasNext());
    }
}
