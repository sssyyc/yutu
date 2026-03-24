const { chromium } = require('playwright-core');
(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe' });
  const page = await browser.newPage({ viewport: { width: 1600, height: 900 } });
  await page.goto('http://127.0.0.1:5174/login', { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(1000);
  await page.locator('input').nth(0).fill('merchant01');
  await page.locator('input').nth(1).fill('123456');
  await page.locator('button:has-text("登录系统")').click();
  await page.waitForTimeout(2500);
  const data = await page.evaluate(() => {
    const overlays = Array.from(document.querySelectorAll('.el-overlay, .el-popup-parent--hidden, .v-modal, .el-drawer__container, .el-dialog__wrapper')).map((el) => ({
      cls: el.className,
      display: getComputedStyle(el).display,
      opacity: getComputedStyle(el).opacity,
      pointerEvents: getComputedStyle(el).pointerEvents,
      rect: el.getBoundingClientRect().toJSON()
    }));
    const atUserBtn = document.elementsFromPoint(window.innerWidth - 120, 40).map(el => ({ tag: el.tagName, cls: el.className, text: (el.textContent || '').trim().slice(0, 60) }));
    const atMenu = document.elementsFromPoint(90, 140).map(el => ({ tag: el.tagName, cls: el.className, text: (el.textContent || '').trim().slice(0, 60) }));
    const routerViewHost = document.querySelector('.merchant-main');
    return {
      overlays,
      atUserBtn,
      atMenu,
      merchantMainHtml: routerViewHost ? routerViewHost.innerText.slice(0, 800) : null,
      bodyClass: document.body.className
    };
  });
  console.log(JSON.stringify(data, null, 2));
  await browser.close();
})();
