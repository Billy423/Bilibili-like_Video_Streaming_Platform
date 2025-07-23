package com.example.bilibili.dao;

import com.example.bilibili.domain.Content;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ContentDao {

    Long addContent(Content content);

}
