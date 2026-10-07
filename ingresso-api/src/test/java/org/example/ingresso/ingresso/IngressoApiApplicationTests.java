package org.example.ingresso.ingresso;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ingresso-test;DB_CLOSE_DELAY=-1",
    "token.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
    "spring.h2.console.enabled=true"
})
class IngressoApiApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
    }

    @Test
    void h2ConsoleServletIsRegisteredWhenEnabled() {
        boolean registered = applicationContext.getBeansOfType(ServletRegistrationBean.class).values().stream()
            .anyMatch(registration -> registration.getUrlMappings().contains("/h2-console/*"));
        assertTrue(registered);
    }

}
