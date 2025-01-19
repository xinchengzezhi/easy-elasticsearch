package com.easy.elastic.search.config;

import com.easy.elastic.search.client.EsQueryClient;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置
 */
@Slf4j
@Configuration
public class EsConfig {

    @Bean("esQueryClient")
    @ConditionalOnMissingBean(EsQueryClient.class)
    public EsQueryClient EsQueryClient() {
        log.info("com.easy.elastic.search.config.EsQueryClient initialized");
        return new EsQueryClient();
    }

}

