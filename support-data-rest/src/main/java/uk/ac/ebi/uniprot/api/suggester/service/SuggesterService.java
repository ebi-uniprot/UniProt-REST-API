package uk.ac.ebi.uniprot.api.suggester.service;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.SimpleQuery;
import uk.ac.ebi.uniprot.api.suggester.Suggestion;
import uk.ac.ebi.uniprot.api.suggester.Suggestions;
import uk.ac.ebi.uniprot.search.SolrCollection;
import uk.ac.ebi.uniprot.search.document.suggest.SuggestDictionary;
import uk.ac.ebi.uniprot.search.document.suggest.SuggestDocument;
import uk.ac.ebi.uniprot.search.field.QueryBuilder;
import uk.ac.ebi.uniprot.search.field.SuggestField;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created 18/07/18
 *
 * @author Edd
 */
public class SuggesterService {
    private static final String SUGGEST_SEARCH_HANDLER = "/search";
    private static String errorFormat;

    static {
        String dicts = Stream
                .of(SuggestDictionary.values())
                .map(Enum::name)
                .map(String::toLowerCase)
                .collect(Collectors.joining(", ", "[", "]"));
        errorFormat = "Unknown dictionary: {}. Expected one of " + dicts + ".";
    }

    private final SolrTemplate solrTemplate;
    private final SolrCollection collection;

    public SuggesterService(SolrTemplate solrTemplate, SolrCollection collection) {
        this.solrTemplate = solrTemplate;
        this.collection = collection;
    }

    public Suggestions findSuggestions(String dictionaryStr, String queryStr) {
        SimpleQuery query = new SimpleQuery(createQueryString(getDictionary(dictionaryStr), queryStr));
        query.setRequestHandler(SUGGEST_SEARCH_HANDLER);

        try {
            List<SuggestDocument> content = solrTemplate.query(collection.name(), query, SuggestDocument.class)
                    .getContent();
            return Suggestions.builder()
                    .dictionary(dictionaryStr)
                    .query(queryStr)
                    .suggestions(convertDocs(content))
                    .build();
        } catch (Exception e) {
            throw new SuggestionRetrievalException("Problem encountered when retrieving suggestions.", e);
        }
    }

    SuggestDictionary getDictionary(String dictionaryStr) {
        try {
            return SuggestDictionary.valueOf(dictionaryStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new UnknownDictionaryException(String.format(errorFormat, dictionaryStr));
        }
    }

    List<Suggestion> convertDocs(List<SuggestDocument> content) {
        return content.stream()
                .map(doc -> {
                    String value = doc.value;
                    if (Objects.nonNull(doc.altValues) && !doc.altValues.isEmpty()) {
                        StringJoiner joiner = new StringJoiner("/", " (", ")");
                        doc.altValues.forEach(joiner::add);
                        value += joiner.toString();
                    }
                    return Suggestion.builder()
                            .id(doc.id)
                            .value(value)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String createQueryString(SuggestDictionary dict, String query) {
        return "\"" + query + "\"" +
                " +" + QueryBuilder.query(SuggestField.Search.dict.name(), dict.name());
    }
}
