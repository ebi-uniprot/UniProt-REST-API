package org.uniprot.api.rest.request;

import java.util.*;
import java.util.stream.Collectors;

import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMetaReader;

public class SortFieldMetaReaderImpl implements ModelFieldMetaReader {

    @Override
    public List<Map<String, Object>> read(String metaFile) {
        List<Map<String, Object>> metaList = new ArrayList<>();
        // e.g. metaFile=uniprotkb-search-fields.json
        String[] fileNameTokens = metaFile.split("-");
        if (fileNameTokens.length > 1) {
            String sourceName = fileNameTokens[0];
            UniProtDataType upDataType = UniProtDataType.valueOf(sourceName.toUpperCase());
            SearchFieldConfig config = SearchFieldConfigFactory.getSearchFieldConfig(upDataType);
            metaList =
                    config.getSearchFieldItems().stream()
                            .filter(
                                    item ->
                                            config.correspondingSortFieldExists(
                                                    item.getFieldName()))
                            .sorted(Comparator.comparing(SearchFieldItem::getFieldName))
                            .map(item -> convertToMap(item, config))
                            .collect(Collectors.toList());
        }

        return metaList;
    }

    private Map<String, Object> convertToMap(
            SearchFieldItem searchFieldItem, SearchFieldConfig config) {
        Map<String, Object> keyValueMap = new LinkedHashMap<>();
        keyValueMap.put("name", searchFieldItem.getFieldName());
        String example =
                config.getCorrespondingSortField(searchFieldItem.getFieldName()).getExample();
        keyValueMap.put("example", example);
        return keyValueMap;
    }
}
