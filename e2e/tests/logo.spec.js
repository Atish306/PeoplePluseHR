const { test, expect } = require('@playwright/test');

test.describe('People+ HR Logo & Branding Verification', () => {

  test('landing page displays People+ HR logo brand mark and typography', async ({ page }) => {
    await page.goto('/');

    const logoIcon = page.locator('.people-plus-logo-icon').first();
    await expect(logoIcon).toBeVisible();

    const brand = page.locator('.people-plus-brand').first();
    await expect(brand).toBeVisible();

    await expect(brand.locator('.brand-word-people')).toHaveText('People');
    await expect(brand.locator('.brand-plus-sign')).toHaveText('+');
    await expect(brand.locator('.brand-word-hr')).toHaveText('HR');
  });

  test('login page displays People+ HR logo in hero brand panel', async ({ page }) => {
    await page.goto('/login');

    const heroBrand = page.locator('.login-hero-bg .people-plus-brand');
    await expect(heroBrand).toBeVisible();

    await expect(heroBrand.locator('.brand-word-people')).toHaveText('People');
    await expect(heroBrand.locator('.brand-plus-sign')).toHaveText('+');
    await expect(heroBrand.locator('.brand-word-hr')).toHaveText('HR');
  });

  test('authenticated sidebar displays People+ HR logo', async ({ page }) => {
    await page.goto('/login');
    await page.fill('#username', 'employee');
    await page.fill('#password', 'employee123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');

    const sidebarLogo = page.locator('.peoplepulse-sidebar-brand .people-plus-brand');
    await expect(sidebarLogo).toBeVisible();

    await expect(sidebarLogo.locator('.brand-word-people')).toHaveText('People');
    await expect(sidebarLogo.locator('.brand-plus-sign')).toHaveText('+');
    await expect(sidebarLogo.locator('.brand-word-hr')).toHaveText('HR');

    // Also check on attendance history page
    await page.goto('/attendance/history');
    await expect(sidebarLogo).toBeVisible();
    await expect(sidebarLogo.locator('.brand-word-people')).toHaveText('People');
  });

  test('book demo page displays People+ HR logo in top navbar', async ({ page }) => {
    await page.goto('/book-demo');

    const navbarBrand = page.locator('.navbar .people-plus-brand');
    await expect(navbarBrand).toBeVisible();

    await expect(navbarBrand.locator('.brand-word-people')).toHaveText('People');
    await expect(navbarBrand.locator('.brand-plus-sign')).toHaveText('+');
    await expect(navbarBrand.locator('.brand-word-hr')).toHaveText('HR');
  });

});
