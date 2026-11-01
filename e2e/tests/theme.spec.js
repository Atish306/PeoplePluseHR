const { test, expect } = require('@playwright/test');

test.describe('Theme & Dark Mode Switcher', () => {

  test('should toggle dark mode on login page', async ({ page }) => {
    await page.goto('/login');

    const html = page.locator('html');
    const toggleBtn = page.locator('.theme-toggle-btn');

    await expect(toggleBtn).toBeVisible();

    const initialTheme = await html.getAttribute('data-bs-theme');

    // Click toggle button
    await toggleBtn.click();

    // Check that data-bs-theme changed
    const switchedTheme = await html.getAttribute('data-bs-theme');
    expect(switchedTheme).not.toEqual(initialTheme);

    // Click toggle button again to restore
    await toggleBtn.click();
    const restoredTheme = await html.getAttribute('data-bs-theme');
    expect(restoredTheme).toEqual(initialTheme);
  });

});
