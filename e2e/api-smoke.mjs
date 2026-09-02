// ============================================================
// E2E冒烟脚本（无依赖，node e2e/api-smoke.mjs [baseUrl]）
// 自动化校验无需人工输入验证码的链路：
//   1. 验证码接口可用   2. 错误验证码被拒   3. 无Key调开放接口被拒
// 完整登录后的业务链路验证见 e2e/README.md（验证码/TOTP需人工读图）
// ============================================================
const baseUrl = process.argv[2] || 'http://127.0.0.1:9000';
let failed = 0;

const check = (name, ok, detail) => {
    console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`);
    if (!ok) failed++;
};

// 1. 验证码接口
const captcha = await fetch(`${baseUrl}/api/auth/captcha`).then(r => r.json());
check('验证码接口', captcha.code === 200 && captcha.data?.captchaId && captcha.data?.image?.startsWith('data:image/png'));

// 2. 错误验证码被拒（登录防护生效）
const badLogin = await fetch(`${baseUrl}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'root', password: '12345678', captchaId: 'e2e', captchaCode: 'E2EE' }),
}).then(r => r.json());
check('错误验证码被拒', badLogin.code === -200, badLogin.message);

// 3. 健康自检（登录外的依赖探测需token，此处仅确认接口不500）
const health = await fetch(`${baseUrl}/api/health/detail`).then(r => r.json());
check('健康自检接口可达', health.code === 200 || health.code === 401, `code=${health.code}`);

// 4. Open API无Key被拒
const open = await fetch(`${baseUrl}/api/open/v1/users`).then(r => r.json());
check('Open API无Key被拒', open.code === 401, open.message);

console.log(failed === 0 ? '\n全部通过' : `\n${failed}项失败`);
process.exit(failed === 0 ? 0 : 1);
