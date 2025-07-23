package com.example.bilibili.api;

import com.example.bilibili.api.support.UserSupport;
import com.example.bilibili.domain.JsonResponse;
import com.example.bilibili.domain.PageResult;
import com.example.bilibili.domain.UserMoment;
import com.example.bilibili.domain.annotation.ApiLimitedRole;
import com.example.bilibili.domain.annotation.DataLimited;
import com.example.bilibili.domain.constant.AuthRoleConstant;
import com.example.bilibili.service.UserMomentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserMomentsApi {

    @Autowired
    private UserMomentsService userMomentsService;

    @Autowired
    private UserSupport userSupport;

    @GetMapping("/user-subscribed-moments")
    public JsonResponse<List<UserMoment>> getUserSubscribedMoments(){
        Long userId = userSupport.getCurrentUserId();
        List<UserMoment> list = userMomentsService.getUserSubscribedMoments(userId);
        return new JsonResponse<>(list);
    }

    @ApiLimitedRole(limitedRoleCodeList = {AuthRoleConstant.ROLE_LV0})
    @DataLimited
    @PostMapping("/user-moments")
    public JsonResponse<String> addUserMoments(@RequestBody UserMoment userMoment) throws Exception {
        Long userId = userSupport.getCurrentUserId();
        userMoment.setUserId(userId);
        userMomentsService.addUserMoments(userMoment);
        return JsonResponse.success();
    }

    @GetMapping("/moments")
    public JsonResponse<PageResult<UserMoment>> pageListMoments(@RequestParam("size") Integer size,
                                                                @RequestParam("no") Integer no,
                                                                String type){
        Long userId = userSupport.getCurrentUserId();
        PageResult<UserMoment> list = userMomentsService.pageListMoments(size, no,
                userId, type);
        return new JsonResponse<>(list);
    }

}
