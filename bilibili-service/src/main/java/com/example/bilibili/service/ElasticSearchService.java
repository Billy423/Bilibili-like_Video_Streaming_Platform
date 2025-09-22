package com.example.bilibili.service;

import com.example.bilibili.dao.repository.UserInfoRepository;
import com.example.bilibili.dao.repository.VideoRepository;
import com.example.bilibili.domain.UserInfo;
import com.example.bilibili.domain.Video;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.bilibili.domain.constant.SearchConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class ElasticSearchService {

    @Autowired
    @Lazy
    private VideoRepository videoRepository;

    @Autowired
    @Lazy
    private UserInfoRepository userInfoRepository;

    @Autowired
    @Lazy
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

    public Page<Video> pageListSearchVideos(String keyword, Integer pageSize, Integer pageNo, String searchType) {
        PageRequest pageRequest = PageRequest.of(pageNo, pageSize);
        switch (searchType) {
            case SearchConstant.CREATE_TIME:
                return videoRepository.findByTitleOrDescriptionOrderByCreateTimeDesc(keyword, keyword, pageRequest);
            case SearchConstant.DANMU_COUNT:
                return videoRepository.findByTitleOrDescriptionOrderByDanmuCountDesc(keyword, keyword, pageRequest);
            default:
                return videoRepository.findByTitleOrDescriptionOrderByViewCountDesc(keyword, keyword, pageRequest);
        }
    }

    public Page<UserInfo> pageListSearchUsers(String keyword, Integer pageSize, Integer pageNo, String searchType) {
        PageRequest pageRequest = PageRequest.of(pageNo, pageSize);
        switch (searchType) {
            case SearchConstant.USER_FAN_COUNT_ASC:
                return userInfoRepository.findByNickOrderByFanCountAsc(keyword, pageRequest);
            default:
                return userInfoRepository.findByNickOrderByFanCountDesc(keyword, pageRequest);
        }
    }

}