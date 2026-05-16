package com.quant.platform.business.system;

import com.quant.platform.common.api.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SystemHealthController {
    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.application.name:quant-ai}")
    private String appName;

    public SystemHealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public Result<Map<String, Object>> health(@RequestParam(name = "checkDb", defaultValue = "false") boolean checkDb) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("app", appName);
        data.put("time", LocalDateTime.now());
        data.put("status", "UP");

        if (checkDb) {
            Integer one = jdbcTemplate.queryForObject("select 1", Integer.class);
            data.put("db", one != null && one == 1 ? "UP" : "DOWN");
        }

        return Result.ok(data);
    }

    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.ok("pong");
    }
}
