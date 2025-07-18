package com.example.bilibili.service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient("bilibili-ms-api")
public interface MsDeclareService {

    @GetMapping("/demos")
    public Long msget(@RequestParam Long id);

    @PostMapping("/demos")
    public Map<String, Object> mspost(@RequestBody Map<String, Object> params);

    @GetMapping("/timeout")
    public String timeout(@RequestParam Long time);

}