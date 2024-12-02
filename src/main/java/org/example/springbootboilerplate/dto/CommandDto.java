package org.example.springbootboilerplate.dto;

import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.SystemUtils;
import org.example.springbootboilerplate.properties.K8SProperties;
import org.example.springbootboilerplate.util.Util;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CommandDto {

    private final K8SProperties k8SProperties;
    private static K8SProperties innerK8sProperties;

    @PostConstruct
    public void init() {
        innerK8sProperties = k8SProperties;
    }

    private static String[] withKubeConfig(Boolean isVcluster) {
        return Objects.nonNull(
                innerK8sProperties.getKubeConfigPath())
                ? new String[]{isVcluster ? "--kube-config" : "--kubeconfig", innerK8sProperties.getKubeConfigPath()}
                : new String[]{};
    }

    /**
     *  window 환경 시에 Object JSON 을 String으로 변환 시에 CMD에서 동작하지 않는 경우가 존재
     *  그에 따른 ms-dos cmd에 맞게 문법 수정해주는 메소드
     *  CustomObjectMapper 에 공통으로 넣지 않은 이유는 타 프로젝트에서 readValue, writeValueAsString 를 시행할때
     *  이 메소드의 변환된 값이 올바르지 않은 경우가 존재하기에 CommandDto에 선언
     * @param json
     * @return
     */
    private static String JSONValidator(String json) {
        if (SystemUtils.OS_NAME.toLowerCase().contains("window")) {
            json = json.replaceAll("\"", "\\\\\"");
        }
        return json;
    }

    private static final String[] installBase = {"helm", "install"};
    private static final String[] connectBase = {"vcluster", "connect"};
    private static final String[] patchBase = {"kubectl", "patch", "-p"};

    @Getter
    @Builder
    public static class InstallCommand {
        private String spaceId;
        private String vclusterNamespace;
        private String chartPath;

        public String[] toCMD() {
            String[] preCommand = {
                    spaceId,
                    "--create-namespace",
                    "-n",
                    vclusterNamespace,
                    "--repo",
                    "https://charts.loft.sh",
                    "vcluster",
                    "--version",
                    "0.15.5",
                    "-f",
                    chartPath
            };
            return Util.concatAllArray(installBase, withKubeConfig(false), preCommand);
        }
    }

    @Builder
    @Getter
    public static class DeleteStorageClass {
        private String spaceId;
        private String type;

        public String[] toCMD() {

            String[] preCommand = {
                    spaceId,
                    "--",
                    "kubectl",
                    "delete",
                    "sc",
                    ObjectUtils.isEmpty(type) ? innerK8sProperties.getBlockStorage().getName() : type
            };

            return Util.concatAllArray(connectBase, withKubeConfig(true), preCommand);
        }
    }

    @Builder
    @Getter
    public static class ResourceQuotaPatch {
        private String resourceQuota;
        private String spaceId;

        public String[] toCMD() {
            String[] preCommand = {
                    JSONValidator(resourceQuota),
                    "resourcequota",
                    spaceId + "-quota",
                    "--namespace",
                    "vcluster-" + spaceId
            };

            return Util.concatAllArray(patchBase, preCommand, withKubeConfig(false));
        }
    }
}