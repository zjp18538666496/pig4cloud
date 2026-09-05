package com.pig4cloud.common.util;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.ip2region.core.Ip2regionSearcher;
import org.springframework.stereotype.Component;

/**
 * 登录IP归属地解析：基于ip2region离线库（mica-ip2region，jar内自带xdb数据，不依赖外网）。
 * 解析失败或私有/内网地址不抛异常，统一降级展示，不影响登录主流程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpRegionService {

    private final Ip2regionSearcher searcher;

    @PostConstruct
    public void init() {
        log.info("IP归属地解析已就绪(ip2region离线库)");
    }

    /**
     * 解析IP归属地：如"广东省深圳市 电信"；内网/本机返回"内网IP"，解析失败返回"未知"
     */
    public String resolve(String ip) {
        if (ip == null || ip.isBlank()) {
            return "未知";
        }
        if (isInternalIp(ip)) {
            return "内网IP";
        }
        try {
            String address = searcher.getAddressAndIsp(ip);
            return address == null || address.isBlank() ? "未知" : address;
        } catch (Exception ex) {
            log.debug("IP归属地解析失败[{}]: {}", ip, ex.getMessage());
            return "未知";
        }
    }

    /**
     * 私有/环回/链路本地地址不走离线库（xdb中无内网网段，解析出来是未知）
     */
    private boolean isInternalIp(String ip) {
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip) || "localhost".equals(ip)) {
            return true;
        }
        if (ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("172.16.")
                || ip.startsWith("172.17.") || ip.startsWith("172.18.") || ip.startsWith("172.19.")
                || ip.startsWith("172.2") || ip.startsWith("172.30.") || ip.startsWith("172.31.")
                || ip.startsWith("fe80:") || ip.startsWith("fc") || ip.startsWith("fd")) {
            return true;
        }
        return false;
    }
}
