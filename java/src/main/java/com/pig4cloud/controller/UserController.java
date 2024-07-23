package com.pig4cloud.controller;

import com.pig4cloud.dao.Response;
import com.pig4cloud.dto.UserDto;
import com.pig4cloud.entity.UserEntity;
import com.pig4cloud.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
@Controller
@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/getUserList")
    public Response getUserList(@RequestBody UserDto userDto){
        return userService.getUserLists(userDto);
    }

    @GetMapping("/getUser")
    public Response getUser(@RequestBody UserEntity userEntity) {
        return userService.getUser(userEntity.getUsername());
    }

    @PostMapping("/createUser")
    public Response createUser(@RequestBody UserEntity userEntity) {
        return userService.createUser(userEntity);
    }

    @PostMapping("/updateUser")
    public Response updateUser(@RequestBody UserEntity userEntity) {
        return userService.updateUser(userEntity);
    }

    @PostMapping("/updateUser1")
    public Response updateUser(@RequestBody Map<String, Object> map) {
        return userService.updateUser1(map);
    }

    @PostMapping("/delUser")
    public Response delUser(@RequestBody UserEntity userEntity) {
        return userService.deleteUser(userEntity);
    }

    @PostMapping("/updateAvatar")
    public Response updateAvatar(@RequestBody Map<String, Object> map) throws IOException {
        return userService.updateAvatar(map);
    }
}
