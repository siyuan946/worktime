package com.example.worktime.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ProcessCodeService {
    private final Map<String, String> map = new HashMap<>();

    public ProcessCodeService() {
        map.put("车", "TURN");
        map.put("钻φ4孔", "DRILL4");
        map.put("喷砂", "SAND");
        map.put("喷铝", "ALU");
        map.put("磨铝面", "GRIND_ALU");
        map.put("喷底漆", "PRIMER");
    }

    public String getCode(String processName) {
        if (processName == null) return null;
        return map.get(processName);
    }
}
