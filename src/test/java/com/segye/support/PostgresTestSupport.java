package com.segye.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 모든 통합 테스트의 베이스. compose.yaml 과 같은 PostgreSQL 이미지를 띄워
 * H2 가 아닌 실제 운영 DB 엔진에서 검증한다.
 *
 * 컨테이너는 JVM 당 하나만 띄우는 싱글턴이다. @Testcontainers + @Container 를 쓰면
 * 하위 클래스마다 컨테이너가 재기동되면서 JDBC URL 이 바뀌고, 그때마다 스프링 컨텍스트
 * 캐시가 깨져 테스트가 크게 느려진다. 정리는 Testcontainers 의 Ryuk 컨테이너가 맡는다.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class PostgresTestSupport {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"))
                    .withDatabaseName("segye")
                    .withUsername("ssafy")
                    .withPassword("ssafy");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
