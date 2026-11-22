package com.segye.auth;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public class JwtProperties {
    private String secret;
    private long accessTokenExpMinutes;

    @Min(1)
    private long refreshTokenExpDays = 30;
}
