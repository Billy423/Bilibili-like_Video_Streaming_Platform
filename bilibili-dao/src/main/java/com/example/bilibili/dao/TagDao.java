package com.example.bilibili.dao;

import com.example.bilibili.domain.Tag;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TagDao {

    void addTag(Tag tag);

}
