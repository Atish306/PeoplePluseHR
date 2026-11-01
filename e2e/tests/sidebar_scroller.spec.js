const { test, expect } = require('@playwright/test');

test.describe('Sidebar Custom Scroller Enhancements', () => {

  test.beforeEach(async ({ page }) => {
    // Log in as employee
    await page.goto('/login');
    await page.fill('#username', 'employee');
    await page.fill('#password', 'employee123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');
  });

  test('sidebar navigation container has modern custom scrollbar properties', async ({ page }) => {
    const nav = page.locator('.peoplepulse-nav');
    await expect(nav).toBeVisible();

    // Verify computed scroll properties
    const scrollStyles = await nav.evaluate(el => {
      const computed = window.getComputedStyle(el);
      return {
        overflowY: computed.overflowY,
        scrollBehavior: computed.scrollBehavior,
        scrollbarWidth: computed.scrollbarWidth
      };
    });

    expect(scrollStyles.overflowY).toBe('auto');
    expect(scrollStyles.scrollBehavior).toBe('smooth');
  });

  test('sidebar scroller styles are defined in stylesheet', async ({ page }) => {
    // Evaluate stylesheet rules for webkit-scrollbar on sidebar nav
    const hasScrollbarRules = await page.evaluate(() => {
      let foundThumb = false;
      let foundWidth = false;
      for (const sheet of Array.from(document.styleSheets)) {
        try {
          for (const rule of Array.from(sheet.cssRules || [])) {
            if (rule.selectorText && rule.selectorText.includes('.peoplepulse-nav::-webkit-scrollbar')) {
              foundWidth = true;
            }
            if (rule.selectorText && rule.selectorText.includes('.peoplepulse-nav::-webkit-scrollbar-thumb')) {
              foundThumb = true;
            }
          }
        } catch (e) {
          // ignore cross-origin stylesheets
        }
      }
      return foundWidth && foundThumb;
    });

    expect(hasScrollbarRules).toBe(true);
  });

});
