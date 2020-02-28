package org.uniprot.api.configure.uniprot.domain.model;

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import org.apache.commons.lang3.StringUtils;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.config.searchfield.model.SearchFieldType;
import org.uniprot.store.search.domain.EvidenceGroup;
import org.uniprot.store.search.domain.impl.AnnotationEvidences;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AdvanceUniProtKBSearchTerm implements Serializable {
    private static final long serialVersionUID = -5776203445383103470L;
    private String id;
    @JsonIgnore private String parentId;
    @JsonIgnore private Integer childNumber;
    @JsonIgnore private Integer seqNumber;
    private String label;
    private String itemType;
    private String term;
    private String dataType;
    private String fieldType;
    @JsonIgnore private String description;
    private String example;
    private String autoComplete;
    private String autoCompleteQueryTerm;
    @JsonIgnore private String autoCompleteQueryFieldValidRegex;
    private String regex;
    private List<Value> values;
    private List<AdvanceUniProtKBSearchTerm> items;
    private List<EvidenceGroup> evidenceGroups;

    @Data
    @AllArgsConstructor
    public static class Value {
        private String name;
        private String value;
    }

    public static List<AdvanceUniProtKBSearchTerm> getUniProtKBSearchTerms() {
        SearchFieldConfig config =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.uniprotkb);
        List<SearchFieldItem> rootFieldItems = getTopLevelFieldItems(config);
        Comparator<AdvanceUniProtKBSearchTerm> comparatorBySeqNumber =
                Comparator.comparing(AdvanceUniProtKBSearchTerm::getSeqNumber);
        Comparator<AdvanceUniProtKBSearchTerm> comparatorByChildNumber =
                Comparator.comparing(AdvanceUniProtKBSearchTerm::getChildNumber);
        List<AdvanceUniProtKBSearchTerm> rootSearchTermConfigs =
                convert(rootFieldItems, comparatorBySeqNumber);

        Queue<AdvanceUniProtKBSearchTerm> queue = new LinkedList<>(rootSearchTermConfigs);

        while (!queue.isEmpty()) { // BFS logic
            AdvanceUniProtKBSearchTerm currentItem = queue.remove();
            List<SearchFieldItem> childFieldItems = getChildFieldItems(config, currentItem.getId());
            List<AdvanceUniProtKBSearchTerm> children =
                    convert(childFieldItems, comparatorByChildNumber);
            queue.addAll(children);
            currentItem.setItems(children);
        }
        return rootSearchTermConfigs;
    }

    public static List<SearchFieldItem> getTopLevelFieldItems(SearchFieldConfig searchFieldConfig) {
        return searchFieldConfig.getAllFieldItems().stream()
                .filter(AdvanceUniProtKBSearchTerm::isTopLevel)
                .collect(Collectors.toList());
    }

    public static List<SearchFieldItem> getChildFieldItems(
            SearchFieldConfig searchFieldConfig, String parentId) {
        return searchFieldConfig.getAllFieldItems().stream()
                .filter(fi -> isChildOf(parentId, fi))
                .collect(Collectors.toList());
    }

    private static AdvanceUniProtKBSearchTerm from(SearchFieldItem fi) {
        AdvanceUniProtKBSearchTerm.AdvanceUniProtKBSearchTermBuilder b =
                AdvanceUniProtKBSearchTerm.builder();
        b.id(fi.getId()).parentId(fi.getParentId()).childNumber(fi.getChildNumber());
        b.seqNumber(fi.getSeqNumber()).label(fi.getLabel()).term(fi.getFieldName());
        b.description(fi.getDescription())
                .example(fi.getExample())
                .autoComplete(fi.getAutoComplete());
        b.autoCompleteQueryTerm(fi.getAutoCompleteQueryField())
                .autoCompleteQueryFieldValidRegex(fi.getAutoCompleteQueryFieldValidRegex());
        b.regex(fi.getValidRegex());
        if (fi.getItemType() != null) {
            b.itemType(fi.getItemType().name());
        }
        if (fi.getDataType() != null) {
            b.dataType(fi.getDataType().name());
        }
        if (fi.getFieldType() != null) {
            b.fieldType(fi.getFieldType().name());
            if (fi.getFieldType() == SearchFieldType.evidence) {
                b.evidenceGroups(AnnotationEvidences.INSTANCE.getEvidences());
            }
        }

        List<SearchFieldItem.Value> values = fi.getValues();
        if (values != null) {
            List<AdvanceUniProtKBSearchTerm.Value> stcValues =
                    values.stream()
                            .map(
                                    value ->
                                            new AdvanceUniProtKBSearchTerm.Value(
                                                    value.getName(), value.getValue()))
                            .collect(Collectors.toList());
            b.values(stcValues);
        }

        return b.build();
    }

    private static List<AdvanceUniProtKBSearchTerm> convert(
            List<SearchFieldItem> fieldItems, Comparator<AdvanceUniProtKBSearchTerm> comparator) {
        return fieldItems.stream()
                .map(AdvanceUniProtKBSearchTerm::from)
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private static boolean isChildOf(String parentId, SearchFieldItem fieldItem) {
        return parentId.equals(fieldItem.getParentId());
    }

    private static boolean isTopLevel(SearchFieldItem fi) {
        return StringUtils.isBlank(fi.getParentId()) && fi.getSeqNumber() != null;
    }
}
