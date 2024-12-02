package org.example.springbootboilerplate.service;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.springbootboilerplate.dto.CommandDto;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class CmdService {

    /**
     *
     * 서버를 실행 시킨 환경 내에서 실행시키고 싶은 커맨드를 실행 이후 처리 작업을 도와주는 메소드
     *
     * @param cmd 실행하고 싶은 커맨드
     * @param comment 실행 이후 성공 / 실패 시에 로그로 찍힐 기본 comment
     * @param resultLogMap key, value 로 묶여있는 map 을 기준으로 로그를 만들어줌
     * @param callback process 정상 실행 이후에 실행해야하는 callback 함수
     * @param failCallback process 실패 이후에 실행해야하는 callback 함수
     * @throws IOException process 런타임 자체 실패
     * @throws InterruptedException process 스트림 획득 실패
     */
    private void baseRunning(
            String[] cmd,
            String comment,
            Map<String, String> resultLogMap,
            @Nullable Consumer<Void> callback,
            @Nullable Consumer<String> failCallback)
            throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(cmd);
        InputStream errorInputStream = process.getErrorStream();
        int exitCode = process.waitFor();

        resultLogMap.forEach((key, value) -> {
            log.info("{} {}=[{}] result=[{}]", comment, key, value, exitCode);
        });

        String error = new String(errorInputStream.readAllBytes(), StandardCharsets.UTF_8);
        if (StringUtils.isNotBlank(error)) {
            log.error("{} error = {}", comment, error);
        }

        if ((exitCode == 0 || exitCode == 141) && callback != null) {
            callback.accept(null);
        }
        else if (failCallback != null) {
            failCallback.accept(error);
        }
    }

    /**
     * spaceId 를 기준으로 새로운 물리적인 space 를 생성하는 메소드
     *
     * @param spaceId
     * @param vclusterNamespace
     * @param path
     * @param callback
     * @param failCallback
     * @throws IOException
     * @throws InterruptedException
     */
    public void createSpaceInCloud(
            String spaceId,
            String vclusterNamespace,
            String path,
            @Nullable Consumer<Void> callback,
            @Nullable Consumer<String> failCallback
    ) throws IOException, InterruptedException {

        String[] cmd =
                CommandDto.InstallCommand
                        .builder()
                        .spaceId(spaceId)
                        .vclusterNamespace(vclusterNamespace)
                        .chartPath(path)
                        .build()
                        .toCMD();
        baseRunning(cmd, "created vcluster", Map.of("spaceId", spaceId), callback, failCallback);
    }

    public void deleteStorageClass(
            String spaceId,
            @Nullable String type,
            @Nullable Consumer<Void> callback,
            @Nullable Consumer<String> failCallback
    ) throws IOException, InterruptedException {
        String[] cmd = CommandDto.DeleteStorageClass
                .builder()
                .spaceId(spaceId)
                .type(type)
                .build()
                .toCMD();

        baseRunning(cmd, "Delete vcluster storageClasses", Map.of("spaceId", spaceId), callback, failCallback);
    }

    public void patchResourceQuota(
            String spaceId,
            String resourceQuota,
            @Nullable Consumer<Void> callback,
            @Nullable Consumer<String> failCallback
    ) throws IOException, InterruptedException {
        String[] cmd = CommandDto.ResourceQuotaPatch
                .builder()
                .spaceId(spaceId)
                .resourceQuota(resourceQuota)
                .build()
                .toCMD();

        baseRunning(cmd, "patch vcluster resource quota", Map.of("spaceId", spaceId), callback, failCallback);
    }
}
