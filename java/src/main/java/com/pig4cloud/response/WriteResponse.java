package com.pig4cloud.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.dao.Response;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 响应处理类
 */
public class WriteResponse {
    public WriteResponse(HttpServletResponse response, Response res) throws IOException {
        response.setCharacterEncoding("utf-8");
        response.setContentType("application/json; charset=utf-8");
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(res);
        response.getWriter().write(json);
    }
}
