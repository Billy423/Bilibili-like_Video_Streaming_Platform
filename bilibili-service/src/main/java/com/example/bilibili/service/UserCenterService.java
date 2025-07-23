package com.example.bilibili.service;

import com.example.bilibili.dao.UserCenterDao;
import com.example.bilibili.domain.*;
import com.example.bilibili.domain.constant.UserCollectionGroupConstant;
import com.example.bilibili.domain.exception.ConditionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserCenterService {

    @Autowired
    private UserCenterDao userCenterDao;

    @Autowired
    private UserService userService;

    @Autowired
    private VideoService videoService;

    @Value("${fdfs.http.storage-addr}")
    private String fastdfsUrl;

    public Map<String, Integer> getUserCenterVideoAreas(Long userId) {
        List<VideoArea> videoAreas = userCenterDao.getUserCenterVideoAreas(userId);
        return videoAreas.stream().
                collect(Collectors.toMap(VideoArea::getArea,VideoArea::getCount));
    }

    public PageResult<Video> pageListUserVideos(Integer size, Integer no,
                                                String area, Long userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("start", (no-1)*size);
        params.put("limit", size);
        params.put("area" , area);
        params.put("userId", userId);
        Integer total = userCenterDao.pageCountUserCenterVideos(params);
        List<Video> list = new ArrayList<>();
        if(total > 0){
            list = userCenterDao.pageListUserCenterVideos(params);
            // turn relative path into absolute path
            list.forEach(video -> video.setThumbnail(fastdfsUrl + video.getThumbnail()));
            // get video and danmu count
            list = videoService.getVideoCount(list);
        }
        return new PageResult<>(total, list);
    }

    public Map<String, Object> pageListUserCenterCollections(Integer size, Integer no,
                                                             Long userId, Long groupId) {
        List<CollectionGroup> groups = userCenterDao.countUserCenterCollectionGroups(userId);
        Map<String, Object> params = new HashMap<>();
        params.put("start", (no-1)*size);
        params.put("limit", size);
        params.put("userId", userId);
        params.put("groupId", groupId);
        Integer total = userCenterDao.pageCountUserCollections(params);
        List<VideoCollection> list = new ArrayList<>();
        if(total > 0){
            list = userCenterDao.pageListUserCollections(params);
            // Get info relative to collection videos
            if(!list.isEmpty()){
                Set<Long> videoIdSet = list.stream().map(VideoCollection :: getVideoId)
                        .collect(Collectors.toSet());
                List<Video> videoList = userCenterDao.getVideoInfoByIds(videoIdSet);
                videoList.forEach(video -> video.setThumbnail(fastdfsUrl + video.getThumbnail()));
                list.forEach(item -> videoList.forEach(video -> {
                    if(video.getId().equals(item.getVideoId())){
                        item.setVideoInfo(video);
                    }
                }));
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("pageResult", new PageResult<>(total, list));
        result.put("groups",groups);
        return result;
    }
    public void addUserCollectionGroups(VideoCollectionGroup videoCollectionGroup) {
        videoCollectionGroup.setCreateTime(new Date());
        videoCollectionGroup.setType(UserCollectionGroupConstant.TYPE_USER);
        userCenterDao.addUserCollectionGroups(videoCollectionGroup);
    }


    public PageResult<UserFollowing>  pageListUserCenterFollowings(Long userId,
                                                                   Integer size,
                                                                   Integer no, Long groupId) {
        Map<String, Object> params = new HashMap<>();
        params.put("start", (no-1)*size);
        params.put("limit", size);
        params.put("userId", userId);
        params.put("groupId", groupId);
        List<UserFollowing> userFollowings = new ArrayList<>();
        Integer total = userCenterDao.pageCountUserCenterFollowings(params);
        if(total > 0){
            userFollowings = userCenterDao.pageListUserCenterFollowings(params);
            if(!userFollowings.isEmpty()){
                Set<Long> followingUserIdSet = userFollowings.stream().map(UserFollowing::getFollowingId).collect(Collectors.toSet());
                List<UserInfo> userInfos = userCenterDao.getUserInfoByIds(followingUserIdSet);
                Map<Long, List<UserInfo>> userInfoMap = userInfos.stream().collect(Collectors.groupingBy(UserInfo::getUserId));
                userFollowings.forEach(userFollowing -> userFollowing.setUserInfo(userInfoMap.get(userFollowing.getFollowingId()).get(0)));
            }
        }
        return new PageResult<>(total, userFollowings);
    }

    public PageResult<UserFollowing> pageListUserFans(Long userId, Integer size, Integer no) {
        if(size == null || no == null){
            throw new ConditionException("Invalid parameter!");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("start", (no-1)*size);
        params.put("limit", size);
        params.put("userId", userId);
        Integer total = userCenterDao.pageCountUserFans(params);
        List<UserFollowing> fans = new ArrayList<>();
        if(total > 0){
            fans = userCenterDao.pageListUserFans(params);
            if(!fans.isEmpty()){
                // Check followings info, check whether the current user follows them or not
                List<UserFollowing> followings = userCenterDao.getUserFollowings(userId);
                Set<Long> fanIdSet = fans.stream()
                        .map(UserFollowing :: getUserId).collect(Collectors.toSet());
                List<UserInfo> userInfoList = userService.getUserInfoByUserIds(fanIdSet);
                fans.forEach(fan -> userInfoList.forEach(userInfo ->{
                    if(fan.getUserId().equals(userInfo.getUserId())){
                        userInfo.setFollowed(followings.stream()
                                .anyMatch(item -> item.getFollowingId()
                                        .equals(fan.getUserId())));
                        fan.setUserInfo(userInfo);
                    }
                }));
            }
        }
        return new PageResult<>(total, fans);
    }

    public List<FollowingGroup> getUserCenterFollowingGroups(Long userId) {
        List<FollowingGroup> defaultGroups = userCenterDao.getUserFollowingGroups(userId);
        List<FollowingGroup> list = userCenterDao.countUserCenterFollowingGroups(userId);
        defaultGroups.forEach(defaultGroup ->{
            defaultGroup.setCount(0);
            if(list.stream()
                    .map(FollowingGroup::getId)
                    .noneMatch(item -> item.equals(defaultGroup.getId()))){
                list.add(defaultGroup);
            }
        });
        list.sort(Comparator.comparingLong(FollowingGroup :: getId));
        return list;
    }

    public Long countUserFans(Long userId) {
        return userCenterDao.countUserFans(userId);
    }
}
