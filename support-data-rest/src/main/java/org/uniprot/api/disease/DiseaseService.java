package org.uniprot.api.disease;

import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.cv.disease.Disease;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.disease.DiseaseDocument;
import org.uniprot.store.search.field.DiseaseField;

@Service
public class DiseaseService extends BasicSearchService<DiseaseDocument, Disease> {

    private static Supplier<DefaultSearchHandler> handlerSupplier =
            () ->
                    new DefaultSearchHandler(
                            DiseaseField.Search.content,
                            DiseaseField.Search.accession,
                            DiseaseField.Search.getBoostFields());

    public DiseaseService(
            DiseaseRepository diseaseRepository,
            DiseaseDocumentToDiseaseConverter toDiseaseConverter,
            DiseaseSolrSortClause diseaseSolrSortClause) {

        super(
                diseaseRepository,
                toDiseaseConverter,
                diseaseSolrSortClause,
                handlerSupplier.get(),
                null);
    }

    @Override
    protected String getIdField() {
        return DiseaseField.Search.accession.name();
    }
}
