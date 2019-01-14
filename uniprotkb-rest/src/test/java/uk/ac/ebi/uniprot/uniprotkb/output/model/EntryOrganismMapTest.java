package uk.ac.ebi.uniprot.uniprotkb.output.model;

import org.junit.jupiter.api.Test;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.UniProtResultFields;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Organism;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Organism.OrganismName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntryOrganismMapTest {

	@Test
	void testFields() {
		List<String> fields =  EntryOrganismMap.FIELDS;
		List<String> expected =Arrays.asList(new String [] {
				 "organism", "organism_id", "tax_id"
		});
		assertEquals(expected, fields);
		for (String field : fields) {
			assertTrue(UniProtResultFields.INSTANCE.getField(field).isPresent());
		}
	}
	
	@Test
	void testGetData() {
		List<OrganismName> names = new ArrayList<>();
		names.add(new OrganismName("scientific", "Homo sapiens"));
		names.add(new OrganismName("common", "Human"));
		Organism organism = new Organism("9606", names);
		EntryOrganismMap dl = new EntryOrganismMap(organism);
		Map<String, String> result = dl.attributeValues();
		assertEquals(3, result.size());
		verify("organism", "Homo sapiens (Human)", result);
		verify("organism_id", "9606", result);
		verify("tax_id", "9606", result);
	}
	
	private void verify(String field, String expected, Map<String, String> result) {
		assertEquals(expected, result.get(field));
	}

}