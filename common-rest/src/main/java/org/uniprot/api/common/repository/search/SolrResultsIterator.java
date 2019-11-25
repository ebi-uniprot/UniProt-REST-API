package org.uniprot.api.common.repository.search;

import static java.util.Collections.emptyList;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import lombok.extern.slf4j.Slf4j;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CursorMarkParams;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.Document;

/**
 * Created 09/10/2019
 *
 * @author Edd
 */
@Slf4j
public class SolrResultsIterator<T extends Document> implements Iterator<List<T>>, Closeable {
    private SolrClient solrClient;
    private SolrCollection collection;
    private Class<T> documentType;
    private SolrQuery query;
    private boolean finished;
    private String currentCursorMark;
    private List<T> batch;
    private boolean currentBatchHasBeenRetrieved;

    public SolrResultsIterator(
            SolrClient solrClient,
            SolrCollection collection,
            SolrQuery query,
            Class<T> documentType) {
        this.solrClient = solrClient;
        this.collection = collection;
        this.currentCursorMark = CursorMarkParams.CURSOR_MARK_START;
        this.query = query;
        this.finished = false;
        this.documentType = documentType;
        this.batch = emptyList();
        this.currentBatchHasBeenRetrieved = true;
    }

    @Override
    public boolean hasNext() {
        if (currentBatchHasBeenRetrieved) {
            if (finished) {
                return false;
            } else {
                loadMoreResults();
                return !batch.isEmpty();
            }
        } else {
            return true;
        }
    }

    @Override
    public List<T> next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements available for this Iterator.");
        } else {
            currentBatchHasBeenRetrieved = true;
            List<T> toReturn = batch;
            batch = null;
            return toReturn;
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        finished();
    }

    private void loadMoreResults() {
        try {
            this.query.set(CursorMarkParams.CURSOR_MARK_PARAM, currentCursorMark);
            log.info(
                    "########################### Calling solr server with query {} {}",
                    "\n",
                    this.query.toString());
            QueryResponse response = solrClient.query(collection.toString(), this.query);
            if (response == null) {
                finished();
            } else {
                batch = response.getBeans(documentType);
                currentBatchHasBeenRetrieved = false;
                String nextCursorMark = response.getNextCursorMark();
                if (currentCursorMark.equals(nextCursorMark)) {
                    finished();
                } else {
                    currentCursorMark = nextCursorMark;
                }
            }
        } catch (SolrServerException | IOException e) {
            String message = "Problem encountered when iterating through search results";
            log.error(message, e);
            throw new QueryRetrievalException(message, e);
        }
    }

    private void finished() {
        this.finished = true;
        this.solrClient = null;
        this.currentCursorMark = null;
        this.query = null;
        this.documentType = null;
        this.collection = null;
    }

    SolrClient getSolrClient() {
        return solrClient;
    }

    SolrCollection getCollection() {
        return collection;
    }

    Class<T> getDocumentType() {
        return documentType;
    }

    SolrQuery getQuery() {
        return query;
    }

    boolean isFinished() {
        return finished;
    }

    String getCurrentCursorMark() {
        return currentCursorMark;
    }
}
