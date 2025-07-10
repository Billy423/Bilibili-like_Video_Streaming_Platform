package com.example.bilibili.api;

import com.example.bilibili.domain.JsonResponse;
import com.example.bilibili.domain.Video;
import com.example.bilibili.service.DemoService;
import com.example.bilibili.service.ElasticSearchService;
import com.example.bilibili.service.util.FastDFSUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
public class DemoApi {

    @Autowired
    private DemoService demoService;

    @Autowired
    public FastDFSUtil fastDFSUtil;
    @Autowired
    private ElasticSearchService elasticSearchService;

    @GetMapping("/query")
    public Map<String, Object> query(Long id) {
        return demoService.query(id);
    }

    @GetMapping("/slices")
    public void slices(MultipartFile file) throws Exception {
        fastDFSUtil.convertFileToSlices(file);
    }

    @GetMapping("/es-videos")
    public JsonResponse<Video> getEsVideos(@RequestParam String keyword) {
        Video video = elasticSearchService.getVideos(keyword);
        return new JsonResponse<>(video);
    }
}
