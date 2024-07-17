package com.pig4cloud.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
class FTPServiceImplTest {
    @Test
    void downloadFile() throws IOException {
        FTPServiceImpl ftpService = new FTPServiceImpl();
        InputStream file = ftpService.downloadFile("/test/2024-07-17 20-48-17.mkv");
    }
}