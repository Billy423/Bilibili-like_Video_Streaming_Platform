package com.example.bilibili.service;

import com.example.bilibili.dao.VideoDao;
import com.example.bilibili.domain.*;
import com.example.bilibili.domain.exception.ConditionException;
import com.example.bilibili.service.util.FastDFSUtil;
import com.example.bilibili.service.util.IpUtil;
import eu.bitwalker.useragentutils.UserAgent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
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

    public void addVideoView(VideoView videoView, HttpServletRequest request) {
        Long userId = videoView.getUserId();
        Long videoId = videoView.getVideoId();
        // Generate clientId
        String agent = request.getHeader("User-Agent");
        UserAgent userAgent = UserAgent.parseUserAgentString(agent);
        String clientId = String.valueOf(userAgent.getId());
        String ip = IpUtil.getIP(request);
        Map<String, Object> params = new HashMap<>();
        if (userId != null) {
            params.put("userId", userId);
        } else {
            params.put("ip", ip);
            params.put("clientId", clientId);
        }
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        params.put("today", sdf.format(now));
        params.put("videoId", videoId);
        // Add video view
        VideoView dbVideoView = videoDao.getVideoView(params);
        if (dbVideoView == null) {
            videoView.setIp(ip);
            videoView.setClientId(clientId);
            videoView.setCreateTime(new Date());
            videoDao.addVideoView(videoView);
        }
    }

    public Integer getVideoViewCounts(Long videoId) {
        return videoDao.getVideoViewCounts(videoId);
    }

    public List<Video> getVideoCount(List<Video> videoList){
        if(!videoList.isEmpty()){
            Set<Long> videoIdSet = videoList.stream().map(Video :: getId)
                    .collect(Collectors.toSet());
            Map<Long, Integer> viewCountMap = this.batchCountVideoView(videoIdSet);
            Map<Long, Integer> danmuCountMap = this.batchCountVideoDanmu(videoIdSet);
            videoList.forEach(video -> {
                video.setViewCount(viewCountMap.get(video.getId()));
                video.setDanmuCount(danmuCountMap.get(video.getId()));
            });
        }
        return videoList;
    }

    /**
     * Collaborative filtering based on user
     * @param userId user id
     */
    public List<Video> recommend(Long userId) throws TasteException {
        List<UserPreference> list = videoDao.getAllUserPreference();
        // Create data model
        DataModel dataModel = this.createDataModel(list);
        // Get user similarity
        UserSimilarity similarity = new UncenteredCosineSimilarity(dataModel);
        // Get neighborhood user
        UserNeighborhood userNeighborhood = new NearestNUserNeighborhood(2, similarity, dataModel);
        long[] ar = userNeighborhood.getUserNeighborhood(userId);
        // Construct recommender
        Recommender recommender = new GenericUserBasedRecommender(dataModel, userNeighborhood, similarity);
        // Recommend videos
        List<RecommendedItem> recommendedItems = recommender.recommend(userId, 5);
        List<Long> itemIds = recommendedItems.stream().map(RecommendedItem::getItemID).collect(Collectors.toList());
        return videoDao.batchGetVideosByIds(itemIds);
    }

    /**
     * Collaborative filtering based on content
     * @param userId user id
     * @param itemId item id
     * @param howMany amount to recommend
     */
    public List<Video> recommendByItem(Long userId, Long itemId, int howMany) throws TasteException {
        List<UserPreference> list = videoDao.getAllUserPreference();
        //Create data model
        DataModel dataModel = this.createDataModel(list);
        //Get user similarity
        ItemSimilarity similarity = new UncenteredCosineSimilarity(dataModel);
        GenericItemBasedRecommender genericItemBasedRecommender = new GenericItemBasedRecommender(dataModel, similarity);
        // Item recommendation similarity: Calculate the number of times two items appear together; the more frequently they appear together, the higher their similarity is considered.
        List<Long> itemIds = genericItemBasedRecommender.recommendedBecause(userId, itemId, howMany)
                .stream()
                .map(RecommendedItem::getItemID)
                .collect(Collectors.toList());
        //Recommend videos
        return videoDao.batchGetVideosByIds(itemIds);
    }

    private DataModel createDataModel(List<UserPreference> userPreferenceList) {
        FastByIDMap<PreferenceArray> fastByIdMap = new FastByIDMap<>();
        Map<Long, List<UserPreference>> map = userPreferenceList.stream().collect(Collectors.groupingBy(UserPreference::getUserId));
        Collection<List<UserPreference>> list = map.values();
        for(List<UserPreference> userPreferences : list){
            GenericPreference[] array = new GenericPreference[userPreferences.size()];
            for(int i = 0; i < userPreferences.size(); i++){
                UserPreference userPreference = userPreferences.get(i);
                GenericPreference item = new GenericPreference(userPreference.getUserId(), userPreference.getVideoId(), userPreference.getValue());
                array[i] = item;
            }
            fastByIdMap.put(array[0].getUserID(), new GenericUserPreferenceArray(Arrays.asList(array)));
        }
        return new GenericDataModel(fastByIdMap);
    }

    public Map<Long, Integer>  batchCountVideoView(Set<Long> videoIdSet){
        List<VideoViewCount> viewCount = videoDao.getVideoViewCountByVideoIds(videoIdSet);
        return viewCount.stream()
                .collect(Collectors.toMap(VideoViewCount::getVideoId,
                        VideoViewCount::getCount));
    }

    public Map<Long, Integer> batchCountVideoDanmu(Set<Long> videoIdSet){
        List<VideoDanmuCount> danmuCount = videoDao.getVideoDanmuCountByVideoIds(videoIdSet);
        return danmuCount.stream()
                .collect(Collectors.toMap(VideoDanmuCount::getVideoId,
                        VideoDanmuCount::getCount));
    }

    public List<VideoTag> getVideoTagsByVideoId(Long videoId) {
        return videoDao.getVideoTagsByVideoId(videoId);
    }

    public void deleteVideoTags(List<Long> tagIdList, Long videoId) {
        videoDao.deleteVideoTags(tagIdList, videoId);
    }

    public List<Video> getVideoRecommendations(String recommendType, Long userId){
        List<Video> list = new ArrayList<>();
        try {
            // Recommend based on the recommendation type:
            //	1.	User-based recommendation
            //	2.	Content-based recommendation
            if("1".equals(recommendType)){
                list = this.recommend(userId);
            }else{
                // Identify the user’s favorite video to serve as the basis for the recommendation.
                List<UserPreference> preferencesList = videoDao.getAllUserPreference();
                Optional<Long> itemIdOpt = preferencesList.stream().filter(item -> item.getUserId().equals(userId))
                        .max(Comparator.comparing(UserPreference :: getValue)).map(UserPreference::getVideoId);
                if(itemIdOpt.isPresent()){
                    list = this.recommendByItem(userId, itemIdOpt.get(), DEFAULT_RECOMMEND_NUMBER);
                }
            }
            // If no recommendation content is calculated, the latest videos will be displayed by default.
            if(list.isEmpty()){
                list = this.pageListVideos(3,1,null).getList();
            }else{
                list.forEach(video -> video.setThumbnail(fastdfsUrl+video.getThumbnail()));
            }
        }catch (Exception e){
            throw new ConditionException("Recommendaiton failed");
        }
        return list;
    }

    public List<Video> getVisitorVideoRecommendations() {
        return this.pageListVideos(DEFAULT_RECOMMEND_NUMBER,1,null).getList();
    }


}
