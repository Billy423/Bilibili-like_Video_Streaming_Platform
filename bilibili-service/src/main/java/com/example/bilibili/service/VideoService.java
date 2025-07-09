package com.example.bilibili.service;

import com.example.bilibili.dao.VideoDao;
import com.example.bilibili.domain.*;
import com.example.bilibili.domain.exception.ConditionException;
import com.example.bilibili.service.util.FastDFSUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VideoService {

    @Autowired
    private VideoDao videoDao;

    @Autowired
    private FastDFSUtil fastDFSUtil;

    @Autowired
    private UserCoinService userCoinService;

    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    @Autowired
    private UserMomentsService userMomentsService;

    private static final int DEFAULT_RECOMMEND_NUMBER = 3;

    private static final int FRAME_NO = 256;

    @Value("${fdfs.http.storage-addr")
    private String fastdfsUrl;

    @Transactional
    public void addVideos(Video video) {
        Date now = new Date();
        video.setCreateTime(new Date());
        videoDao.addVideos(video);

        // Save video tag
        Long videoId = video.getId();
        List<VideoTag> tagList = video.getVideoTagList();
        tagList.forEach(item -> {
            item.setCreateTime(now);
            item.setVideoId(videoId);
        });
        videoDao.batchAddVideoTags(tagList);
    }

    public PageResult<Video> pageListVideos(Integer size, Integer no, String area) {
        Map<String, Object> params = new HashMap<>();
        params.put("start", (no-1)*size);
        params.put("limit", size);
        params.put("area" , area);
        List<Video> list = new ArrayList<>();
        Integer total = videoDao.pageCountVideos(params);
        if (total > 0) {
            list = videoDao.pageListVideos(params);
        }
        return new PageResult<>(total, list);
    }

    public void viewVideoOnlineBySlices(HttpServletRequest request,
                                        HttpServletResponse response,
                                        String url) {
        try {
            fastDFSUtil.viewVideoOnlineBySlices(request, response, url);
        } catch (Exception ignored){}
    }

    public void addVideoLike(Long videoId, Long userId) {
        Video video = videoDao.getVideoById(videoId);
        if (video == null) {
            throw new ConditionException("Illegal video！");
        }
        VideoLike videoLike = videoDao.getVideoLikeByVideoIdAndUserId(videoId, userId);
        if (videoLike != null) {
            throw new ConditionException("Already liked！");
        }
        videoLike = new VideoLike();
        videoLike.setVideoId(videoId);
        videoLike.setUserId(userId);
        videoLike.setCreateTime(new Date());
        videoDao.addVideoLike(videoLike);
    }

    public void deleteVideoLike(Long videoId, Long userId) {
        videoDao.deleteVideoLike(videoId, userId);
    }

    public Map<String, Object> getVideoLikes(Long videoId, Long userId) {
        Long count = videoDao.getVideoLikes(videoId);
        VideoLike videoLike = videoDao.getVideoLikeByVideoIdAndUserId(videoId, userId);
        boolean like = videoLike != null;
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("like", like);
        return result;
    }

    @Transactional
    public void addVideoCollection(VideoCollection videoCollection, Long userId) {
        Long videoId = videoCollection.getVideoId();
        Long groupId = videoCollection.getGroupId();
        if(videoId == null || groupId == null){
            throw new ConditionException("Parameter exception！");
        }
        Video video = videoDao.getVideoById(videoId);
        if(video == null){
            throw new ConditionException("Illegal video！");
        }
        // Delete old video
        videoDao.deleteVideoCollection(videoId, userId);
        // Add new collection
        videoCollection.setUserId(userId);
        videoCollection.setCreateTime(new Date());
        videoDao.addVideoCollection(videoCollection);
    }

    @Transactional
    public void updateVideoCollection(VideoCollection videoCollection, Long userId) {
        Long videoId = videoCollection.getVideoId();
        Long groupId = videoCollection.getGroupId();
        if (videoId == null || groupId == null) {
            throw new ConditionException("Illegal parameter！");
        }
        Video video = videoDao.getVideoById(videoId);
        if (video == null) {
            throw new ConditionException("Illegal video！");
        }
        videoCollection.setUserId(userId);
        videoDao.updateVideoCollection(videoCollection);
    }

    public void deleteVideoCollection(Long videoId, Long userId) {
        videoDao.deleteVideoCollection(videoId, userId);
    }

    public Map<String, Object> getVideoCollections(Long videoId, Long userId) {
        Long count = videoDao.getVideoCollections(videoId);
        VideoCollection videoCollection = videoDao.getVideoCollectionByVideoIdAndUserId(videoId, userId);
        boolean like = videoCollection != null;
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("like", like);
        return result;
    }

    @Transactional
    public void addVideoCoins(VideoCoin videoCoin, Long userId) {
        Long videoId = videoCoin.getVideoId();
        Integer amount = videoCoin.getAmount();
        if (videoId == null) {
            throw new ConditionException("Parameter exception！");
        }
        Video video = videoDao.getVideoById(videoId);
        if (video == null) {
            throw new ConditionException("Illegal video！");
        }
        // Check whether current user has enough coins
        Integer userCoinsAmount = userCoinService.getUserCoinsAmount(userId);
        userCoinsAmount = userCoinsAmount == null ? 0 : userCoinsAmount;
        if (amount > userCoinsAmount) {
            throw new ConditionException("Coin amount not enough！");
        }
        // Check how many coins the user has deposited for current video
        VideoCoin dbVideoCoin = videoDao.getVideoCoinByVideoIdAndUserId(videoId, userId);
        // Increase coin deposit
        if (dbVideoCoin == null) {
            videoCoin.setUserId(userId);
            videoCoin.setCreateTime(new Date());
            videoDao.addVideoCoin(videoCoin);
        } else {
            Integer dbAmount = dbVideoCoin.getAmount();
            dbAmount += amount;
            // Update coin deposit
            videoCoin.setUserId(userId);
            videoCoin.setAmount(dbAmount);
            videoCoin.setUpdateTime(new Date());
            videoDao.updateVideoCoin(videoCoin);
        }
        // Update user total coin amount
        userCoinService.updateUserCoinsAmount(userId, (userCoinsAmount - amount));
    }

    public Map<String, Object> getVideoCoins(Long videoId, Long userId) {
        Long count = videoDao.getVideoCoinsAmount(videoId);
        VideoCoin videoCollection = videoDao.getVideoCoinByVideoIdAndUserId(videoId, userId);
        boolean like = videoCollection != null;
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("like", like);
        return result;
    }

    public void addVideoComment(VideoComment videoComment, Long userId) {
        Long videoId = videoComment.getVideoId();
        if (videoId == null) {
            throw new ConditionException("Parameter exception！");
        }
        Video video = videoDao.getVideoById(videoId);
        if (video == null) {
            throw new ConditionException("Illegal video！");
        }
        videoComment.setUserId(userId);
        videoComment.setCreateTime(new Date());
        videoDao.addVideoComment(videoComment);
    }

    public PageResult<VideoComment> pageListVideoComments(Integer size, Integer no, Long videoId) {
        Video video = videoDao.getVideoById(videoId);
        if (video == null) {
            throw new ConditionException("Illegal video！");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("start", (no-1)*size);
        params.put("limit", size);
        params.put("videoId", videoId);
        Integer total = videoDao.pageCountVideoComments(params);
        List<VideoComment> list = new ArrayList<>();
        if (total > 0) {
            list = videoDao.pageListVideoComments(params);
            if (!list.isEmpty()) {
                // Batch query for secondary comments
                List<Long> parentIdList = list.stream().map(VideoComment::getId).collect(Collectors.toList());
                // Batch query for user information
                Set<Long> userIdList = list.stream().map(VideoComment::getUserId).collect(Collectors.toSet());
                List<VideoComment> childCommentList = videoDao.batchGetVideoCommentsByRootIds(parentIdList);
                Set<Long> replyUserIdList = childCommentList.stream()
                        .map(VideoComment::getUserId).collect(Collectors.toSet());
                Set<Long> childUserIdList = childCommentList.stream()
                        .map(VideoComment::getReplyUserId).collect(Collectors.toSet());
                userIdList.addAll(replyUserIdList);
                userIdList.addAll(childUserIdList);
                List<UserInfo> userInfoList = userService.batchGetUserInfoByUserIds(userIdList);
                Map<Long, UserInfo> userInfoMap = userInfoList.stream()
                        .collect(Collectors.toMap(UserInfo:: getUserId, userInfo -> userInfo));
                list.forEach(comment -> {
                    Long id = comment.getId();
                    List<VideoComment> childList = new ArrayList<>();
                    childCommentList.forEach(child -> {
                        if (id.equals(child.getRootId())) {
                            child.setUserInfo(userInfoMap.get(child.getUserId()));
                            child.setReplyUserInfo(userInfoMap.get(child.getReplyUserId()));
                            childList.add(child);
                        }
                    });
                    comment.setChildList(childList);
                    comment.setUserInfo(userInfoMap.get(comment.getUserId()));
                });
            }
        }
        return new PageResult<>(total, list);
    }

    public Map<String, Object> getVideoDetails(Long videoId) {
        Video video =  videoDao.getVideoDetails(videoId);
        Long userId = video.getUserId();
        User user = userService.getUserInfo(userId);
        UserInfo userInfo = user.getUserInfo();
        Map<String, Object> result = new HashMap<>();
        result.put("video", video);
        result.put("userInfo", userInfo);
        return result;
    }
}
