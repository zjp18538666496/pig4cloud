package com.pig4cloud.post.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.common.context.UserContext;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.log.annotation.LogRecord;
import com.pig4cloud.post.entity.SysPostEntity;
import com.pig4cloud.post.mapper.SysPostMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 岗位管理：租户内CRUD（sys_post带tenant_id列走租户拦截器自动隔离）
 */
@Service
public class PostService {

    private final SysPostMapper postMapper;

    public PostService(SysPostMapper postMapper) {
        this.postMapper = postMapper;
    }

    public R<PageResult<SysPostEntity>> getPostLists(long page, long pageSize, String postName) {
        Page<SysPostEntity> result = postMapper.selectPage(new Page<>(page, pageSize),
                new QueryWrapper<SysPostEntity>()
                        .like(StringUtils.hasText(postName), "post_name", postName)
                        .orderByAsc("sort").orderByAsc("id"));
        return R.ok("获取数据成功", PageResult.of(result.getRecords(), result.getTotal(), pageSize, page));
    }

    /**
     * 启用中的岗位（用户编辑弹窗下拉）
     */
    public R<List<SysPostEntity>> getEnabledPosts() {
        return R.ok("获取数据成功", postMapper.selectList(
                new QueryWrapper<SysPostEntity>().eq("status", "1").orderByAsc("sort")));
    }

    public R<Void> createPost(SysPostEntity post) {
        post.setId(null);
        // 租户归属由服务端决定：取当前登录用户租户，平台操作归平台层
        post.setTenant_id(UserContext.getTenantId() == null ? 0 : UserContext.getTenantId());
        post.setCreate_time(new Date());
        postMapper.insert(post);
        return R.ok("创建成功", null);
    }

    public R<Void> updatePost(SysPostEntity post) {
        if (postMapper.selectById(post.getId()) == null) {
            throw new BizException("岗位不存在");
        }
        post.setTenant_id(null); // 租户归属不可改
        post.setUpdate_time(new Date());
        postMapper.updateById(post);
        return R.ok("更新成功", null);
    }

    public R<Void> deletePost(Integer id) {
        postMapper.deleteById(id);
        return R.ok("删除成功", null);
    }
}
