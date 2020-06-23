package org.uniprot.api.uniprotkb.controller.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidAccessionList;
import org.uniprot.api.rest.validation.ValidContentTypes;
import org.uniprot.api.rest.validation.ValidFacets;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotKBFacetConfig;
import org.uniprot.store.config.UniProtDataType;
import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
public class GetByAccessionsRequest implements SearchRequest {

    @NotNull(message = "{search.required}")
    @ValidAccessionList
    private String accessions;//comma separated list of accessions

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "uniprotkb-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPROTKB)
    private String fields;

    @Parameter(description = "Name of the facet search")
    @ValidFacets(facetConfig = UniprotKBFacetConfig.class)
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    private String facets;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Size of the result. Defaults to 25")
    @Positive(message = "{search.positive}")
    private Integer size;

    @Override
    public String getQuery() {
        return null;
    }

    @Override
    public String getSort() {
        return null;
    }
}
