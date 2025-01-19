//package com.easy.elasticsearch.config;
//
//import com.easy.elastic.search.client.EsQueryClient;
//import org.apache.http.HttpHost;
//import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
//import org.elasticsearch.client.RestClient;
//import org.elasticsearch.client.RestClientBuilder;
//import org.elasticsearch.client.RestHighLevelClient;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Configuration
//public class TestConfig {
//    @Bean
//    public EsQueryClient bean() {
//        return new EsQueryClient();
//    }
//    @Value("${spring.elasticsearch.rest.uris}")
//    private String[] elasticsearchUris;
//
//    @Value("${spring.elasticsearch.rest.username}")
//    private String elasticsearchUsername;
//
//    @Value("${spring.elasticsearch.rest.password}")
//    private String elasticsearchPassword;
//
////    @Bean
////    public RestHighLevelClient restHighLevelClient() {
////        RestClientBuilder builder = RestClient.builder(getHttpHosts())
////                .setHttpClientConfigCallback(httpClientBuilder -> {
////                    httpClientBuilder.setMaxConnPerRoute(100);
////                    httpClientBuilder.setMaxConnTotal(200);
////                    return httpClientBuilder;
////                })
////                .setRequestConfigCallback(requestConfigBuilder -> {
////                    // 配置其他请求参数，如连接超时时间、Socket超时时间等
////                    return requestConfigBuilder;
////                });
////
////        return new RestHighLevelClient(builder);
////    }
////    @Bean
////    public RestHighLevelClient res() {
////        RestClientBuilder builder = RestClient.builder(getHttpHosts());
////        builder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
////            @Override
////            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
////                httpClientBuilder.setMaxConnTotal(100);
////                return httpClientBuilder;
////            }
////        });
////
////        return new RestHighLevelClient(builder);
////    }
////    private HttpHost[] getHttpHosts() {
////        List<HttpHost> hosts = new ArrayList<>();
////        for (String uri : elasticsearchUris) {
////            hosts.add(HttpHost.create(uri));
////        }
////        return hosts.toArray(new HttpHost[0]);
////    }
//}
