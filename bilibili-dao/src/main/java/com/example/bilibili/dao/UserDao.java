package com.example.bilibili.dao;

import com.example.bilibili.domain.User;
import com.example.bilibili.domain.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserDao {

    User getUserByPhone(String phone);

    Integer addUser(User user);

    Integer addUserInfo(UserInfo userInfo);

    User getUserByPhoneOrEmail(@Param("phone") String phone, @Param("email") String email);

    User getUserById(long id);

    UserInfo getUserInfoByUserId(long id);
}
