package com.quant.platform;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.quant.platform")
@MapperScan(basePackages = "com.quant.platform", markerInterface = BaseMapper.class)
@EnableScheduling
public class QuantPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuantPlatformApplication.class, args);
    }
}
