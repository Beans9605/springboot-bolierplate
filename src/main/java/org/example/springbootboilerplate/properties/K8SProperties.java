package org.example.springbootboilerplate.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Objects;
import java.util.regex.Matcher;

@Data
@Component
@ConfigurationProperties(prefix = "kubernetes")
public class K8SProperties {
    /**
     * Nullable Field 로 동작, 데이터가 없을 때는 K8S API, helm, vcluster 의 동작이 ${HOME}/.kube/config 기준으로 동작한다.
     */
    private String kubeConfigPath;
    private String fileSystemId;
    private NfsStorageProperty nfsStorage;
    private BlockStorageProperty blockStorage;

    @Data
    public static class NfsStorageProperty {
        private String provisioner;
        private String name;
    }

    @Data
    public static class BlockStorageProperty {
        private String name;
    }

    @PostConstruct
    public void init() {
        if (Objects.nonNull(kubeConfigPath)) {
            /*
              Java 에서는 HOME 경로를 "~" 문자열로 인식하지 않는다. System property 안에 있는 jar 를 실행시킨
              user의 home을 직접적으로 가져와야 인식한다.
              그러나 관념적으로 "~" 문자열을 HOME으로 개발자들이 인식하고 있으므로 첫번째 "~" 문자열을 user.home 으로 치환해준다.
             */
            kubeConfigPath = kubeConfigPath.replaceFirst("^~", Matcher.quoteReplacement(System.getProperty("user.home")));
        }
    }
}
