package com.shopping;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.shopping.mapper")
@EnableScheduling
public class ShoppingApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShoppingApplication.class, args);
    }

    @Bean
    @ConditionalOnProperty(name = "demo.password-reset.enabled", havingValue = "true")
    ApplicationRunner demoPasswordFixer(JdbcTemplate jdbc) {
        return args -> {
            String oldHash = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi6WxPvq6WqZQ3Jk8hH2I1QWvVV7Y8K";
            String newHash = new BCryptPasswordEncoder().encode("123456");
            jdbc.update(
                    "UPDATE tb_user SET password = ? WHERE password = ? AND phone IN ('13800000001','13800000002')",
                    newHash,
                    oldHash
            );
            jdbc.update(
                    "UPDATE tb_merchant SET password = ? WHERE password = ? AND phone IN ('13700000001','13700000002')",
                    newHash,
                    oldHash
            );
        };
    }
}
