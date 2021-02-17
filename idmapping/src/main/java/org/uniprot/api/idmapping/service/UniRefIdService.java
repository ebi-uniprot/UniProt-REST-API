package org.uniprot.api.idmapping.service;

import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.controller.request.IdMappingSearchRequest;
import org.uniprot.api.rest.respository.facet.impl.UniRefFacetConfig;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.util.Pair;

/**
 * @author sahmad
 * @created 16/02/2021
 */
public class UniRefIdService extends BasicIdService<UniRefEntry> {
    public UniRefIdService(
            IDMappingPIRService idMappingService,
            StoreStreamer<UniRefEntry> storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            UniRefFacetConfig facetConfig) {
        super(idMappingService, storeStreamer, tupleStream, facetConfig);
    }

    @Override
    public QueryResult<Pair<String, UniRefEntry>> getMappedEntries(
            IdMappingSearchRequest searchRequest) {
        return null;
    }

    @Override
    public String getFacetIdField() {
        return "id";
    }
}