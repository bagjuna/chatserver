package com.example.chatserver;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.37")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:alpine"))
        .withExposedPorts(6379); // Redis 기본 포트 개방

    static {
        mysql.start();
        redis.start(); // Redis도 시작!
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);

        // ★ Redis 설정 주입 ★
        // 컨테이너가 랜덤하게 배정한 호스트와 포트를 스프링에 알려줍니다.
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // ★ 에러 원인 해결: redisPassword 빈 값이라도 넣어주기
        // (application.yml에서 ${redisPassword}를 쓰고 있다면 이 키를 맞춰줘야 함)
        registry.add("redisPassword", () -> "");
        registry.add("spring.data.redis.password", () -> ""); // 혹시 몰라 이것도 추가
    }
}
