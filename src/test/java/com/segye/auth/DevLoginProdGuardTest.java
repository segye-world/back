package com.segye.auth;

import com.segye.support.PostgresTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(value = {"test", "prod"}, inheritProfiles = false)
@TestPropertySource(properties = "app.auth.dev-login-enabled=true")
@DisplayName("dev-login 운영 차단")
class DevLoginProdGuardTest extends PostgresTestSupport {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("prod 프로필에서는 dev-login-enabled=true 여도 dev-login 이 만들어지지 않는다")
    void devLoginIsNeverCreatedInProd() {
        assertThat(context.getBeanNamesForType(DevLoginController.class)).isEmpty();
    }
}
