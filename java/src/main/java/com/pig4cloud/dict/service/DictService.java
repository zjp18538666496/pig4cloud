package com.pig4cloud.dict.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.common.dto.BasePageQuery;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.dict.entity.SysDictEntity;
import com.pig4cloud.dict.entity.SysDictItemEntity;
import com.pig4cloud.dict.mapper.SysDictItemMapper;
import com.pig4cloud.dict.mapper.SysDictMapper;
import com.pig4cloud.log.annotation.LogRecord;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 字典管理：字典与字典项CRUD；业务下拉通过getDictByCode按编码取启用项
 */
@Service
public class DictService {

    private final SysDictMapper dictMapper;
    private final SysDictItemMapper dictItemMapper;

    public DictService(SysDictMapper dictMapper, SysDictItemMapper dictItemMapper) {
        this.dictMapper = dictMapper;
        this.dictItemMapper = dictItemMapper;
    }

    @Getter
    @Setter
    public static class DictQueryDto extends BasePageQuery {
        private String dict_name = "";
    }

    @Getter
    @Setter
    public static class DictDto {
        private Integer id;
        private String dict_code;
        private String dict_name;
        private String status = "1";
        private String remark;
    }

    @Getter
    @Setter
    public static class DictItemDto {
        private Integer dict_id;
        private String label;
        private String value;
        private Integer sort = 0;
        private String status = "1";
    }

    public R<PageResult<SysDictEntity>> getDictLists(DictQueryDto dto) {
        long page = dto.getPage();
        long pageSize = dto.getPageSize();
        Page<SysDictEntity> result = dictMapper.selectPage(new Page<>(page, pageSize),
                new QueryWrapper<SysDictEntity>()
                        .like(StringUtils.hasText(dto.getDict_name()), "dict_name", dto.getDict_name())
                        .orderByDesc("id"));
        return R.ok("获取数据成功", PageResult.of(result.getRecords(), result.getTotal(), pageSize, page));
    }

    @LogRecord(module = "字典管理", operation = "新增字典")
    public R<Void> createDict(DictDto dto) {
        if (!StringUtils.hasText(dto.getDict_code())) {
            throw new BizException("字典编码不能为空");
        }
        if (dictMapper.selectCount(new QueryWrapper<SysDictEntity>().eq("dict_code", dto.getDict_code())) > 0) {
            throw new BizException("字典编码已存在");
        }
        SysDictEntity dict = new SysDictEntity();
        dict.setDict_code(dto.getDict_code());
        dict.setDict_name(dto.getDict_name());
        dict.setStatus(dto.getStatus());
        dict.setRemark(dto.getRemark());
        dict.setCreate_time(new Date());
        dictMapper.insert(dict);
        return R.ok("创建成功", null);
    }

    @LogRecord(module = "字典管理", operation = "编辑字典")
    public R<Void> updateDict(DictDto dto) {
        SysDictEntity exists = dictMapper.selectById(dto.getId());
        if (exists == null) {
            throw new BizException("字典不存在");
        }
        // 编码创建后不可改（业务按code引用）
        SysDictEntity update = new SysDictEntity();
        update.setId(dto.getId());
        update.setDict_name(dto.getDict_name());
        update.setStatus(dto.getStatus());
        update.setRemark(dto.getRemark());
        update.setUpdate_time(new Date());
        dictMapper.updateById(update);
        return R.ok("更新成功", null);
    }

    @LogRecord(module = "字典管理", operation = "删除字典")
    @Transactional
    public R<Void> deleteDict(Integer id) {
        dictItemMapper.delete(new QueryWrapper<SysDictItemEntity>().eq("dict_id", id));
        dictMapper.deleteById(id);
        return R.ok("删除成功", null);
    }

    public R<List<SysDictItemEntity>> getItemLists(Integer dictId) {
        return R.ok("获取数据成功", dictItemMapper.selectList(
                new QueryWrapper<SysDictItemEntity>().eq("dict_id", dictId).orderByAsc("sort")));
    }

    /**
     * 全量保存字典项（前端整组提交，先删后插）
     */
    @LogRecord(module = "字典管理", operation = "编辑字典项")
    @Transactional
    public R<Void> saveItems(Integer dictId, List<DictItemDto> items) {
        if (dictMapper.selectById(dictId) == null) {
            throw new BizException("字典不存在");
        }
        dictItemMapper.delete(new QueryWrapper<SysDictItemEntity>().eq("dict_id", dictId));
        if (items != null) {
            int sort = 0;
            for (DictItemDto item : items) {
                if (!StringUtils.hasText(item.getLabel()) || !StringUtils.hasText(item.getValue())) {
                    continue;
                }
                SysDictItemEntity entity = new SysDictItemEntity();
                entity.setDict_id(dictId);
                entity.setLabel(item.getLabel());
                entity.setValue(item.getValue());
                entity.setSort(item.getSort() == null ? sort : item.getSort());
                entity.setStatus(item.getStatus() == null ? "1" : item.getStatus());
                dictItemMapper.insert(entity);
                sort++;
            }
        }
        return R.ok("保存成功", null);
    }

    /**
     * 业务按编码取启用中的字典项（按sort排序），下拉数据源
     */
    public R<List<SysDictItemEntity>> getDictByCode(String code) {
        SysDictEntity dict = dictMapper.selectOne(new QueryWrapper<SysDictEntity>().eq("dict_code", code));
        if (dict == null || !"1".equals(dict.getStatus())) {
            return R.ok("获取数据成功", List.of());
        }
        return R.ok("获取数据成功", dictItemMapper.selectList(
                new QueryWrapper<SysDictItemEntity>()
                        .eq("dict_id", dict.getId())
                        .eq("status", "1")
                        .orderByAsc("sort")));
    }
}
