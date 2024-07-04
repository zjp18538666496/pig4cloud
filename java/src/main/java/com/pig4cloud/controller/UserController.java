package com.pig4cloud.controller;

import com.pig4cloud.dao.Response;
import com.pig4cloud.dto.UserDto;
import com.pig4cloud.entity.UserEntity;
import com.pig4cloud.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/getUserList")
    public Response getListUser(@RequestBody UserDto userDto){
        return userService.getUserLists(userDto);
    }

    @GetMapping("/getUser")
    public Response getUser(@RequestBody UserEntity userEntity) {
        return userService.getUser(userEntity.getUsername());
    }

    @PostMapping("/createUser")
    public Response createUser(@RequestBody UserEntity userEntity, @RequestParam Map<String, Object> map) {
        return userService.createUser(userEntity);
    }

    @PostMapping("/updateUser")
    public Response updateUser(@RequestBody UserEntity userEntity, @RequestParam Map<String, Object> map) {
        return userService.updateUser(userEntity);
    }

    @PostMapping("/delUser")
    public Response delUser(@RequestBody UserEntity userEntity, @RequestParam Map<String, Object> map) {
        return userService.deleteUser(userEntity);
    }
}
