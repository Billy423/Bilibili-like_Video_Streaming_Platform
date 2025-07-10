package com.example.bilibili.service;

import com.example.bilibili.dao.repository.UserInfoRepository;
import com.example.bilibili.dao.repository.VideoRepository;
import com.example.bilibili.domain.UserInfo;
import com.example.bilibili.domain.Video;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ElasticSearchService {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    public void addUserInfo(UserInfo userInfo) {
        userInfoRepository.save(userInfo);
    }

    public void addVideo(Video video) {
        videoRepository.save(video);
    }

    public List<Map<String, Object>> getContents(String keyword, Integer pageNo, Integer pageSize) throws IOException {
        String[] indices = {"videos", "user-infos"};

        SearchRequest request = SearchRequest.of(s -> s
                .index(Arrays.asList(indices))
                .from((pageNo - 1) * pageSize)
                .size(pageSize)
                .query(q -> q
                        .multiMatch(m -> m
                                .query(keyword)
                                .fields("title", "nick", "description")
                        )
                )
                .highlight(h -> h
                        .fields("title", f -> f)
                        .fields("nick", f -> f)
                        .fields("description", f -> f)
                        .preTags("<span style=\"color:red\">")
                        .postTags("</span>")
                )
                .timeout("60s")
        );

        SearchResponse<Map> response = elasticsearchClient.search(request, Map.class);
        List<Map<String, Object>> results = new ArrayList<>();
        for (Hit<Map> hit : response.hits().hits()) {
            Map source = hit.source();
            if (source != null) {
                Map<String, Object> sourceMap = new HashMap<>();
                source.forEach((key, value) -> sourceMap.put(String.valueOf(key), value));

                Map<String, List<String>> highlights = hit.highlight();
                if (highlights != null) {
                    highlights.forEach((field, values) -> {
                        if (values != null && !values.isEmpty()) {
                            sourceMap.put(field, String.join(", ", values));
                        }
                    });
                }

                results.add(sourceMap);
            }
        }

        return results;
    }

    public Video getVideos(String keyword) {
        return videoRepository.findByTitleLike(keyword);
    }

    public void deleteAllVideos() {
        videoRepository.deleteAll();
    }

    public long countVideoBySearchTxt(String searchTxt) {
        return this.videoRepository.countByTitleOrDescription(searchTxt, searchTxt);
    }

    public long countUserBySearchTxt(String searchTxt) {
        return this.userInfoRepository.countByNick(searchTxt);
    }

    public void updateVideoViewCount(Long videoId) {
        Optional<Video> videoOpt = videoRepository.findById(videoId);
        videoOpt.ifPresent(video -> {
            int viewCount = video.getViewCount() == null ? 0 : video.getViewCount();
            video.setViewCount(viewCount + 1);
            videoRepository.save(video);
        });
    }

}