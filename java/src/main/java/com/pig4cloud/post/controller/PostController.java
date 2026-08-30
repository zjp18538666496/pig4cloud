package com.pig4cloud.post.controller;

import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.log.annotation.LogRecord;
import com.pig4cloud.post.entity.SysPostEntity;
import com.pig4cloud.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping("/getPostLists")
    @PreAuthorize("hasAuthority('post:write')")
    public R<PageResult<SysPostEntity>> getPostLists(@RequestBody Map<String, Object> body) {
        long page = body.get("page") == null ? 1 : ((Number) body.get("page")).longValue();
        long pageSize = body.get("pageSize") == null ? 10 : ((Number) body.get("pageSize")).longValue();
        String postName = (String) body.get("post_name");
        return postService.getPostLists(page, pageSize, postName);
    }

    /**
     * 启用中的岗位下拉（用户编辑弹窗，登录即可）
     */
    @PostMapping("/getEnabledPosts")
    public R<List<SysPostEntity>> getEnabledPosts() {
        return postService.getEnabledPosts();
    }

    @PostMapping("/createPost")
    @LogRecord(module = "岗位管理", operation = "新增岗位")
    @PreAuthorize("hasAuthority('post:write')")
    public R<Void> createPost(@RequestBody SysPostEntity post) {
        return postService.createPost(post);
    }

    @PostMapping("/updatePost")
    @LogRecord(module = "岗位管理", operation = "编辑岗位")
    @PreAuthorize("hasAuthority('post:write')")
    public R<Void> updatePost(@RequestBody SysPostEntity post) {
        return postService.updatePost(post);
    }

    @PostMapping("/delPost")
    @LogRecord(module = "岗位管理", operation = "删除岗位")
    @PreAuthorize("hasAuthority('post:remove')")
    public R<Void> delPost(@RequestBody SysPostEntity post) {
        return postService.deletePost(post.getId());
    }
}
