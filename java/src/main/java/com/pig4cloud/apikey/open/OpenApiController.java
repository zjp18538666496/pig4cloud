package com.pig4cloud.apikey.open;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.notice.entity.NoticeEntity;
import com.pig4cloud.notice.mapper.NoticeMapper;
import com.pig4cloud.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 开放接口示例（/api/open/**，X-Api-Key鉴权+Scope授权+限流）。
 * 返回字段刻意收窄，不含密码等敏感信息；新增开放接口时用OpenApiContext.requireScope断言
 */
@RestController
@RequestMapping("/api/open/v1")
@RequiredArgsConstructor
public class OpenApiController {

    private final UserMapper userMapper;
    private final NoticeMapper noticeMapper;

    /**
     * 用户列表（scope: user:read）
     */
    @GetMapping("/users")
    public R<PageResult<Map<String, Object>>> users(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long pageSize,
                                                    HttpServletRequest request) {
        OpenApiContext.requireScope(request, "user:read");
        List<Map<String, Object>> rows = userMapper.selectPage(pageSize, page - 1, null, null, "", null, null)
                .stream()
                .map(row -> Map.of(
                        "id", row.get("id"),
                        "username", row.get("username"),
                        "name", row.get("name") == null ? "" : row.get("name"),
                        "deptName", row.get("dept_name") == null ? "" : row.get("dept_name"),
                        "createTime", row.get("create_time") == null ? "" : row.get("create_time")))
                .toList();
        long total = userMapper.selectUserList2Count(null, null, "", null, null);
        return R.ok("请求成功", PageResult.of(rows, total, pageSize, page));
    }

    /**
     * 已发布公告（scope: notice:read）
     */
    @GetMapping("/notices")
    public R<List<Map<String, Object>>> notices(@RequestParam(defaultValue = "10") long limit,
                                                HttpServletRequest request) {
        OpenApiContext.requireScope(request, "notice:read");
        List<Map<String, Object>> rows = noticeMapper.selectList(new QueryWrapper<NoticeEntity>()
                        .eq("status", "1").orderByDesc("id").last("LIMIT " + Math.min(limit, 100)))
                .stream()
                .map(notice -> Map.<String, Object>of(
                        "id", notice.getId(),
                        "title", notice.getTitle(),
                        "content", notice.getContent() == null ? "" : notice.getContent(),
                        "tenantId", notice.getTenant_id(),
                        "createTime", notice.getCreate_time() == null ? "" : notice.getCreate_time().toString()))
                .toList();
        return R.ok("请求成功", rows);
    }
}
