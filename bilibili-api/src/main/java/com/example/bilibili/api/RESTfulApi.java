package com.example.bilibili.api;

import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
public class RESTfulApi {

    private final Map<Integer, Map<String, Object>> dataMap;

    public RESTfulApi() {
        dataMap = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", i);
            data.put("name", "name" + i);
            dataMap.put(i, data);
        }
    }

    @GetMapping("/objects/{id}")
    public Map<String, Object> getData(@PathVariable Integer id) { // Maps integer id into ("/objects/{id}")
        return dataMap.get(id);
    }

    @DeleteMapping("/objects/{id}")
    public String deleteData(@PathVariable Integer id) {
        dataMap.remove(id);
        return "Delete success";
    }

    @PostMapping("/objects")
    public String postData(@RequestBody Map<String, Object> data) {
        Integer[] idArray = dataMap.keySet().toArray(new Integer[0]);
        Arrays.sort(idArray);
        int nextId = idArray[idArray.length - 1] + 1;
        dataMap.put(nextId, data);
        return "Post success";
    }

    @PutMapping("/objects")
    public String putData(@RequestBody Map<String, Object> data) {
        Integer id = Integer.valueOf(String.valueOf(data.get("id")));
        Map<String, Object> containedData = dataMap.get(id);
        if (containedData == null) {
            Integer[] idArray = dataMap.keySet().toArray(new Integer[0]);
            Arrays.sort(idArray);
            int nextId = idArray[idArray.length - 1] + 1;
            dataMap.put(nextId, data);
        } else {
            dataMap.put(id, data);
        }
        return "Put success";
    }
}
