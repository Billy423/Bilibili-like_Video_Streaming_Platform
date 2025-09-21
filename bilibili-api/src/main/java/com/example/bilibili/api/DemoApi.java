package com.example.bilibili.api;

import com.example.bilibili.domain.JsonResponse;
import com.example.bilibili.domain.Video;
import com.example.bilibili.service.DemoService;
import com.example.bilibili.service.ElasticSearchService;
import com.example.bilibili.service.feign.MsDeclareService;
import com.example.bilibili.service.util.FastDFSUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Demo API Controller - Demonstrates various technical features
 * Key features: FastDFS file slicing, Elasticsearch integration, microservices communication, circuit breaker pattern
 */
@RestController
public class DemoApi {

    @Autowired
    private DemoService demoService;

    @Autowired
    public FastDFSUtil fastDFSUtil; // For distributed file storage and video slicing
    @Autowired
    private ElasticSearchService elasticSearchService; // For search functionality

    @Autowired
    private MsDeclareService msDeclareService; // Feign client for microservice communication

    @Autowired
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory; // Resilience4j circuit breaker

    @GetMapping("/query")
    public Map<String, Object> query(Long id) {
        return demoService.query(id);
    }

    @GetMapping("/slices")
    public void slices(MultipartFile file) throws Exception {
        // Convert video file to slices for streaming (FastDFS)
        fastDFSUtil.convertFileToSlices(file);
    }

    @GetMapping("/es-videos")
    public JsonResponse<Video> getEsVideos(@RequestParam String keyword) {
        // Search videos using Elasticsearch
        Video video = elasticSearchService.getVideos(keyword);
        return new JsonResponse<>(video);
    }

    @GetMapping("/demos")
    public Long msget(@RequestParam Long id) {
        // Microservice communication via Feign client
        return msDeclareService.msget(id);
    }

    @PostMapping("/demos")
    public Map<String, Object> mspost(@RequestBody Map<String, Object> params) {
        // Microservice communication via Feign client
        return msDeclareService.mspost(params);
    }

    // Resilience4j Circuit Breaker Example
    @GetMapping("/timeout")
    public String circuitBreakerWithResilience4j(@RequestParam Long time) {
        // Demonstrate circuit breaker pattern for fault tolerance
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("timeoutCircuitBreaker");
        return circuitBreaker.run(
                () -> msDeclareService.timeout(time),
                throwable -> fallback(time) // Fallback when service fails
        );
    }

    // Fallback method for Circuit Breaker
    public String fallback(Long time) {
        return "Timeout error! Service took too long for input time: " + time + "ms";
    }
}
