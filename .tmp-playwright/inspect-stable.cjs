const { chromium } = require("playwright-core");
(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe' });
  const page = await browser.newPage({ viewport: { width: 1531, height: 1012 } });
  const logs = [];
  page.on('console', msg => logs.push(`console:${msg.type()}:${msg.text()}`));
  page.on('pageerror', err => logs.push(`pageerror:${err.message}`));
  await page.goto('http://127.0.0.1:4174/login', { waitUntil: 'domcontentloaded' });
  await page.locator('input').nth(0).fill('merchant01');
  await page.locator('input').nth(1).fill('123456');
  await page.locator('button:has-text("登录系统")').click();
  await page.waitForTimeout(2000);
  await page.goto('http://127.0.0.1:4174/merchant/customer/list', { waitUntil: 'networkidle' });
  await page.waitForTimeout(1200);
  console.log('URL=' + page.url());
  console.log('TEXT=' + (await page.locator('body').innerText()).slice(0, 1000));
  console.log('LOGS=' + logs.join('\n'));
  await browser.close();
})();
