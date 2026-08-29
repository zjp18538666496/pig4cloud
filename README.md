# pig4cloud admin

前后端分离的RBAC权限管理系统：用户/角色/菜单管理、JWT双token认证、动态菜单路由、FTP文件上传。

- 后端：Java 17 + Spring Boot 3.3 + Spring Security + MyBatis-Plus（`java/`）
- 前端：Vue 3 + Vite + Element Plus + Pinia（`web/`）
- 数据库：MySQL（`sql/pigx_admin.sql`）

演示账号：root / 12345678

## 快速启动

### 1. 数据库

首次启动会**自动创建数据库、建表并灌入演示数据**（JDBC URL 带 `createDatabaseIfNotExist=true`，表不存在时自动执行 `sql/pigx_admin_init.sql`），无需手动导入。

手动导入方式（可选）：

```sql
source sql/pigx_admin.sql
```

### 2. 后端（端口9000）

```bash
cd java
# 复制本地敏感配置模板（数据库/FTP/JWT密钥，已被gitignore），按实际环境修改
cp src/main/resources/application-local.yaml.example src/main/resources/application-local.yaml
mvn spring-boot:run
```

> JWT密钥必须至少32字节，可用 `openssl rand -base64 32` 生成。
> 也可改用环境变量：`JWT_SECRET`、`CORS_ORIGINS`、数据库账号密码等。

### 3. 前端

```bash
cd web
npm install
npm run dev:development   # /api 代理到 http://127.0.0.1:9000
```

## 后端结构（按业务分包）

```
com.pig4cloud
├── common        # 统一响应R<T>/PageResult、全局异常、公共配置、分页查询基类
├── auth          # 登录/刷新token/JWT/权限过滤（/api/auth/**）
├── user          # 用户管理（/api/user/**）
├── role          # 角色管理（/api/role/**）
├── menu          # 菜单管理（/api/menu/**）
└── file          # 本地与FTP文件上传下载（/api/file/**）
```

约定：

- 所有接口统一挂 `/api` 前缀；响应体为 `{code, message, data}`，`code=200` 成功、`-200` 业务失败、`401` 触发前端刷新token。
- 认证：access token（1小时）+ refresh token（30天），通过响应头 `Authorization` / `Refresh-Token` 下发；动态路由由路由守卫按需注册（首次导航/刷新/重新登录后自动重建）。
- 权限点=角色编码（如`root`）+ 按钮菜单的权限标识（`sys_menu.perms`，如`user:remove`），登录时随用户信息下发：后端接口用 `@PreAuthorize("hasAuthority('user:remove')")` 控制，前端按钮用 `v-permission="['user:remove']"` 控制；注册接口 `/api/user/register` 无需登录。
- 前端动态路由只接受 `sys_menu.component_path` 指向 `/views` 下真实存在的组件（白名单）。

## 接口文档

后端启动后访问 http://localhost:9000/swagger-ui/index.html

## 部署

```bash
cd java
mvn -DskipTests package
cd docker/Compose
JWT_SECRET=xxx docker compose up -d --build
```

Dockerfile基于eclipse-temurin:17-jre，按Spring Boot分层jar构建（依赖层缓存友好）。

## Docker环境变量

| 变量 | 说明 |
|---|---|
| `JWT_SECRET` | JWT签名密钥（必填，Base64编码≥32字节） |
| `CORS_ORIGINS` | 允许的跨域来源，逗号分隔，默认`*` |
