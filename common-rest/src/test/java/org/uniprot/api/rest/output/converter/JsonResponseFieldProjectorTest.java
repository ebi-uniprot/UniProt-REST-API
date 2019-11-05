package org.uniprot.api.rest.output.converter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.core.builder.DiseaseBuilder;
import org.uniprot.core.cv.disease.CrossReference;
import org.uniprot.core.cv.disease.Disease;
import org.uniprot.core.cv.keyword.Keyword;
import org.uniprot.core.cv.keyword.impl.KeywordImpl;
import org.uniprot.core.json.parser.uniprot.*;
import org.uniprot.core.json.parser.uniprot.comment.*;
import org.uniprot.core.uniprot.ProteinExistence;
import org.uniprot.core.uniprot.UniProtEntry;
import org.uniprot.core.uniprot.UniProtEntryType;
import org.uniprot.core.uniprot.UniProtId;
import org.uniprot.core.uniprot.builder.UniProtEntryBuilder;
import org.uniprot.core.uniprot.builder.UniProtIdBuilder;
import org.uniprot.core.uniprot.comment.Comment;
import org.uniprot.store.search.field.DiseaseField;
import org.uniprot.store.search.field.UniProtField;

/** @author sahmad */
class JsonResponseFieldProjectorTest {

    private final JsonResponseFieldProjector fieldProjector = new JsonResponseFieldProjector();
    private Disease disease;

    @BeforeEach
    void setUp() {
        DiseaseBuilder diseaseBuilder = new DiseaseBuilder();
        Keyword keyword = new KeywordImpl("Mental retardation", "KW-0991");
        CrossReference xref1 =
                new CrossReference("MIM", "617140", Collections.singletonList("phenotype"));
        CrossReference xref2 = new CrossReference("MedGen", "CN238690");
        CrossReference xref3 = new CrossReference("MeSH", "D000015");
        CrossReference xref4 = new CrossReference("MeSH", "D008607");
        this.disease =
                diseaseBuilder
                        .id("ZTTK syndrome")
                        .accession("DI-04860")
                        .acronym("ZTTKS")
                        .definition(
                                "An autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.")
                        .alternativeNames(
                                Arrays.asList(
                                        "Zhu-Tokita-Takenouchi-Kim syndrome",
                                        "ZTTK multiple congenital anomalies-mental retardation syndrome"))
                        .crossReferences(Arrays.asList(xref1, xref2, xref3, xref4))
                        .keywords(keyword)
                        .reviewedProteinCount(1L)
                        .unreviewedProteinCount(0L)
                        .build();
    }

    @Test
    void testProjectAllFieldsByDefault() {
        Map<String, Object> returnMap =
                this.fieldProjector.project(
                        this.disease, null, Arrays.asList(DiseaseField.ResultFields.values()));
        Assertions.assertEquals(DiseaseField.ResultFields.values().length, returnMap.size());
        Assertions.assertTrue(returnMap.containsKey("id"));
        Assertions.assertNotNull(returnMap.get("id"));
        Assertions.assertTrue(returnMap.containsKey("accession"));
        Assertions.assertNotNull(returnMap.get("accession"));
        Assertions.assertTrue(returnMap.containsKey("acronym"));
        Assertions.assertNotNull(returnMap.get("acronym"));
        Assertions.assertTrue(returnMap.containsKey("definition"));
        Assertions.assertNotNull(returnMap.get("definition"));
        Assertions.assertTrue(returnMap.containsKey("alternativeNames"));
        Assertions.assertNotNull(returnMap.get("alternativeNames"));
        Assertions.assertTrue(returnMap.containsKey("crossReferences"));
        Assertions.assertNotNull(returnMap.get("crossReferences"));
        Assertions.assertTrue(returnMap.containsKey("keywords"));
        Assertions.assertNotNull(returnMap.get("keywords"));
        Assertions.assertTrue(returnMap.containsKey("reviewedProteinCount"));
        Assertions.assertNotNull(returnMap.get("reviewedProteinCount"));
        Assertions.assertTrue(returnMap.containsKey("unreviewedProteinCount"));
        Assertions.assertNotNull(returnMap.get("unreviewedProteinCount"));

        // test the return map in order of fields defined in DiseaseField.ResultFields
        Object[] orderedEntry = returnMap.entrySet().toArray();
        Assertions.assertEquals(DiseaseField.ResultFields.values().length, orderedEntry.length);
        String idKey = (String) ((Map.Entry) orderedEntry[0]).getKey();
        Assertions.assertEquals(DiseaseField.ResultFields.id.getJavaFieldName(), idKey);
        String accessionKey = (String) ((Map.Entry) orderedEntry[1]).getKey();
        Assertions.assertEquals(
                DiseaseField.ResultFields.accession.getJavaFieldName(), accessionKey);
        String acronymKey = (String) ((Map.Entry) orderedEntry[2]).getKey();
        Assertions.assertEquals(DiseaseField.ResultFields.acronym.getJavaFieldName(), acronymKey);
        String definitionKey = (String) ((Map.Entry) orderedEntry[3]).getKey();
        Assertions.assertEquals(
                DiseaseField.ResultFields.definition.getJavaFieldName(), definitionKey);
        String alternativeNamesKey = (String) ((Map.Entry) orderedEntry[4]).getKey();
        Assertions.assertEquals(
                DiseaseField.ResultFields.alternative_names.getJavaFieldName(),
                alternativeNamesKey);
        String crossReferencesKey = (String) ((Map.Entry) orderedEntry[5]).getKey();
        Assertions.assertEquals(
                DiseaseField.ResultFields.cross_references.getJavaFieldName(), crossReferencesKey);
        String keywordsKey = (String) ((Map.Entry) orderedEntry[6]).getKey();
        Assertions.assertEquals(DiseaseField.ResultFields.keywords.getJavaFieldName(), keywordsKey);
        String reviewedProteinCountKey = (String) ((Map.Entry) orderedEntry[7]).getKey();
        Assertions.assertEquals(
                DiseaseField.ResultFields.reviewed_protein_count.getJavaFieldName(),
                reviewedProteinCountKey);
        String unreviewedProteinCountKey = (String) ((Map.Entry) orderedEntry[8]).getKey();
        Assertions.assertEquals(
                DiseaseField.ResultFields.unreviewed_protein_count.getJavaFieldName(),
                unreviewedProteinCountKey);
    }

    @Test
    void testProjectFewValidFields() {
        List<String> returnFields =
                Stream.of("keywords", "alternative_names", "id").collect(Collectors.toList());
        Map<String, List<String>> filterFieldMap =
                returnFields.stream()
                        .collect(Collectors.toMap(f -> f, f -> Collections.emptyList()));

        Map<String, Object> returnMap =
                this.fieldProjector.project(
                        this.disease,
                        filterFieldMap,
                        Arrays.asList(DiseaseField.ResultFields.values()));

        Assertions.assertEquals(returnFields.size(), returnMap.size());

        Assertions.assertTrue(returnMap.containsKey("id"));
        Assertions.assertNotNull(returnMap.get("id"));
        Assertions.assertTrue(returnMap.containsKey("alternativeNames"));
        Assertions.assertNotNull(returnMap.get("alternativeNames"));
        Assertions.assertTrue(returnMap.containsKey("keywords"));
        Assertions.assertNotNull(returnMap.get("keywords"));
        Assertions.assertFalse(returnMap.containsKey("accession"));

        // test the return map in order of fields defined in DiseaseField.ResultFields even
        // filterFieldMap is passed
        Object[] orderedEntry = returnMap.entrySet().toArray();
        Assertions.assertEquals(returnFields.size(), orderedEntry.length);
        String idKey = (String) ((Map.Entry) orderedEntry[0]).getKey();
        Assertions.assertEquals(DiseaseField.ResultFields.id.getJavaFieldName(), idKey);
        String alternativeNamesKey = (String) ((Map.Entry) orderedEntry[1]).getKey();
        Assertions.assertEquals(
                DiseaseField.ResultFields.alternative_names.getJavaFieldName(),
                alternativeNamesKey);
        String keywordsKey = (String) ((Map.Entry) orderedEntry[2]).getKey();
        Assertions.assertEquals(DiseaseField.ResultFields.keywords.getJavaFieldName(), keywordsKey);
    }

    @Test
    void testProjectFewValidOneInvalidFields() { // it should ignore invalid fields and return
        // only valid fields
        List<String> returnFields =
                Stream.of("unreviewed_protein_count", "invalid_field_name", "id")
                        .collect(Collectors.toList());
        Map<String, List<String>> filterFieldMap =
                returnFields.stream()
                        .collect(Collectors.toMap(f -> f, f -> Collections.emptyList()));

        Map<String, Object> returnMap =
                this.fieldProjector.project(
                        this.disease,
                        filterFieldMap,
                        Arrays.asList(DiseaseField.ResultFields.values()));

        Assertions.assertEquals(returnFields.size() - 1, returnMap.size());

        Assertions.assertTrue(returnMap.containsKey("id"));
        Assertions.assertNotNull(returnMap.get("id"));
        Assertions.assertTrue(returnMap.containsKey("unreviewedProteinCount"));
        Assertions.assertNotNull(returnMap.get("unreviewedProteinCount"));
        Assertions.assertFalse(returnMap.containsKey("invalid_field_name"));

        // test the return map in order of fields defined in DiseaseField.ResultFields even
        // filterFieldMap is passed with one wrong field
        Object[] orderedEntry = returnMap.entrySet().toArray();
        Assertions.assertEquals(2, orderedEntry.length);
        String idKey = (String) ((Map.Entry) orderedEntry[0]).getKey();
        Assertions.assertEquals(DiseaseField.ResultFields.id.getJavaFieldName(), idKey);
        String unreviewedProteinCountKey = (String) ((Map.Entry) orderedEntry[1]).getKey();
        Assertions.assertEquals(
                DiseaseField.ResultFields.unreviewed_protein_count.getJavaFieldName(),
                unreviewedProteinCountKey);
    }

    @Test
    void testProjectAllUniProtKBFields() {
        List<Comment> comments = new ArrayList<>();
        comments.add(AlternativeProductsCommentTest.getAlternativeProductsComment());
        comments.add(BPCPCommentTest.getBpcpComment());
        comments.add(CatalyticActivityCommentTest.getCatalyticActivityComment());
        comments.add(CofactorCommentTest.getCofactorComment());
        comments.add(DiseaseCommentTest.getDiseaseComment());
        comments.add(FreeTextCommentTest.getFreeTextComment());
        comments.add(InteractionCommentTest.getInteractionComment());
        comments.add(MassSpectrometryCommentTest.getMassSpectrometryComment());
        comments.add(RnaEditingCommentTest.getRnaEditingComment());
        comments.add(SequenceCautionCommentTest.getSequenceCautionComment());
        comments.add(SubcellularLocationCommentTest.getSubcellularLocationComment());
        comments.add(WebResourceCommentTest.getWebResourceComment());

        UniProtId uniProtId = new UniProtIdBuilder("uniprot id").build();
        UniProtEntryBuilder builder = new UniProtEntryBuilder();
        UniProtEntry entry =
                builder.primaryAccession(UniProtAccessionTest.getUniProtAccession())
                        .uniProtId(uniProtId)
                        .active()
                        .entryType(UniProtEntryType.SWISSPROT)
                        .addSecondaryAccession(UniProtAccessionTest.getUniProtAccession())
                        .entryAudit(EntryAuditTest.getEntryAudit())
                        .proteinExistence(ProteinExistence.PROTEIN_LEVEL)
                        .proteinDescription(ProteinDescriptionTest.getProteinDescription())
                        .genes(Collections.singletonList(GeneTest.createCompleteGene()))
                        .annotationScore(2)
                        .organism(OrganimsTest.getOrganism())
                        .organismHosts(Collections.singletonList(OrganimHostTest.getOrganismHost()))
                        .comments(comments)
                        .features(Collections.singletonList(FeatureTest.getFeature()))
                        .internalSection(InternalSectionTest.getInternalSection())
                        .keywords(Collections.singletonList(KeywordTest.getKeyword()))
                        .geneLocations(
                                Collections.singletonList(GeneLocationTest.getGeneLocation()))
                        .references(UniProtReferenceTest.getUniProtReferences())
                        .databaseCrossReferences(
                                Collections.singletonList(
                                        UniProtDBCrossReferenceTest.getUniProtDBCrossReference()))
                        .sequence(SequenceTest.getSequence())
                        .build();

        Map<String, List<String>> filterFieldMap = new HashMap<>();
        filterFieldMap.put("gene", Collections.EMPTY_LIST);
        filterFieldMap.put("organism", Collections.EMPTY_LIST);
        filterFieldMap.put("feature", Collections.EMPTY_LIST);
        filterFieldMap.put("xref", Collections.EMPTY_LIST);
        filterFieldMap.put("keyword", Collections.EMPTY_LIST);
        List<String> cTypes = new ArrayList<>();
        cTypes.add("disease");
        cTypes.add("webresource");
        filterFieldMap.put("comment", cTypes);
        Map<String, Object> result =
                this.fieldProjector.project(
                        entry, filterFieldMap, Arrays.asList(UniProtField.ResultFields.values()));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(
                11, result.size(), "total number of expected fields does not match");
        Assertions.assertNotNull(result.get("entryType"));
        Assertions.assertNotNull(result.get("primaryAccession"));
        Assertions.assertNotNull(result.get("uniProtId"));
        Assertions.assertNotNull(result.get("entryAudit"));
        Assertions.assertNotNull(result.get("annotationScore"));
        Assertions.assertNotNull(result.get("genes"));
        Assertions.assertNotNull(result.get("organism"));
        Assertions.assertNotNull(result.get("features"));
        Assertions.assertNotNull(result.get("databaseCrossReferences"));
        Assertions.assertNotNull(result.get("keywords"));
        Assertions.assertNotNull(result.get("comments"));
        Assertions.assertEquals(cTypes.size(), ((List<?>) result.get("comments")).size());
        Assertions.assertEquals(entry.getEntryType(), result.get("entryType"));
        Assertions.assertEquals(entry.getPrimaryAccession(), result.get("primaryAccession"));

        // check the order of key in the map, the order should be same as definition in
        // UniProtField.ResultField
        Object[] orderedEntry = result.entrySet().toArray();
        Assertions.assertEquals(11, orderedEntry.length);
        String entryTypeKey = (String) ((Map.Entry) orderedEntry[0]).getKey();
        Assertions.assertEquals(
                UniProtField.ResultFields.entryType.getJavaFieldName(), entryTypeKey);
        String primaryAccessionKey = (String) ((Map.Entry) orderedEntry[1]).getKey();
        Assertions.assertEquals(
                UniProtField.ResultFields.primaryAccession.getJavaFieldName(), primaryAccessionKey);
        String uniProtIdKey = (String) ((Map.Entry) orderedEntry[2]).getKey();
        Assertions.assertEquals(
                UniProtField.ResultFields.uniProtId.getJavaFieldName(), uniProtIdKey);
        String entryAuditKey = (String) ((Map.Entry) orderedEntry[3]).getKey();
        Assertions.assertEquals(
                UniProtField.ResultFields.entryAudit.getJavaFieldName(), entryAuditKey);
        String annotationScoreKey = (String) ((Map.Entry) orderedEntry[4]).getKey();
        Assertions.assertEquals(
                UniProtField.ResultFields.annotationScore.getJavaFieldName(), annotationScoreKey);
        String organismKey = (String) ((Map.Entry) orderedEntry[5]).getKey();
        Assertions.assertEquals(UniProtField.ResultFields.organism.getJavaFieldName(), organismKey);
        String genesKey = (String) ((Map.Entry) orderedEntry[6]).getKey();
        Assertions.assertEquals(UniProtField.ResultFields.gene.getJavaFieldName(), genesKey);
        String commentsKey = (String) ((Map.Entry) orderedEntry[7]).getKey();
        Assertions.assertEquals(UniProtField.ResultFields.comment.getJavaFieldName(), commentsKey);
        String featuresKey = (String) ((Map.Entry) orderedEntry[8]).getKey();
        Assertions.assertEquals(UniProtField.ResultFields.feature.getJavaFieldName(), featuresKey);
        String keywordsKey = (String) ((Map.Entry) orderedEntry[9]).getKey();
        Assertions.assertEquals(UniProtField.ResultFields.keyword.getJavaFieldName(), keywordsKey);
        String databaseCrossReferencesKey = (String) ((Map.Entry) orderedEntry[10]).getKey();
        Assertions.assertEquals(
                UniProtField.ResultFields.xref.getJavaFieldName(), databaseCrossReferencesKey);
    }
}
