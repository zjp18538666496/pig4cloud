package com.pig4cloud.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.dao.Response;
import com.pig4cloud.dao.impl.ResponseImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class MenuControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper(); // 用于JSON处理

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    public void setUp() {
        // 使用WebApplicationContext加载整个Spring上下文
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void testGetMenuLists() throws Exception {
        // 构建请求内容
        String jsonContent = "{}";

        // 发起请求并获取结果
        MvcResult mvcResult = mockMvc.perform(post("/menu/getMenuLists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Response actualResponse = objectMapper.readValue(responseContent, ResponseImpl.class);

        // 打印响应内容
        System.out.println("响应内容: " + responseContent);
    }
}
