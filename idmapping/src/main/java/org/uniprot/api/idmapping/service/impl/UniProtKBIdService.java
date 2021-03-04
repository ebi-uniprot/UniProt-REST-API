package org.uniprot.api.idmapping.service.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.service.BasicIdService;
import org.uniprot.api.rest.respository.facet.impl.UniprotKBFacetConfig;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Service
public class UniProtKBIdService extends BasicIdService<UniProtKBEntry, UniProtKBEntryPair> {
    private final RDFStreamer uniProtKBRDFStreamer;

    public UniProtKBIdService(
            @Qualifier("uniProtKBEntryStoreStreamer") StoreStreamer<UniProtKBEntry> storeStreamer,
            @Qualifier("uniproKBfacetTupleStreamTemplate") FacetTupleStreamTemplate tupleStream,
            UniprotKBFacetConfig facetConfig,
            RDFStreamer uniProtKBRDFStreamer) {
        super(storeStreamer, tupleStream, facetConfig);
        this.uniProtKBRDFStreamer = uniProtKBRDFStreamer;
    }

    @Override
    protected UniProtKBEntryPair convertToPair(
            IdMappingStringPair mId, Map<String, UniProtKBEntry> idEntryMap) {
        return UniProtKBEntryPair.builder()
                .from(mId.getFrom())
                .to(idEntryMap.get(mId.getTo()))
                .build();
    }

    @Override
    protected String getEntryId(UniProtKBEntry entry) {
        return entry.getPrimaryAccession().getValue();
    }

    @Override
    public String getSolrIdField() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB)
                .getSearchFieldItemByName("accession_id")
                .getFieldName();
    }

    @Override
    public UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPROTKB;
    }

    @Override
    protected RDFStreamer getRDFStreamer() {
        return this.uniProtKBRDFStreamer;
    }
}
