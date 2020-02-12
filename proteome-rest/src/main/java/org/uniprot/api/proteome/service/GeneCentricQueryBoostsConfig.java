package org.uniprot.api.proteome.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.common.repository.search.QueryBoostsFileReader;

@Configuration
public class GeneCentricQueryBoostsConfig {
    private static final String BOOSTS_RESOURCE_LOCATION = "/genecentric-query-boosts.config";

    @Bean
    public QueryBoosts geneCentricQueryBoosts() {
        return new QueryBoostsFileReader(BOOSTS_RESOURCE_LOCATION).getQueryBoosts();
    }
}
