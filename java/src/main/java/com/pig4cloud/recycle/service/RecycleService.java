package com.pig4cloud.recycle.service;

import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.R;
import com.pig4cloud.log.annotation.LogRecord;
import com.pig4cloud.recycle.mapper.RecycleMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 回收站：软删除数据的查看/恢复/彻底删除（recycle:manage权限，租户拦截器自动隔离）
 */
@Service
@RequiredArgsConstructor
public class RecycleService {

    private final RecycleMapper recycleMapper;

    @Getter
    @Setter
    public static class RecycleItemDto {
        private Integer id;

        /**
         * user / role
         */
        private String type;
    }

    public R<List<Map<String, Object>>> getLists() {
        List<Map<String, Object>> items = recycleMapper.deletedUsers();
        items.addAll(recycleMapper.deletedRoles());
        return R.ok("获取数据成功", items);
    }

    @LogRecord(module = "回收站", operation = "恢复数据")
    public R<Void> restore(RecycleItemDto dto) {
        int rows = switch (dto.getType() == null ? "" : dto.getType()) {
            case "user" -> recycleMapper.restoreUser(dto.getId());
            case "role" -> recycleMapper.restoreRole(dto.getId());
            default -> throw new BizException("类型不支持");
        };
        if (rows <= 0) {
            throw new BizException("数据不存在或已恢复");
        }
        return R.ok("恢复成功", null);
    }

    /**
     * 彻底删除（不可恢复；用户删除级联清角色/岗位关联）
     */
    @LogRecord(module = "回收站", operation = "彻底删除")
    public R<Void> purge(RecycleItemDto dto) {
        int rows = switch (dto.getType() == null ? "" : dto.getType()) {
            case "user" -> recycleMapper.purgeUser(dto.getId());
            case "role" -> recycleMapper.purgeRole(dto.getId());
            default -> throw new BizException("类型不支持");
        };
        if (rows <= 0) {
            throw new BizException("数据不存在");
        }
        return R.ok("已彻底删除", null);
    }
}
