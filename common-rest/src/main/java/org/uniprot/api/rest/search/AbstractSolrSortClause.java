package org.uniprot.api.rest.search;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to handle solr sort clause, like parsing, providing default sort order
 *
 * @author sahmad
 */
public abstract class AbstractSolrSortClause {
    public Sort getSort(String sortClause, boolean hasScore) {
        Sort result;
        if (StringUtils.isEmpty(sortClause)) {
            result = createDefaultSort(hasScore);
        } else {
            result = createSort(sortClause);
        }

        String documentIdFieldName = getSolrDocumentIdFieldName();
        if (result != null && result.getOrderFor(documentIdFieldName) == null) {
            result = result.and(new Sort(Sort.Direction.ASC, documentIdFieldName));
        }
        return result;
    }

    protected abstract Sort createDefaultSort(boolean hasScore);

    protected abstract String getSolrDocumentIdFieldName();

    protected abstract String getSolrSortFieldName(String name);

    protected List<Pair<String, Sort.Direction>> parseSortClause(String sortClause) {
        List<Pair<String, Sort.Direction>> fieldSortPairs = new ArrayList<>();

        String[] tokenizedSortClause =
                sortClause.split("\\s*,\\s*"); // e.g. field1 asc, field2 desc, field3 asc
        boolean hasIdField = false;
        for (String singleSortPairStr : tokenizedSortClause) {
            String[] fieldSortPairArr = singleSortPairStr.split("\\s+");
            if (fieldSortPairArr.length != 2) {
                throw new IllegalArgumentException("You must pass field and sort value in pair.");
            }
            String solrFieldName = getSolrSortFieldName(fieldSortPairArr[0]);
            fieldSortPairs.add(
                    new ImmutablePair<>(
                            solrFieldName, Sort.Direction.fromString(fieldSortPairArr[1])));
            if (solrFieldName.equals(getSolrDocumentIdFieldName())) {
                hasIdField = true;
            }
        }
        if (!hasIdField && !fieldSortPairs.isEmpty()) {
            fieldSortPairs.add(
                    new ImmutablePair<>(getSolrDocumentIdFieldName(), Sort.Direction.ASC));
        }
        return fieldSortPairs;
    }

    private Sort createSort(String sortClause) {
        return convertToSolrSort(parseSortClause(sortClause));
    }

    private Sort convertToSolrSort(List<Pair<String, Sort.Direction>> fieldSortPairs) {
        Sort sort = null;
        for (Pair<String, Sort.Direction> sField : fieldSortPairs) {
            if (sort == null) {
                sort = new Sort(sField.getRight(), sField.getLeft());
            } else {
                sort = sort.and(new Sort(sField.getRight(), sField.getLeft()));
            }
        }
        return sort;
    }
}
