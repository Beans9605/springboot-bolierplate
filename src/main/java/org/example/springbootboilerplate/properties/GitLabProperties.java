package org.example.springbootboilerplate.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "api.gitlab")
public class GitLabProperties {

    private String url;
    private String token;

}
