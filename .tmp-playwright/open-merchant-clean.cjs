const { chromium } = require("playwright-core");
const path = require("path");

(async () => {
  const profileDir = path.join("D:/bishe/project", `.edge-merchant-clean-${Date.now()}`);
  const context = await chromium.launchPersistentContext(profileDir, {
    headless: false,
    executablePath: "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",
    args: ["--window-size=1440,960"]
  });

  const page = context.pages()[0] || (await context.newPage());
  await page.goto("http://127.0.0.1:5174/login", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1200);

  const inputs = await page.locator("input").count();
  if (inputs >= 2) {
    await page.locator("input").nth(0).fill("merchant01");
    await page.locator("input").nth(1).fill("123456");
    const loginButton = page.locator('button:has-text("登录系统")');
    if (await loginButton.count()) {
      await loginButton.click();
      await page.waitForTimeout(2500);
    }
  }

  await page.goto("http://127.0.0.1:5174/merchant/route/list", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(2000);

  setInterval(() => {}, 1000);
})();
