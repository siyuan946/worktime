package com.example.worktime.service;

import com.example.worktime.model.Worker;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class WorkerService {
    private final Map<String, Worker> map = new HashMap<>();

    public WorkerService() {
        map.put("1001", new Worker("1001", "张三", "A车间", "一班"));
        map.put("1002", new Worker("1002", "李四", "A车间", "二班"));
        map.put("1003", new Worker("1003", "王五", "B车间", "一班"));
    }

    public Worker getByCode(String code) {
        return map.get(code);
    }
}
