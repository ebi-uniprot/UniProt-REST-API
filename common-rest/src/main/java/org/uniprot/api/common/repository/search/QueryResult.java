package org.uniprot.api.common.repository.search;

import lombok.Getter;

import java.util.Collection;

import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.page.Page;
import org.uniprot.api.common.repository.search.term.TermInfo;

/**
 * Solr Repository response entity
 *
 * @author lgonzales
 */
@Getter
public class QueryResult<T> {
    private final Collection<TermInfo> matchedFields;
    private Page page;
    private final Collection<T> content;
    private final Collection<Facet> facets;

    private QueryResult(Collection<T> content, Page page, Collection<Facet> facets, Collection<TermInfo> matchedFields) {
        this.content = content;
        this.page = page;
        this.facets = facets;
        this.matchedFields = matchedFields;
    }

    public static <T> QueryResult<T> of(Collection<T> content, Page page) {
        return new QueryResult<>(content, page, null, null);
    }

    public static <T> QueryResult<T> of(Collection<T> content, Page page, Collection<Facet> facets) {
        return new QueryResult<>(content, page, facets, null);
    }

    public static <T> QueryResult<T> of(Collection<T> content, Page page, Collection<Facet> facets, Collection<TermInfo> termInfos) {
        return new QueryResult<>(content, page, facets, termInfos);
    }

    public Page getPageAndClean() {
        Page result = this.page;
        this.page = null;
        return result;
    }
}