package com.pig4cloud.user.controller;

import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.log.annotation.LogRecord;
import com.pig4cloud.common.result.R;
import com.pig4cloud.user.dto.PasswordUpdateDto;
import com.pig4cloud.user.dto.ResetPasswordDto;
import com.pig4cloud.user.dto.UserCreateDto;
import com.pig4cloud.user.dto.UserDeleteDto;
import com.pig4cloud.user.dto.UserDto;
import com.pig4cloud.user.dto.UserUpdateDto;
import com.pig4cloud.user.service.UserService;
import com.pig4cloud.user.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    public R<Void> updateAvatar(@RequestParam("id") Long id, @RequestParam("avatar") MultipartFile avatar) throws IOException {
        return userService.updateAvatar(id, avatar);
    }

    @PostMapping("/updatePassword")
    @LogRecord(module = "用户管理", operation = "修改密码")
    public R<Void> updatePassword(@Valid @RequestBody PasswordUpdateDto dto) {
        return userService.updatePassword(dto);
    }

    @PostMapping("/resetPassword")
    @LogRecord(module = "用户管理", operation = "重置密码")
    @PreAuthorize("hasAuthority('user:write')")
    public R<Void> resetPassword(@Valid @RequestBody ResetPasswordDto dto) {
        return userService.resetPassword(dto);
    }
}
