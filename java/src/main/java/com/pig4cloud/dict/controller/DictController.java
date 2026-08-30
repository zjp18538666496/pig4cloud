package com.pig4cloud.dict.controller;

import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.dict.entity.SysDictEntity;
import com.pig4cloud.dict.entity.SysDictItemEntity;
import com.pig4cloud.dict.service.DictService;
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
@RequestMapping("/api/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    @PostMapping("/getDictLists")
    @PreAuthorize("hasAuthority('dict:write')")
    public R<PageResult<SysDictEntity>> getDictLists(@RequestBody DictService.DictQueryDto dto) {
        return dictService.getDictLists(dto);
    }

    @PostMapping("/createDict")
    @PreAuthorize("hasAuthority('dict:write')")
    public R<Void> createDict(@RequestBody DictService.DictDto dto) {
        return dictService.createDict(dto);
    }

    @PostMapping("/updateDict")
    @PreAuthorize("hasAuthority('dict:write')")
    public R<Void> updateDict(@RequestBody DictService.DictDto dto) {
        return dictService.updateDict(dto);
    }

    @PostMapping("/delDict")
    @PreAuthorize("hasAuthority('dict:remove')")
    public R<Void> delDict(@RequestBody DictService.DictDto dto) {
        return dictService.deleteDict(dto.getId());
    }

    /**
     * 字典项列表（登录即可，表单下拉用）
     */
    @PostMapping("/getItemLists")
    public R<List<SysDictItemEntity>> getItemLists(@RequestBody Map<String, Integer> body) {
        return dictService.getItemLists(body.get("dict_id"));
    }

    /**
     * 按字典编码取启用项（业务下拉数据源，登录即可）
     */
    @GetMapping("/getDictByCode")
    public R<List<SysDictItemEntity>> getDictByCode(@RequestParam String code) {
        return dictService.getDictByCode(code);
    }

    /**
     * 全量保存字典项
     */
    @PostMapping("/saveItems")
    @PreAuthorize("hasAuthority('dict:write')")
    public R<Void> saveItems(@RequestBody Map<String, Object> body) {
        Integer dictId = (Integer) body.get("dict_id");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> raw = (List<Map<String, Object>>) body.get("items");
        List<DictService.DictItemDto> items = raw == null ? List.of() : raw.stream().map(m -> {
            DictService.DictItemDto item = new DictService.DictItemDto();
            item.setLabel((String) m.get("label"));
            item.setValue((String) m.get("value"));
            item.setSort(m.get("sort") == null ? 0 : ((Number) m.get("sort")).intValue());
            item.setStatus((String) m.get("status"));
            return item;
        }).toList();
        return dictService.saveItems(dictId, items);
    }
}
