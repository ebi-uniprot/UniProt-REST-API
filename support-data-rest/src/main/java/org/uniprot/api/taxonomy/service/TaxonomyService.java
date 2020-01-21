package org.uniprot.api.taxonomy.service;

import org.springframework.stereotype.Service;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.taxonomy.repository.TaxonomyFacetConfig;
import org.uniprot.api.taxonomy.repository.TaxonomyRepository;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;
import org.uniprot.store.search.domain2.UniProtSearchFields;

import java.util.function.Supplier;

import static org.uniprot.store.search.field.TaxonomyField.Search;

@Service
public class TaxonomyService extends BasicSearchService<TaxonomyDocument, TaxonomyEntry> {
    private static final Supplier<DefaultSearchHandler> handlerSupplier =
            () ->
                    new DefaultSearchHandler(
                            UniProtSearchFields.TAXONOMY, "content", "id", Search.getBoostFields());

    public TaxonomyService(
            TaxonomyRepository repository,
            TaxonomyFacetConfig facetConfig,
            TaxonomyEntryConverter converter,
            TaxonomySortClause taxonomySortClause) {

        super(repository, converter, taxonomySortClause, handlerSupplier.get(), facetConfig);
    }

    public TaxonomyEntry findById(final long taxId) {
        return findByUniqueId(String.valueOf(taxId));
    }

    @Override
    protected String getIdField() {
        return Search.id.name();
    }
}
