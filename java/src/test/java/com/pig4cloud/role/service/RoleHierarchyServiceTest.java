package com.pig4cloud.role.service;

import com.pig4cloud.role.entity.RoleEntity;
import com.pig4cloud.role.mapper.RoleMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 角色层级单测：祖先链展开与子孙收集
 */
public class RoleHierarchyServiceTest {

    private RoleMapper roleMapper;
    private RoleHierarchyService service;

    private RoleEntity role(Integer id, Integer parentId) {
        RoleEntity entity = new RoleEntity();
        entity.setId(id);
        entity.setParent_id(parentId);
        return entity;
    }

    @BeforeEach
    public void setUp() {
        roleMapper = Mockito.mock(RoleMapper.class);
        service = new RoleHierarchyService(roleMapper);
        // 三级链：100 <- 101 <- 102，另有独立角色 200
        List<RoleEntity> all = List.of(role(100, 0), role(101, 100), role(102, 101), role(200, 0));
        when(roleMapper.selectList(any())).thenReturn(all);
    }

    @Test
    public void testEffectiveRoleIdsWalksUpChain() {
        List<Integer> effective = service.effectiveRoleIds(List.of(role(102, 101)));
        // 自身+全部祖先
        Assertions.assertEquals(List.of(102, 101, 100), effective);
    }

    @Test
    public void testEffectiveRoleIdsEmpty() {
        Assertions.assertTrue(service.effectiveRoleIds(List.of()).isEmpty());
    }

    @Test
    public void testDescendantIds() {
        Set<Integer> descendants = service.descendantIds(100);
        Assertions.assertEquals(Set.of(101, 102), descendants);
    }

    @Test
    public void testCycleDataDoesNotLoopForever() {
        // 脏数据成环：110 -> 111 -> 110
        when(roleMapper.selectList(any()))
                .thenReturn(List.of(role(110, 111), role(111, 110)));
        List<Integer> effective = service.effectiveRoleIds(List.of(role(110, 111)));
        Assertions.assertEquals(Set.of(110, 111), new java.util.HashSet<>(effective));
    }
}
