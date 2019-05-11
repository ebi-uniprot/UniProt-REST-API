package uk.ac.ebi.uniprot.disease;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.ac.ebi.uniprot.api.disease.DiseaseDocumentToDiseaseConverter;
import uk.ac.ebi.uniprot.cv.disease.CrossReference;
import uk.ac.ebi.uniprot.cv.disease.Disease;
import uk.ac.ebi.uniprot.cv.keyword.Keyword;
import uk.ac.ebi.uniprot.cv.keyword.impl.KeywordImpl;
import uk.ac.ebi.uniprot.domain.builder.DiseaseBuilder;
import uk.ac.ebi.uniprot.json.parser.disease.DiseaseJsonConfig;
import uk.ac.ebi.uniprot.search.document.disease.DiseaseDocument;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes={DiseaseDocumentToDiseaseConverter.class})
public class DiseaseDocumentToDiseaseConverterTest {
    @Autowired
    private DiseaseDocumentToDiseaseConverter toDiseaseConverter;
    ObjectMapper diseaseObjectMapper = DiseaseJsonConfig.getInstance().getFullObjectMapper();

    @Test
    void shouldConvertDiseaseDocToDisease() throws JsonProcessingException {
        // create a disease object
        String id = "Sample Disease";
        String accession ="DI-12345";
        String acronym = "SAMPLE-DIS";
        String def = "This is sample definition.";
        List<String> altNames = Arrays.asList("name1", "name2", "name3");
        Long reviwedProteinCount = 100L;
        Long unreviwedProteinCount = 200L;

        // cross ref
        List<String> props = Arrays.asList("prop1", "prop2", "prop3");
        String xrefId = "XREF-123";
        String databaseType = "SAMPLE_TYPE";
        CrossReference cr = new CrossReference(databaseType, xrefId, props);

        //keyword
        String kId = "Sample Keyword";
        String kwAC = "KW-1234";
        Keyword keyword = new KeywordImpl(kId, kwAC);

        DiseaseBuilder builder = DiseaseBuilder.newInstance();
        builder.id(id).accession(accession).acronym(acronym).definition(def);
        builder.alternativeNames(altNames).crossReferences(cr);
        builder.keywords(keyword).reviewedProteinCount(reviwedProteinCount).unreviewedProteinCount(unreviwedProteinCount);

        Disease disease = builder.build();

        //convert disease to object
        byte[] diseaseObj = this.diseaseObjectMapper.writeValueAsBytes(disease);


        DiseaseDocument.DiseaseDocumentBuilder docBuilder = DiseaseDocument.builder();
        docBuilder.accession(accession);
        docBuilder.diseaseObj(ByteBuffer.wrap(diseaseObj));

        DiseaseDocument diseaseDocument = docBuilder.build();
        Disease convertedDisease = this.toDiseaseConverter.apply(diseaseDocument);

        // verify the result
        Assertions.assertEquals(disease.getId(), convertedDisease.getId());
        Assertions.assertEquals(disease.getAccession(), convertedDisease.getAccession());
        Assertions.assertEquals(disease.getAcronym(), convertedDisease.getAcronym());
        Assertions.assertEquals(disease.getDefinition(), convertedDisease.getDefinition());
        Assertions.assertEquals(disease.getReviewedProteinCount(), convertedDisease.getReviewedProteinCount());
        Assertions.assertEquals(disease.getUnreviewedProteinCount(), convertedDisease.getUnreviewedProteinCount());
        Assertions.assertEquals(disease.getAlternativeNames(), convertedDisease.getAlternativeNames());
        Assertions.assertEquals(disease.getCrossReferences(), convertedDisease.getCrossReferences());
        Assertions.assertEquals(disease.getKeywords(), convertedDisease.getKeywords());
    }
}
