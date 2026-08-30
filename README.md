# pig4cloud admin

前后端分离的RBAC权限管理系统：用户/角色/部门/菜单管理、JWT双token认证、动态菜单路由、多租户（套餐/配额）、在线用户管理、通知公告、登录日志与操作日志。

- 后端：Java 17 + Spring Boot 3.3 + Spring Security + MyBatis-Plus + MongoDB（日志）（`java/`）
- 前端：Vue 3 + Vite + Element Plus + Pinia + ECharts（`web/`）
- 数据库：MySQL（`sql/pigx_admin.sql`）

演示账号：root / 12345678

## 功能总览

- **权限管理**：用户/角色/部门/岗位/菜单（含按钮权限点）管理，数据权限（本租户全部/本部门及以下/仅本人）
- **角色层级**：角色可挂上级角色形成角色树，子角色沿父链继承菜单/按钮权限；角色编码与数据权限不继承（防止继承super越权），后端校验同租户/防成环
- **认证安全**：图形验证码（可开关）、登录失败锁定（阈值/时长可配）+ IP级限流、JWT双token、登出/强退token拉黑、邮箱找回密码、密码策略（最小长度/复杂度可配、首登强制改密、密码过期）
- **权限即时生效**：改角色菜单/换套餐/禁用租户/删用户等敏感操作自动踢相关会话
- **在线用户**：实时在线会话列表、强制下线
- **多租户**：共享表+tenant_id自动隔离；租户套餐（决定可用菜单，变更即时生效）、有效期（过期自动禁用任务）、用户数配额、租户删除
- **通知与消息**：通知公告（平台/租户两级，可选扇出站内信）、站内信中心（WebSocket实时推送+未读铃铛）
- **系统管理**：字典管理、参数配置（密码策略/锁定阈值/日志保留等即时生效）、定时任务管理（cron调度/手动执行/执行日志）
- **日志审计**：操作日志与登录日志（MongoDB，按租户隔离+超管全局视图，支持Excel导出、保留期自动清理）
- **Excel导入导出**：用户列表导出/模板下载/批量导入（逐行校验报告）
- **首页仪表盘**：统计卡片+登录趋势+最新公告
- **前端体验**：菜单图标、暗黑模式、多标签页、中英文切换、验证码开关联动

## 状态存储与多实例

验证码/在线会话/登录锁定/token黑名单/IP限流等一次性状态统一走 `StateStore` 抽象：

- `app.store.type=memory`（默认）：单机内存实现，重启清空，零额外依赖
- `app.store.type=redis`：多实例共享，需配置 `spring.data.redis.*`（或环境变量 `REDIS_HOST/REDIS_PORT/REDIS_PASSWORD`）

多实例部署务必切 redis。

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
> 也可改用环境变量：`JWT_SECRET`、`CORS_ORIGINS`、`MAIL_ENABLED`、数据库账号密码等。

### 找回密码邮件（可选）

默认关闭。开启方式：在 `application-local.yaml` 中配置 `spring.mail.*`（模板见 `application-local.yaml.example`），并设置环境变量 `MAIL_ENABLED=true`。未配置时"忘记密码"接口会提示联系管理员。

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
├── auth          # 登录/刷新token/登出/JWT/验证码/失败锁定/找回密码（/api/auth/**）
├── online        # 在线用户管理：会话列表/强制下线（/api/online/**）
├── user          # 用户管理（/api/user/**）
├── role          # 角色管理（含数据权限data_scope）（/api/role/**）
├── dept          # 部门管理+数据权限计算（/api/dept/**）
├── menu          # 菜单管理（/api/menu/**）
├── tenant        # 租户管理与租户套餐（/api/tenant/**）
├── notice        # 通知公告（/api/notice/**）
├── stats         # 首页仪表盘统计（/api/stats/**）
├── log           # 操作日志与登录日志（MongoDB存储）（/api/log/**）
└── file          # 本地与FTP文件上传下载（/api/file/**）
```

约定：

- 所有接口统一挂 `/api` 前缀；响应体为 `{code, message, data}`，`code=200` 成功、`-200` 业务失败、`401` 触发前端刷新token。
- 认证：access token（1小时）+ refresh token（30天），通过响应头 `Authorization` / `Refresh-Token` 下发；动态路由由路由守卫按需注册（首次导航/刷新/重新登录后自动重建）。
- 权限点=角色编码（如`root`）+ 按钮菜单的权限标识（`sys_menu.perms`，如`user:remove`），登录时随用户信息下发：后端接口用 `@PreAuthorize("hasAuthority('user:remove')")` 控制，前端按钮用 `v-permission="['user:remove']"` 控制；注册接口 `/api/user/register` 无需登录。
- 前端动态路由只接受 `sys_menu.component_path` 指向 `/views` 下真实存在的组件（白名单）。
- 在线会话/验证码/失败锁定/找回密码验证码均为**内存实现**（单机有效、重启清空），多实例部署需替换为Redis等共享存储（见`auth/online`与`auth/service`下的Store类边界）。

## 多租户

共享表方案（`tenant_id` 列 + MyBatis-Plus 租户拦截器自动拼条件）：

- 租户模型：**账号全库唯一，登录不填租户**，账号归属哪个租户由 `sys_user.tenant_id` 决定；菜单为平台级共享，用户/角色/部门按租户隔离
- 平台超级管理员：账号 `admin/12345678`（`tenant_id=0`，角色 `super`），可跨租户管理，专属"平台管理"菜单
- 开通租户：超管在"平台管理 → 租户管理"开通，**选择租户套餐**（决定租户管理员可用菜单），可设置有效期与用户数上限；自动创建 `{租户ID}admin` 管理员账号（初始密码 `12345678`）+ 租户管理员角色并绑定套餐内菜单
- 租户被禁用或过期后其下账号无法登录；用户数达到配额后无法继续新增用户
- 通知公告：超管发布平台公告（全员可见），租户管理员发布本租户公告

## 数据库升级

- 全新环境无需手动导入：首次启动自动建库建表灌数据。
- **老库升级**：本版本新增部门/公告/套餐表与若干列。后端启动时自动检测（`sys_dept`表不存在）并执行 `java/src/main/resources/sql/upgrade_20260830.sql`，也可手动 source。存量租户的 tenant_admin 角色如需新菜单，请在角色管理里重新勾选保存。

## 接口文档

后端启动后访问 http://localhost:9000/swagger-ui/index.html

## 生产部署

### 1. 后端打包（Spring Boot → 可执行jar）

```bash
cd java
mvn -DskipTests package
# 产物：target/pig4cloud-1.0-SNAPSHOT.jar（需JRE 17）
```

直接运行：

```bash
JWT_SECRET=$(openssl rand -base64 32) \
STORE_TYPE=redis REDIS_HOST=10.126.126.3 REDIS_PASSWORD=123456 \
java -jar target/pig4cloud-1.0-SNAPSHOT.jar
```

**环境变量一览**（均有默认值，按需注入）：

| 变量 | 默认 | 说明 |
|---|---|---|
| `JWT_SECRET` | 无（必填） | JWT签名密钥，Base64编码≥32字节 |
| `CORS_ORIGINS` | `*` | 允许的跨域来源，逗号分隔；前后端同域部署可不配 |
| `STORE_TYPE` | `memory` | 状态存储：`memory`单机 / `redis`多实例 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | `127.0.0.1` / `6379` / 空 | Redis连接（STORE_TYPE=redis时使用） |
| `DB_INIT` | `true` | 首次启动自动建库建表+演示数据；生产建议`false` |
| `MAIL_ENABLED` | `false` | 找回密码邮件开关（需配置spring.mail.*） |

数据库（MySQL）与MongoDB连接放在`application-local.yaml`（参考application-local.yaml.example），也可用`SPRING_DATASOURCE_URL`等标准环境变量覆盖。

### 2. 前端打包（Vue3 → 纯静态dist）

```bash
cd web
npm run build
# 产物：web/dist/（相对路径base，可挂在任意静态目录）
```

前端生产环境请求走相对路径`/api`、WebSocket走`/ws`，**必须由Nginx反向代理到后端**。

### 3. Nginx配置（前端静态 + 接口/WebSocket代理）

完整示例见`docker/nginx.conf.example`，核心三段：

```nginx
server {
    listen 80;
    client_max_body_size 1024m;           # 文件上传不限大小

    location / {                           # 前端静态页
        root /opt/pigx-admin/dist;
        try_files $uri $uri/ /index.html;  # history路由刷新必备，不能省
    }

    location /api/ {                       # 后端接口
        proxy_pass http://127.0.0.1:9000;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /ws/ {                        # 站内信WebSocket
        proxy_pass http://127.0.0.1:9000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 300s;
    }
}
```

### 4. Docker部署

```bash
cd java && mvn -DskipTests package
cd docker/Compose
JWT_SECRET=$(openssl rand -base64 32) docker compose up -d --build
```

Dockerfile基于eclipse-temurin:17-jre，按Spring Boot分层jar构建（依赖层缓存友好）。Compose已透传全部环境变量（见docker/Compose/docker-compose.yml），在同级建`.env`文件填写`JWT_SECRET`、`STORE_TYPE`、`REDIS_HOST`等即可。

### 5. 部署注意事项

- **Nginx的`try_files ... /index.html`不能省**：前端是history路由，缺失会导致刷新业务页面404
- **多实例部署**：`STORE_TYPE=redis`必配，且任务调度为单机内存版，多实例时任务会重复执行（需自行保证幂等或只开一个实例跑任务）
- **数据库自动初始化**：全新库首启自动建表+演示数据；老库自动执行增量升级（sys_schema_version登记）
- **文件存储依赖FTP**：头像/上传走FTP（application-local.yaml的ftp.*），生产需保证FTP可达
