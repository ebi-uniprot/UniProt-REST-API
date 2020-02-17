package org.uniprot.api.disease;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.uniprot.core.cv.disease.DiseaseCrossReference;
import org.uniprot.core.cv.disease.DiseaseEntry;
import org.uniprot.core.cv.disease.builder.DiseaseEntryBuilder;
import org.uniprot.core.cv.disease.impl.DiseaseCrossReferenceImpl;
import org.uniprot.core.cv.keyword.Keyword;
import org.uniprot.core.cv.keyword.impl.KeywordImpl;
import org.uniprot.core.json.parser.disease.DiseaseJsonConfig;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.document.disease.DiseaseDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

@Slf4j
public class DiseaseSolrDocumentHelper {
    public static void createDiseaseDocuments(DataStoreManager storeManager, int documentCount) {
        Set<String> accessionBag = new HashSet<>();
        int batchSize = 100;
        if (documentCount <= batchSize) {
            batchSize = documentCount;
        }
        // create solr docs in batches
        for (int i = 0, j = 1; i < documentCount; i += batchSize, j++) {
            List<DiseaseDocument> diseaseDocuments = new ArrayList<>();
            IntStream.rangeClosed(1, batchSize)
                    .forEach(
                            index -> {
                                String accession = getNextUniqueAccession(accessionBag);
                                DiseaseDocument diseaseDocument =
                                        createDiseaseDocument(accession, index);
                                diseaseDocuments.add(diseaseDocument);
                            });
            log.info("Creating docs of batch {} with size {}", j, diseaseDocuments.size());
            storeManager.saveDocs(DataStoreManager.StoreType.DISEASE, diseaseDocuments);
            log.info("Total Docs Created: {}", accessionBag.size());
        }
    }

    public static void createDiseaseDocuments(
            DataStoreManager storeManager, String accession, long suffix) {
        DiseaseDocument diseaseDoc = createDiseaseDocument(accession, suffix);
        storeManager.saveDocs(DataStoreManager.StoreType.DISEASE, diseaseDoc);
    }

    private static String getNextUniqueAccession(Set<String> accessionBag) {
        String accPrefix = "DI-";
        long num = ThreadLocalRandom.current().nextLong(10000, 99999);
        String accession = accPrefix + num;
        if (accessionBag.contains(accession)) {
            return getNextUniqueAccession(accessionBag);
        }
        accessionBag.add(accession);
        return accession;
    }

    private static DiseaseDocument createDiseaseDocument(String accession, long suffix) {
        DiseaseEntryBuilder diseaseBuilder = new DiseaseEntryBuilder();
        Keyword keyword = new KeywordImpl("Mental retardation" + suffix, "KW-0991" + suffix);
        DiseaseCrossReference xref1 =
                new DiseaseCrossReferenceImpl(
                        "MIM" + suffix,
                        "617140" + suffix,
                        Collections.singletonList("phenotype" + suffix));
        DiseaseCrossReference xref2 =
                new DiseaseCrossReferenceImpl("MedGen" + suffix, "CN238690" + suffix);
        DiseaseCrossReference xref3 =
                new DiseaseCrossReferenceImpl("MeSH" + suffix, "D000015" + suffix);
        DiseaseCrossReference xref4 =
                new DiseaseCrossReferenceImpl("MeSH" + suffix, "D008607" + suffix);
        DiseaseEntry diseaseEntry =
                diseaseBuilder
                        .id("ZTTK syndrome" + suffix)
                        .accession(accession)
                        .acronym("ZTTKS" + suffix)
                        .definition(
                                "An autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.")
                        .alternativeNamesSet(
                                Arrays.asList(
                                        "Zhu-Tokita-Takenouchi-Kim syndrome",
                                        "ZTTK multiple congenital anomalies-mental retardation syndrome"))
                        .crossReferencesSet(Arrays.asList(xref1, xref2, xref3, xref4))
                        .keywordsAdd(keyword)
                        .reviewedProteinCount(suffix)
                        .unreviewedProteinCount(suffix)
                        .build();

        List<String> kwIds;
        if (diseaseEntry.getKeywords() != null) {
            kwIds =
                    diseaseEntry.getKeywords().stream()
                            .map(Keyword::getId)
                            .collect(Collectors.toList());
        } else {
            kwIds = new ArrayList<>();
        }
        // name is a combination of id, acronym, definition, synonyms, keywords
        List<String> name =
                Stream.concat(
                                Stream.concat(
                                        Stream.of(
                                                diseaseEntry.getId(),
                                                diseaseEntry.getAcronym(),
                                                diseaseEntry.getDefinition()),
                                        kwIds.stream()),
                                diseaseEntry.getAlternativeNames().stream())
                        .collect(Collectors.toList());
        // content is name + accession
        List<String> content = new ArrayList<>(name);
        content.add(diseaseEntry.getAccession());
        DiseaseDocument document =
                DiseaseDocument.builder()
                        .accession(accession)
                        .name(name)
                        .content(content)
                        .diseaseObj(getDiseaseBinary(diseaseEntry))
                        .build();

        return document;
    }

    private static ByteBuffer getDiseaseBinary(DiseaseEntry entry) {
        try {
            return ByteBuffer.wrap(
                    DiseaseJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse DiseaseEntry entry to binary json: ", e);
        }
    }
}
