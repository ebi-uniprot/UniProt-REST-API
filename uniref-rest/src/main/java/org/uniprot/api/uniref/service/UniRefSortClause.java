package org.uniprot.api.uniref.service;

import javax.annotation.PostConstruct;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Component
public class UniRefSortClause extends AbstractSolrSortClause {
    private static final String DOC_ID = "id";

    @PostConstruct
    public void init() {
        addDefaultFieldOrderPair(DOC_ID, Sort.Direction.ASC);
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return DOC_ID;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIREF;
    }
}
