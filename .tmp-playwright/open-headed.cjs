const { chromium } = require('playwright-core');
(async () => {
  const context = await chromium.launchPersistentContext('D:/bishe/project/.edge-clean-user', {
    headless: false,
    executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
    args: ['--window-size=1400,900']
  });
  const page = context.pages()[0] || await context.newPage();
  await page.goto('http://127.0.0.1:5174/login', { waitUntil: 'domcontentloaded' });
  console.log('opened');
})();
