package com.example.bilibili.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.bilibili.dao.DanmuDao;
import com.example.bilibili.domain.Danmu;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Danmu Service - Handles real-time bullet comments (danmu) with Redis caching
 * Key features: Redis caching for performance, async processing, time-based filtering
 */
@Service
public class DanmuService {

    private static final String DANMU_KEY = "dm-video-"; // Redis key prefix for video danmu

    @Autowired
    private DanmuDao danmuDao;

    @Autowired
    private RedisTemplate<String, String> redisTemplate; // For caching danmu data

    public void addDanmu(Danmu danmu){
        danmuDao.addDanmu(danmu);
    }

    @Async
    public void asyncAddDanmu(Danmu danmu){
        // Asynchronously save danmu to database for better performance
        danmuDao.addDanmu(danmu);
    }

    /**
     * The query strategy prioritizes retrieving barrage data from Redis.
     * If the data is not found in Redis, it queries the database and then writes the retrieved data back into Redis.
     */
    public List<Danmu> getDanmus(Long videoId,
                                 String startTime, String endTime) throws Exception {

        String key = DANMU_KEY + videoId;
        String value = redisTemplate.opsForValue().get(key);
        List<Danmu> list;
        if (!StringUtil.isNullOrEmpty(value)) {
            list = JSONArray.parseArray(value, Danmu.class);
            if (!StringUtil.isNullOrEmpty(startTime)
                    && !StringUtil.isNullOrEmpty(endTime)) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date startDate = sdf.parse(startTime);
                Date endDate = sdf.parse(endTime);
                List<Danmu> childList = new ArrayList<>();
                for (Danmu danmu : list) {
                    Date createTime = danmu.getCreateTime();
                    if (createTime.after(startDate) && createTime.before(endDate)) {
                        childList.add(danmu);
                    }
                }
                list = childList;
            }
        } else {
            Map<String, Object> params = new HashMap<>();
            params.put("videoId", videoId);
            params.put("startTime", startTime);
            params.put("endTime", endTime);
            list = danmuDao.getDanmus(params);
            // Save Danmu to redis
            redisTemplate.opsForValue().set(key, JSONObject.toJSONString(list));
        }
        return list;
    }

    public void addDanmusToRedis(Danmu danmu) {
        // Add new danmu to Redis cache for real-time display
        String key = DANMU_KEY + danmu.getVideoId();
        String value = redisTemplate.opsForValue().get(key);
        List<Danmu> list = new ArrayList<>();
        if (!StringUtil.isNullOrEmpty(value)) {
            list = JSONArray.parseArray(value, Danmu.class);
        }
        list.add(danmu);
        redisTemplate.opsForValue().set(key, JSONObject.toJSONString(list));
    }

}
