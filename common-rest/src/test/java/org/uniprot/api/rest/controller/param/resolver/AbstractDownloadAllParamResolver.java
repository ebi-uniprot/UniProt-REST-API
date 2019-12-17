package org.uniprot.api.rest.controller.param.resolver;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.output.UniProtMediaType;

/**
 * Parameter resolver for tests download all/everything without any filter for all supported content
 * types
 */
public abstract class AbstractDownloadAllParamResolver extends BaseDownloadParamResolver
        implements ParameterResolver {

    @Override
    public boolean supportsParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(DownloadParamAndResult.class);
    }

    @Override
    public Object resolveParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        DownloadParamAndResult result = null;
        Method method =
                extensionContext
                        .getTestMethod()
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "AbstractParamResolverForTestDownloadAll: Unable to find test method"));
        switch (method.getName()) {
            case "testDownloadAllJSON":
                result = getDownloadAllParamAndResult(MediaType.APPLICATION_JSON);
                break;
            case "testDownloadAllTSV":
                result = getDownloadAllParamAndResult(UniProtMediaType.TSV_MEDIA_TYPE);
                break;
            case "testDownloadAllList":
                result = getDownloadAllParamAndResult(UniProtMediaType.LIST_MEDIA_TYPE);
                break;
            case "testDownloadAllOBO":
                result = getDownloadAllParamAndResult(UniProtMediaType.OBO_MEDIA_TYPE);
                break;
            case "testDownloadAllXLS":
                result = getDownloadAllParamAndResult(UniProtMediaType.XLS_MEDIA_TYPE);
                break;
        }
        return result;
    }

    protected abstract DownloadParamAndResult getDownloadAllParamAndResult(MediaType contentType);
}