package com.example.bilibili.service;

import com.example.bilibili.dao.TagDao;
import com.example.bilibili.domain.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TagService {

    @Autowired
    private TagDao tagDao;

    public Long addTag(Tag tag) {
        tagDao.addTag(tag);
        return tag.getId();
    }

}
