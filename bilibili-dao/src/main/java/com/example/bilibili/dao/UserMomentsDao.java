package com.example.bilibili.dao;

import com.example.bilibili.domain.UserMoment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMomentsDao {

    Integer addUserMoments(UserMoment userMoment);

    Integer pageCountMoments(Map<String, Object> params);

    List<UserMoment> pageListMoments(Map<String, Object> params);
}
