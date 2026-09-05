# E2E 冒烟测试

## 自动化（CI可跑）

```bash
# 后端启动后执行（默认 http://127.0.0.1:9000）
node e2e/api-smoke.mjs
```

覆盖：验证码接口、错误验证码拒绝、健康自检可达、Open API 无 Key 拒绝。

## 人工辅助链路（验证码/TOTP 需读图）

涉及真实登录的链路验证码是图形验证码，需人工读图后传入。步骤：

1. `curl http://127.0.0.1:9000/api/auth/captcha` → 把 `data.image` 的 base64 存为 png 看图读码，记下 `captchaId` 与码；
2. 带码登录拿响应头 `Authorization`；
3. 依次验证各功能链路（历史脚本参考）：
   - 在线用户/强退：`POST /api/online/getOnlineUsers` → `POST /api/online/kickOut {tokenJti}`
   - 2FA：`POST /api/auth/2fa/setup` → 用相同 TOTP 算法算码 → `/2fa/enable` → 登录应返回 code=1001 → 带码登录 200
   - 健康自检：`GET /api/health/detail`
   - 监控：`GET /api/monitor/overview`
   - Open API：管理端建 Key → 带 `X-Api-Key` 调 `/api/open/v1/users`
   - 回收站：删用户 → `GET /api/recycle/getLists` 应包含 → restore 后可登录
