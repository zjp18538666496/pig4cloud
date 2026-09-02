package com.pig4cloud.recycle.controller;

import com.pig4cloud.common.result.R;
import com.pig4cloud.recycle.service.RecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 回收站（recycle:manage权限）：软删除的用户/角色可恢复或彻底删除
 */
@RestController
@RequestMapping("/api/recycle")
@RequiredArgsConstructor
public class RecycleController {

    private final RecycleService recycleService;

    @GetMapping("/getLists")
    @PreAuthorize("hasAuthority('recycle:manage')")
    public R<List<Map<String, Object>>> getLists() {
        return recycleService.getLists();
    }

    @PostMapping("/restore")
    @PreAuthorize("hasAuthority('recycle:manage')")
    public R<Void> restore(@RequestBody RecycleService.RecycleItemDto dto) {
        return recycleService.restore(dto);
    }

    @PostMapping("/purge")
    @PreAuthorize("hasAuthority('recycle:manage')")
    public R<Void> purge(@RequestBody RecycleService.RecycleItemDto dto) {
        return recycleService.purge(dto);
    }
}
