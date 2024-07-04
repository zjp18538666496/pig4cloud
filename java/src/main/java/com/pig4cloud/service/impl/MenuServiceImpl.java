package com.pig4cloud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.dao.MenuMapper;
import com.pig4cloud.dao.Response;
import com.pig4cloud.dao.impl.ResponseImpl;
import com.pig4cloud.dto.MenuDto;
import com.pig4cloud.entity.MenuEntity;
import com.pig4cloud.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Component
public class MenuServiceImpl implements MenuService {
    @Autowired
    private MenuMapper menuMapper;

    @Override
    public Response createMenu(MenuEntity menuEntity) {
        int rows = menuMapper.insert(menuEntity);
        return new ResponseImpl(200, rows > 0 ? "创建成功" : "创建失败", null);
    }

    @Override
    public Response updateMenu(MenuEntity menuEntity) {
        UpdateWrapper<MenuEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", menuEntity.getId());
        int rows = menuMapper.update(menuEntity, updateWrapper);
        return new ResponseImpl(rows > 0 ? 200 : -200, rows > 0 ? "更新成功" : "更新失败", null);
    }

    @Override
    public Response deleteMenu(MenuEntity menuEntity) {
        QueryWrapper<MenuEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", menuEntity.getId());
        int rows = menuMapper.delete(queryWrapper);
        return new ResponseImpl(rows > 0 ? 200 : -200, rows > 0 ? "删除成功" : "删除失败", null);
    }

    @Override
    public Response getMenuLists(MenuDto menuDto) {
        long page = menuDto.getPage();
        long pageSize = menuDto.getPageSize();
        Page<MenuEntity> rowPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<MenuEntity> queryWrapper = new LambdaQueryWrapper<>();
        rowPage = menuMapper.selectPage(rowPage, queryWrapper);
        Response response = new ResponseImpl(200, "获取数据成功", null);
        response.pagination(rowPage);
        return response;
    }
}
