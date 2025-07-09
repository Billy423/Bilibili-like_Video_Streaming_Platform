package com.example.bilibili.api;

import com.example.bilibili.api.support.UserSupport;
import com.example.bilibili.domain.Danmu;
import com.example.bilibili.domain.JsonResponse;
import com.example.bilibili.service.DanmuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DanmuApi {

    @Autowired
    private DanmuService danmuService;

    @Autowired
    private UserSupport userSupport;

    @GetMapping("/danmus")
    public JsonResponse<List<Danmu>> getDanmus(@RequestParam Long videoId,
                                               String startTime,
                                               String endTime) throws Exception {
        List<Danmu> list;
        try {
            // Check if user is in visitor mode or login mode
            userSupport.getCurrentUserId();
            // If in login mode, allow user to filter by time
            list = danmuService.getDanmus(videoId, startTime, endTime);
        } catch (Exception ignored) {
            // If in visitor mode, prevent user from filter by time
            list = danmuService.getDanmus(videoId, null, null);
        }
        return new JsonResponse<>(list);
    }

}