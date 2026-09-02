package com.pig4cloud.gen.service;

import com.pig4cloud.common.exception.BizException;
import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码生成器：读information_schema表元数据，渲染本项目风格的前后端CRUD代码
 * （Entity/Mapper/Service/ServiceImpl/Controller + 前端api.js/Index.vue + 菜单SQL），支持预览与zip下载
 */
@Service
@RequiredArgsConstructor
public class GenService {

    private final JdbcTemplate jdbcTemplate;

    @Getter
    @Setter
    public static class GenQueryDto {
        private String table;
        /**
         * 业务包名（如dept/notice），决定后端包路径与前端api路径
         */
        private String module;
        /**
         * 类名（如DeptLog），不传按表名驼峰推导
         */
        private String className;
        private String author = "gen";
    }

    /**
     * 可生成代码的表（排除系统表/引擎表）
     */
    public List<Map<String, Object>> tables() {
        return jdbcTemplate.queryForList(
                "SELECT table_name, table_comment FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE' " +
                "AND table_name NOT LIKE 'act_%' AND table_name NOT LIKE 'sys_schema%' " +
                "ORDER BY table_name");
    }

    public Map<String, String> preview(GenQueryDto dto) {
        TableMeta meta = loadMeta(dto);
        Map<String, String> files = new LinkedHashMap<>();
        String cn = meta.className;
        String lc = cn.substring(0, 1).toLowerCase() + cn.substring(1);
        files.put("backend/" + cn + ".java", render(ENTITY, meta));
        files.put("backend/" + cn + "Mapper.java", render(MAPPER, meta));
        files.put("backend/" + cn + "Service.java", render(SERVICE, meta));
        files.put("backend/" + cn + "ServiceImpl.java", render(SERVICE_IMPL, meta));
        files.put("backend/" + cn + "Controller.java", render(CONTROLLER, meta));
        files.put("frontend/api/" + lc + ".js", render(API_JS, meta));
        files.put("frontend/views/" + lc + "-manager/Index.vue", render(VUE_PAGE, meta));
        files.put("menu.sql", render(MENU_SQL, meta));
        return files;
    }

    private TableMeta loadMeta(GenQueryDto dto) {
        if (dto.getTable() == null || dto.getTable().isBlank()) {
            throw new BizException("表名不能为空");
        }
        if (dto.getTable().contains("'") || !dto.getTable().matches("[a-zA-Z0-9_]+")) {
            throw new BizException("表名不合法");
        }
        List<Map<String, Object>> tables = tables();
        String comment = tables.stream()
                .filter(t -> dto.getTable().equals(t.get("table_name")))
                .map(t -> String.valueOf(t.getOrDefault("table_comment", "")))
                .findFirst().orElseThrow(() -> new BizException("表不存在：" + dto.getTable()));
        TableMeta meta = new TableMeta();
        meta.table = dto.getTable();
        meta.tableComment = comment;
        meta.module = dto.getModule() == null || dto.getModule().isBlank()
                ? dto.getTable().replaceFirst("^sys_", "") : dto.getModule();
        meta.className = dto.getClassName() == null || dto.getClassName().isBlank()
                ? camel(dto.getTable().replaceFirst("^sys_", "")) : dto.getClassName();
        meta.author = dto.getAuthor();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT column_name, data_type, column_comment, column_key FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ? ORDER BY ordinal_position", dto.getTable());
        for (Map<String, Object> row : rows) {
            Column column = new Column();
            column.name = String.valueOf(row.get("column_name"));
            column.javaName = camel(column.name);
            column.javaName = column.javaName.substring(0, 1).toLowerCase() + column.javaName.substring(1);
            column.type = javaType(String.valueOf(row.get("data_type")));
            column.comment = String.valueOf(row.getOrDefault("column_comment", ""));
            column.pk = "PRI".equals(row.get("column_key"));
            meta.columns.add(column);
        }
        if (meta.columns.isEmpty()) {
            throw new BizException("表不存在或无字段：" + dto.getTable());
        }
        return meta;
    }

    private String javaType(String dataType) {
        return switch (dataType) {
            case "tinyint", "smallint", "int", "integer" -> "Integer";
            case "bigint" -> "Long";
            case "datetime", "timestamp", "date" -> "Date";
            case "decimal", "numeric", "double", "float" -> "java.math.BigDecimal";
            default -> "String";
        };
    }

    private String camel(String underscore) {
        StringBuilder sb = new StringBuilder();
        for (String part : underscore.split("_")) {
            if (!part.isEmpty()) {
                sb.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
            }
        }
        return sb.toString();
    }

    private String render(String template, TableMeta meta) {
        String cn = meta.className;
        String lc = cn.substring(0, 1).toLowerCase() + cn.substring(1);
        StringBuilder fields = new StringBuilder();
        StringBuilder vueColumns = new StringBuilder();
        StringBuilder vueFormItems = new StringBuilder();
        for (Column column : meta.columns) {
            String label = column.comment.isEmpty() ? column.javaName : column.comment;
            if (column.pk) {
                fields.append("    /**\n     * ").append(column.comment).append("\n     */\n")
                        .append("    @TableId(type = IdType.AUTO)\n")
                        .append("    private ").append(column.type).append(" ").append(column.javaName).append(";\n\n");
            } else {
                fields.append("    /**\n     * ").append(column.comment).append("\n     */\n")
                        .append("    private ").append(column.type).append(" ").append(column.javaName).append(";\n\n");
            }
            vueColumns.append("            <el-table-column prop=\"").append(column.javaName)
                    .append("\" label=\"").append(label).append("\" align=\"center\" />\n");
            if (!column.pk) {
                vueFormItems.append("            <el-form-item label=\"").append(label)
                        .append("\"><el-input v-model=\"dialog.form.").append(column.javaName).append("\" /></el-form-item>\n");
            }
        }
        return template
                .replace("{PACKAGE}", "com.pig4cloud." + meta.module)
                .replace("{CLASS}", cn)
                .replace("{LC}", lc)
                .replace("{TABLE}", meta.table)
                .replace("{COMMENT}", meta.tableComment == null || meta.tableComment.isEmpty() ? cn : meta.tableComment)
                .replace("{AUTHOR}", meta.author)
                .replace("{FIELDS}", fields.toString())
                .replace("{VUE_COLUMNS}", vueColumns.toString())
                .replace("{VUE_FORM_ITEMS}", vueFormItems.toString());
    }

    private static class TableMeta {
        String table;
        String tableComment;
        String module;
        String className;
        String author;
        List<Column> columns = new ArrayList<>();
    }

    private static class Column {
        String name;
        String javaName;
        String type;
        String comment;
        boolean pk;
    }

    // ========== 模板 ==========
    private static final String ENTITY = """
            package {PACKAGE}.entity;

            import com.baomidou.mybatisplus.annotation.IdType;
            import com.baomidou.mybatisplus.annotation.TableId;
            import com.baomidou.mybatisplus.annotation.TableName;
            import lombok.Data;

            /**
             * {COMMENT}（代码生成）
             *
             * @author {AUTHOR}
             */
            @Data
            @TableName("{TABLE}")
            public class {CLASS} {

            {FIELDS}}
            """;

    private static final String MAPPER = """
            package {PACKAGE}.mapper;

            import com.baomidou.mybatisplus.core.mapper.BaseMapper;
            import {PACKAGE}.entity.{CLASS};
            import org.apache.ibatis.annotations.Mapper;
            import org.springframework.stereotype.Repository;

            /**
             * {COMMENT} Mapper（代码生成）
             *
             * @author {AUTHOR}
             */
            @Repository
            @Mapper
            public interface {CLASS}Mapper extends BaseMapper<{CLASS}> {
            }
            """;

    private static final String SERVICE = """
            package {PACKAGE}.service;

            import com.pig4cloud.common.result.PageResult;
            import com.pig4cloud.common.result.R;
            import {PACKAGE}.entity.{CLASS};

            import java.util.Map;

            /**
             * {COMMENT} Service（代码生成）
             *
             * @author {AUTHOR}
             */
            public interface {CLASS}Service {

                R<PageResult<Map<String, Object>>> getLists(long page, long pageSize);

                R<Void> create({CLASS} entity);

                R<Void> update({CLASS} entity);

                R<Void> delete(Integer id);
            }
            """;

    private static final String SERVICE_IMPL = """
            package {PACKAGE}.service;

            import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
            import com.pig4cloud.common.exception.BizException;
            import com.pig4cloud.common.result.PageResult;
            import com.pig4cloud.common.result.R;
            import {PACKAGE}.entity.{CLASS};
            import {PACKAGE}.mapper.{CLASS}Mapper;
            import lombok.RequiredArgsConstructor;
            import org.springframework.stereotype.Service;

            import java.util.Date;
            import java.util.Map;

            /**
             * {COMMENT} Service实现（代码生成）
             *
             * @author {AUTHOR}
             */
            @Service
            @RequiredArgsConstructor
            public class {CLASS}ServiceImpl implements {CLASS}Service {

                private final {CLASS}Mapper mapper;

                @Override
                public R<PageResult<Map<String, Object>>> getLists(long page, long pageSize) {
                    Page<{CLASS}> result = mapper.selectPage(new Page<>(page, pageSize),
                            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<{CLASS}>().orderByDesc("id"));
                    return R.ok("获取数据成功", PageResult.of(result.getRecords(), result.getTotal(), pageSize, page));
                }

                @Override
                public R<Void> create({CLASS} entity) {
                    entity.setId(null);
                    mapper.insert(entity);
                    return R.ok("创建成功", null);
                }

                @Override
                public R<Void> update({CLASS} entity) {
                    if (mapper.selectById(entity.getId()) == null) {
                        throw new BizException("数据不存在");
                    }
                    mapper.updateById(entity);
                    return R.ok("更新成功", null);
                }

                @Override
                public R<Void> delete(Integer id) {
                    mapper.deleteById(id);
                    return R.ok("删除成功", null);
                }
            }
            """;

    private static final String CONTROLLER = """
            package {PACKAGE}.controller;

            import com.pig4cloud.common.result.PageResult;
            import com.pig4cloud.common.result.R;
            import {PACKAGE}.entity.{CLASS};
            import {PACKAGE}.service.{CLASS}Service;
            import lombok.RequiredArgsConstructor;
            import org.springframework.security.access.prepost.PreAuthorize;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestBody;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RequestParam;
            import org.springframework.web.bind.annotation.RestController;

            /**
             * {COMMENT}管理（代码生成）——请按需补@PreAuthorize权限点与菜单
             *
             * @author {AUTHOR}
             */
            @RestController
            @RequestMapping("/api/{LC}")
            @RequiredArgsConstructor
            public class {CLASS}Controller {

                private final {CLASS}Service service;

                @PostMapping("/getLists")
                public R<PageResult<{CLASS}>> getLists(@RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "10") long pageSize) {
                    return service.getLists(page, pageSize);
                }

                @PostMapping("/create")
                public R<Void> create(@RequestBody {CLASS} entity) {
                    return service.create(entity);
                }

                @PostMapping("/update")
                public R<Void> update(@RequestBody {CLASS} entity) {
                    return service.update(entity);
                }

                @PostMapping("/del")
                public R<Void> del(@RequestBody {CLASS} entity) {
                    return service.delete(entity.getId());
                }
            }
            """;

    private static final String API_JS = """
            import service from '@/utils/request.js'

            /**
             * {COMMENT}分页列表（代码生成）
             */
            export function get{CLASS}Lists(data) {
                return service({ url: '/{LC}/getLists', method: 'post', data })
            }

            export function create{CLASS}(data) {
                return service({ url: '/{LC}/create', method: 'post', data })
            }

            export function update{CLASS}(data) {
                return service({ url: '/{LC}/update', method: 'post', data })
            }

            export function del{CLASS}(data) {
                return service({ url: '/{LC}/del', method: 'post', data })
            }
            """;

    private static final String VUE_PAGE = """
            <script setup>
            import { reactive, ref } from 'vue'
            import { ElMessage, ElMessageBox } from 'element-plus'
            import { create{CLASS}, del{CLASS}, get{CLASS}Lists, update{CLASS} } from '@/api/{LC}.js'

            const table = reactive({
                query: { page: 1, pageSize: 10 },
                rows: [],
                total: 0,
                height: window.innerHeight - 50 - 30 - 40 - 52 - 52,
            })

            const getList = () => {
                get{CLASS}Lists(table.query).then((res) => {
                    if (res?.code === 200) {
                        table.rows = res.data.rows
                        table.total = res.data.total
                    }
                })
            }
            getList()

            const handleSizeChange = () => {
                table.query.page = 1
                getList()
            }

            const dialog = reactive({
                visible: false,
                type: 'create',
                form: {},
            })
            const formRef = ref()

            const openDialog = (type, row) => {
                dialog.type = type
                dialog.form = type === 'create' ? { } : { ...row }
                dialog.visible = true
            }

            const save = () => {
                const action = dialog.type === 'create' ? create{CLASS} : update{CLASS}
                action(dialog.form).then((res) => {
                    if (res?.code === 200) {
                        ElMessage.success(res.message || '保存成功')
                        dialog.visible = false
                        getList()
                    } else {
                        ElMessage.error(`保存失败！${res?.message}`)
                    }
                })
            }

            const handleDelete = (row) => {
                ElMessageBox.confirm('确定删除该条{COMMENT}吗？', '提示', { type: 'warning' }).then(() => {
                    del{CLASS}({ id: row.id }).then((res) => {
                        if (res?.code === 200) {
                            ElMessage.success(res.message || '删除成功')
                            getList()
                        } else {
                            ElMessage.error(`删除失败！${res?.message}`)
                        }
                    })
                })
            }
            </script>

            <template>
                <div class="page">
                    <div class="flex justify-end mb-10px">
                        <el-button type="primary" @click="openDialog('create')">新增</el-button>
                    </div>
                    <el-table :data="table.rows" border :max-height="table.height">
            {VUE_COLUMNS}            <el-table-column label="操作" width="150" align="center">
                            <template #default="scope">
                                <el-button size="small" type="primary" link @click="openDialog('edit', scope.row)">编辑</el-button>
                                <el-button size="small" type="danger" link @click="handleDelete(scope.row)">删除</el-button>
                            </template>
                        </el-table-column>
                    </el-table>
                    <el-pagination
                        class="mt-10px flex justify-end"
                        v-model:current-page="table.query.page"
                        v-model:page-size="table.query.pageSize"
                        :page-sizes="[10, 20, 30, 40, 50]"
                        :background="true"
                        layout="total, sizes, prev, pager, next, jumper"
                        :total="table.total"
                        @size-change="handleSizeChange"
                        @current-change="getList"
                    />

                    <el-dialog v-model="dialog.visible" :title="dialog.type === 'create' ? '新增' : '编辑'" width="560">
                        <el-form ref="formRef" :model="dialog.form" label-width="100px">
            {VUE_FORM_ITEMS}        </el-form>
                        <template #footer>
                            <el-button @click="dialog.visible = false">取消</el-button>
                            <el-button type="primary" @click="save">保存</el-button>
                        </template>
                    </el-dialog>
                </div>
            </template>

            <style scoped>
            .page {
                padding: 20px;
            }
            </style>
            """;

    private static final String MENU_SQL = """
            -- {COMMENT}菜单SQL（代码生成）：请将父级id与菜单id按实际调整后执行
            -- 假设挂载在系统管理(2)下，菜单id取 29901、按钮 2990101/2990102
            INSERT INTO `sys_menu` VALUES (29901, 2, '{COMMENT}', '/{LC}-manager', '1', '1', '2', '@/views/{LC}-manager/Index.vue', '{LC}-manager', NULL);
            INSERT INTO `sys_menu` VALUES (2990101, 29901, '{COMMENT}编辑', NULL, '1', '2', '3', NULL, NULL, '{LC}:write');
            INSERT INTO `sys_menu` VALUES (2990102, 29901, '{COMMENT}删除', NULL, '1', '2', '3', NULL, NULL, '{LC}:remove');
            -- 给角色绑定新菜单（示例：root=102）
            INSERT INTO `role_menu` (`menu_id`, `role_id`) VALUES (29901, 102);
            INSERT INTO `role_menu` (`menu_id`, `role_id`) VALUES (2990101, 102);
            INSERT INTO `role_menu` (`menu_id`, `role_id`) VALUES (2990102, 102);
            -- 后端接口权限点：在生成的Controller上按需加 @PreAuthorize("hasAuthority('{LC}:write')") 等
            """;
}
