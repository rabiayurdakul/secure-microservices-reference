package com.reference.edgegateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/demo"
})
class EdgeGatewayApplicationTests {

    @Test
    void contextLoads() {
    }

}
