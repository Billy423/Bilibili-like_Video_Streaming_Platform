package com.example.bilibili.service.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.TransportUtils;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import jakarta.annotation.PreDestroy;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfig {

    @Value("${elasticsearch.url}")
    private String esUrl;

    private RestClient restClient;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // Create RestClient
        RestClientBuilder restClientBuilder = RestClient.builder(HttpHost.create(esUrl));
        this.restClient = restClientBuilder.build();

        // Create ElasticsearchTransport
        RestClientTransport transport = new RestClientTransport(
                restClient,
                new co.elastic.clients.json.jackson.JacksonJsonpMapper()
        );

        // Create ElasticsearchClient
        return new ElasticsearchClient(transport);
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (restClient != null) {
                restClient.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}