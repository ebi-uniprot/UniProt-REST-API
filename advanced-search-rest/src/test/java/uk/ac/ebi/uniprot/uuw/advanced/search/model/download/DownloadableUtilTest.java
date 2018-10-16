package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import uk.ac.ebi.uniprot.dataservice.restful.features.domain.Evidence;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.EvidencedString;
import uk.ac.ebi.uniprot.dataservice.restful.features.domain.DbReferenceObject;

public class DownloadableUtilTest {
	@Test
	void testEvidencesToString() {
		String expected = "{ECO:0000269|PubMed:18258429, ECO:0000313|EMBL:AAC45250.1}";
		List<Evidence> evidences = new ArrayList<>();
		evidences.add(createEvidence("ECO:0000269", "PubMed", "18258429"));
		// ECO:0000313|EMBL:AAC45250.1
		evidences.add(createEvidence("ECO:0000313", "EMBL", "AAC45250.1"));
		String result = DownloadableUtil.evidencesToString(evidences);
		assertEquals(expected, result);
	}

	@Test
	void testEvidencesToStringEmpty() {
		String result = DownloadableUtil.evidencesToString(null);
		assertEquals("", result);
		result = DownloadableUtil.evidencesToString(Collections.emptyList());
		assertEquals("", result);
	}

	@Test
	void testConvertEvidencedString() {
		List<Evidence> evidences = new ArrayList<>();
		evidences.add(createEvidence("ECO:0000269", "PubMed", "18258429"));
		evidences.add(createEvidence("ECO:0000313", "EMBL", "AAC45250.1"));

		String text = "Some text.";
		String expected = "Some text. {ECO:0000269|PubMed:18258429, ECO:0000313|EMBL:AAC45250.1}";
		EvidencedString evStr = new EvidencedString(text, evidences);
		String result = DownloadableUtil.convertEvidencedString(evStr);
		assertEquals(expected, result);
	}

	@Test
	void testConvertEvidencedStringEmptyEv() {
		List<Evidence> evidences = new ArrayList<>();

		String text = "Some text.";
		String expected = "Some text.";
		EvidencedString evStr = new EvidencedString(text, evidences);
		String result = DownloadableUtil.convertEvidencedString(evStr);
		assertEquals(expected, result);
	}

	private Evidence createEvidence(String code, String dbType, String dbId) {
		DbReferenceObject dbref = new DbReferenceObject();
		dbref.setName(dbType);
		dbref.setId(dbId);
		Evidence evidence = new Evidence();
		evidence.setCode(code);
		evidence.setSource(dbref);
		return evidence;
	}
}
