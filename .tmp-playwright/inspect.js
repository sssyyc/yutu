const { chromium } = require('playwright-core');
(async () => {
  const browser = await chromium.launch({
    headless: true,
    executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
  });
  const page = await browser.newPage();
  const logs = [];
  const errors = [];
  page.on('console', msg => logs.push(`[${msg.type()}] ${msg.text()}`));
  page.on('pageerror', err => errors.push(String(err && err.stack || err)));
  await page.goto('http://127.0.0.1:5174/login', { waitUntil: 'networkidle' });
  await page.fill('input[placeholder="请输入用户名"]', 'merchant01').catch(() => {});
  await page.fill('input[placeholder="请输入密码"]', '123456').catch(() => {});
  const btn = page.locator('button:has-text("登录系统")');
  if (await btn.count()) {
    await btn.click();
    await page.waitForLoadState('networkidle').catch(() => {});
  }
  await page.goto('http://127.0.0.1:5174/merchant', { waitUntil: 'networkidle' }).catch(() => {});
  const bodyText = await page.locator('body').innerText().catch(() => '');
  const screenshotPath = 'D:/bishe/project/.tmp-playwright/merchant-page.png';
  await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {});
  console.log('URL=' + page.url());
  console.log('BODY_START');
  console.log(bodyText.slice(0, 2000));
  console.log('BODY_END');
  console.log('LOGS_START');
  console.log(logs.join('\n'));
  console.log('LOGS_END');
  console.log('ERRORS_START');
  console.log(errors.join('\n'));
  console.log('ERRORS_END');
  await browser.close();
})();
