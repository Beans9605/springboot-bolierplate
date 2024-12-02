package org.example.springbootboilerplate.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "custom.keycloak")
public class KeycloakProperties {
    private String url;
    private String publicKey;
    private Client client;

    @Data
    public static class Client {
        private String id;
        private String secret;
    }
}
