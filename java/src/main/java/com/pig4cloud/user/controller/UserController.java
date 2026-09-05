package com.pig4cloud.user.controller;

import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.log.annotation.LogRecord;
import com.pig4cloud.common.annotation.Reauth;
import com.pig4cloud.common.result.R;
import com.pig4cloud.user.dto.PasswordUpdateDto;
import com.pig4cloud.user.dto.ResetPasswordDto;
import com.pig4cloud.user.dto.UserCreateDto;
import com.pig4cloud.user.dto.UserDeleteDto;
import com.pig4cloud.user.dto.UserDto;
import com.pig4cloud.user.dto.UserUpdateDto;
import com.pig4cloud.user.service.UserService;
import com.pig4cloud.user.vo.UserVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/getUserList")
    public R<PageResult<Map<String, Object>>> getUserList(@RequestBody UserDto userDto) {
        return userService.getUserLists(userDto);
    }

    @GetMapping("/getUser")
    public R<UserVO> getUser(@RequestParam String username) {
        return userService.getUser(username);
    }

    @PostMapping("/register")
    @LogRecord(module = "认证", operation = "用户注册")
    public R<Void> register(@Valid @RequestBody UserCreateDto dto) {
        // 注册与用户管理分开：注册无需登录（SecurityConfig放行），用户管理接口走权限点
        return userService.createUser(dto);
    }

    @PostMapping("/createUser")
    @LogRecord(module = "用户管理", operation = "新增用户")
    @PreAuthorize("hasAuthority('user:write')")
    public R<Void> createUser(@Valid @RequestBody UserCreateDto dto) {
        return userService.createUser(dto);
    }

    @PostMapping("/updateUser")
    @LogRecord(module = "用户管理", operation = "编辑用户")
    public R<Void> updateUser(@Valid @RequestBody UserUpdateDto dto) {
        // 接口只要求登录；管理员(user:write)或本人的判断在Service层
        return userService.updateUser(dto);
    }

    @PostMapping("/delUser")
    @LogRecord(module = "用户管理", operation = "删除用户")
    @PreAuthorize("hasAuthority('user:remove')")
    public R<Void> delUser(@Valid @RequestBody UserDeleteDto dto) {
        return userService.deleteUser(dto);
    }

    @PostMapping("/updateAvatar")
    @LogRecord(module = "用户管理", operation = "更新头像")
    public R<Void> updateAvatar(@RequestParam("id") Long id, @RequestParam("avatar") MultipartFile avatar) throws Exception {
        return userService.updateAvatar(id, avatar);
    }

    @PostMapping("/updatePassword")
    @LogRecord(module = "用户管理", operation = "修改密码")
    public R<Void> updatePassword(@Valid @RequestBody PasswordUpdateDto dto) {
        return userService.updatePassword(dto);
    }

    @PostMapping("/resetPassword")
    @Reauth
    @LogRecord(module = "用户管理", operation = "重置密码")
    @PreAuthorize("hasAuthority('user:write')")
    public R<Void> resetPassword(@Valid @RequestBody ResetPasswordDto dto) {
        return userService.resetPassword(dto);
    }

    /**
     * 用户导出（当前筛选+数据权限，xlsx）
     */
    @PostMapping("/export")
    @LogRecord(module = "用户管理", operation = "导出用户")
    @PreAuthorize("hasAuthority('user:write')")
    public void export(@RequestBody UserDto userDto, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> rows = userService.exportRows(userDto);
        List<UserExportRow> data = rows.stream().map(row -> {
            UserExportRow export = new UserExportRow();
            export.setUsername((String) row.get("username"));
            export.setName((String) row.get("name"));
            export.setDeptName((String) row.get("dept_name"));
            export.setTenantName((String) row.get("tenant_name"));
            export.setMobile((String) row.get("mobile"));
            export.setEmail((String) row.get("email"));
            export.setRoleNames((String) row.get("role_names"));
            export.setCreateTime((String) row.get("create_time"));
            export.setLastLoginTime((String) row.get("last_login_time"));
            return export;
        }).toList();
        respondExcel(response, "用户列表",
                new String[]{"账号", "昵称", "部门", "租户", "手机号", "邮箱", "角色", "创建时间", "最后登录"},
                data);
    }

    /**
     * 用户导入模板
     */
    @GetMapping("/importTemplate")
    @PreAuthorize("hasAuthority('user:write')")
    public void importTemplate(HttpServletResponse response) throws IOException {
        UserExportRow template = new UserExportRow();
        template.setUsername("demo001");
        template.setName("张三");
        template.setPassword("Abc12345");
        template.setMobile("13800000000");
        template.setEmail("demo@example.com");
        respondExcel(response, "用户导入模板",
                new String[]{"账号", "昵称", "密码", "手机号", "邮箱"},
                List.of(template));
    }

    /**
     * 用户导入（逐行按密码策略/查重校验，返回成功数与失败明细）
     */
    @PostMapping("/import")
    @LogRecord(module = "用户管理", operation = "导入用户")
    @PreAuthorize("hasAuthority('user:write')")
    public R<Map<String, Object>> importUsers(@RequestParam("file") MultipartFile file) throws IOException {
        List<UserImportRow> rows;
        try {
            rows = com.alibaba.excel.EasyExcel.read(file.getInputStream())
                    .head(UserImportRow.class).sheet().doReadSync();
        } catch (Exception ex) {
            throw new com.pig4cloud.common.exception.BizException("Excel解析失败，请使用导入模板填写");
        }
        List<com.pig4cloud.user.dto.UserCreateDto> createRows = rows.stream().map(row -> {
            com.pig4cloud.user.dto.UserCreateDto create = new com.pig4cloud.user.dto.UserCreateDto();
            create.setUsername(row.getUsername());
            create.setName(row.getName());
            create.setPassword(row.getPassword());
            create.setMobile(row.getMobile());
            create.setEmail(row.getEmail());
            return create;
        }).toList();
        return R.ok("请求成功", userService.importUsers(createRows));
    }

    private void respondExcel(HttpServletResponse response, String fileName,
                              String[] headers, List<?> data) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encoded = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=utf-8''" + encoded + ".xlsx");
        // 动态头导出：用Map行+head(List<List<String>>)方式
        com.alibaba.excel.EasyExcel.write(response.getOutputStream())
                .head(java.util.Arrays.stream(headers)
                        .map(h -> java.util.List.of(h))
                        .collect(java.util.stream.Collectors.toList()))
                .sheet("sheet1")
                .doWrite(data.isEmpty() ? java.util.List.of() : data);
    }

    /**
     * 导出行模型（也用作导入模板表头载体；导入另用UserImportRow）
     */
    @lombok.Data
    public static class UserExportRow {
        @com.alibaba.excel.annotation.ExcelProperty("账号")
        private String username;
        @com.alibaba.excel.annotation.ExcelProperty("昵称")
        private String name;
        @com.alibaba.excel.annotation.ExcelProperty("密码")
        private String password;
        @com.alibaba.excel.annotation.ExcelProperty("部门")
        private String deptName;
        @com.alibaba.excel.annotation.ExcelProperty("租户")
        private String tenantName;
        @com.alibaba.excel.annotation.ExcelProperty("手机号")
        private String mobile;
        @com.alibaba.excel.annotation.ExcelProperty("邮箱")
        private String email;
        @com.alibaba.excel.annotation.ExcelProperty("角色")
        private String roleNames;
        @com.alibaba.excel.annotation.ExcelProperty("创建时间")
        private String createTime;
        @com.alibaba.excel.annotation.ExcelProperty("最后登录")
        private String lastLoginTime;
    }

    /**
     * 导入行模型：账号/昵称/密码/手机号/邮箱
     */
    @lombok.Data
    public static class UserImportRow {
        @com.alibaba.excel.annotation.ExcelProperty("账号")
        private String username;
        @com.alibaba.excel.annotation.ExcelProperty("昵称")
        private String name;
        @com.alibaba.excel.annotation.ExcelProperty("密码")
        private String password;
        @com.alibaba.excel.annotation.ExcelProperty("手机号")
        private String mobile;
        @com.alibaba.excel.annotation.ExcelProperty("邮箱")
        private String email;
    }
}
