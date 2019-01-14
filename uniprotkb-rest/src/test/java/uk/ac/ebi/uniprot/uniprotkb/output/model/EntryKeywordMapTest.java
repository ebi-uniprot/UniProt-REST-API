package uk.ac.ebi.uniprot.uniprotkb.output.model;

import org.junit.jupiter.api.Test;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.UniProtResultFields;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.EvidencedString;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Keyword;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntryKeywordMapTest {
	@Test
	void testFields() {
		List<String> fields = EntryKeywordMap.FIELDS;
		List<String> expected = Arrays
				.asList("keyword", "keywordid");
		assertEquals(expected, fields);
		for (String field : fields) {
			assertTrue(UniProtResultFields.INSTANCE.getField(field).isPresent());
		}
	}
	@Test
	void testMapEmpty() {
		EntryKeywordMap dl = new EntryKeywordMap(null);
		Map<String, String> result = dl.attributeValues();
		assertTrue(result.isEmpty());
	}
	@Test
	void testMap() {
		List<Keyword> keywords = new ArrayList<>();
		keywords.add(new Keyword("KW-0002", create("3D-structure")));
		keywords.add(new Keyword("KW-0106", create("Calcium")));
		EntryKeywordMap dl = new EntryKeywordMap(keywords);
		Map<String, String> result = dl.attributeValues();
		assertEquals(2, result.size());
		verify("KW-0002; KW-0106", "keywordid", result );
		verify("3D-structure;Calcium", "keyword", result );
	}
	
	private void verify(String expected, String field, Map<String, String> result) {
		String evaluated = result.get(field);
		assertEquals(expected, evaluated);
	}
	private EvidencedString create(String value) {
		return new EvidencedString(value, Collections.emptyList());
	}
}