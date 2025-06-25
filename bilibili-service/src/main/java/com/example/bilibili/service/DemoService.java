package com.example.bilibili.service;

import com.example.bilibili.dao.DemoDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DemoService {

    @Autowired
    private DemoDao demoDao;

    public Map<String, Object> query(Long id) {
        return demoDao.query(id);
    }


}
