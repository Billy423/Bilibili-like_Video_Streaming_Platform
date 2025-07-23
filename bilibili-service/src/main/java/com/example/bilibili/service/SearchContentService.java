package com.example.bilibili.service;

import com.example.bilibili.domain.UserInfo;
import com.example.bilibili.domain.Video;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class SearchContentService {

    @Autowired
    private ElasticSearchService elasticSearchService;

    @Value("${fdfs.http.storage-addr}")
    private String fastdfsUrl;

    public Map<String, Object> countBySearchTxt(String searchTxt) {
        Map<String, Object> result = new HashMap<>();
        long videoCount = elasticSearchService.countVideoBySearchTxt(searchTxt); // count video
        long userCount = elasticSearchService.countUserBySearchTxt(searchTxt); // count user
        // Construct returns
        result.put("videoCount", videoCount);
        result.put("userCount", userCount);
        return result;
    }

    public Page<Video> pageListSearchVideos(String keyword,Integer pageSize,
                                            Integer pageNo, String searchType) {
        Page<Video> result = elasticSearchService.pageListSearchVideos(keyword, pageSize,
                pageNo-1, searchType);
        result.getContent().forEach(item  -> item.setThumbnail(fastdfsUrl + item.getThumbnail()));
        return result;
    }

    public Page<UserInfo> pageListSearchUsers(String keyword, Integer pageSize,
                                              Integer pageNo, String searchType) {
        Page<UserInfo> result = elasticSearchService.pageListSearchUsers(keyword, pageSize,
                pageNo-1, searchType);
        return result;
    }
}
