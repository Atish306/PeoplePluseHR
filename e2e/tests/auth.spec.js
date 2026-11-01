const { test, expect } = require('@playwright/test');

test.describe('Authentication & Access Control', () => {

  test('should display login page with branding and form controls', async ({ page }) => {
    await page.goto('/login');

    // Title should contain login / portal reference
    await expect(page).toHaveTitle(/Sign In|greytHR|PeoplePulse/i);

    // Form inputs should be visible
    const usernameInput = page.locator('#username');
    const passwordInput = page.locator('#password');
    const submitBtn = page.locator('button[type="submit"]');

    await expect(usernameInput).toBeVisible();
    await expect(passwordInput).toBeVisible();
    await expect(submitBtn).toBeVisible();
  });

  test('should show error notification on invalid credentials', async ({ page }) => {
    await page.goto('/login');

    await page.fill('#username', 'invalid_user');
    await page.fill('#password', 'wrong_password');
    await page.click('button[type="submit"]');

    // Should remain on login or redirect to /login?error=true with error message
    await expect(page).toHaveURL(/login/);
    const alert = page.locator('.alert-danger');
    await expect(alert).toBeVisible();
  });

  test('should login as Employee and navigate to /dashboard', async ({ page }) => {
    await page.goto('/login');

    // Test autofill button for employee
    const empQuickBtn = page.locator('button:has-text("Employee")');
    if (await empQuickBtn.isVisible()) {
      await empQuickBtn.click();
    } else {
      await page.fill('#username', 'employee');
      await page.fill('#password', 'employee123');
    }

    await page.click('button[type="submit"]');

    await page.waitForURL('**/dashboard');
    expect(page.url()).toContain('/dashboard');
    await expect(page.locator('body')).toContainText(/Alex Smith|Dashboard|Clock/i);
  });

  test('should login as Admin and navigate to /admin/dashboard', async ({ page }) => {
    await page.goto('/login');

    const adminQuickBtn = page.locator('button:has-text("Admin")');
    if (await adminQuickBtn.isVisible()) {
      await adminQuickBtn.click();
    } else {
      await page.fill('#username', 'admin');
      await page.fill('#password', 'admin123');
    }

    await page.click('button[type="submit"]');

    await page.waitForURL('**/admin/dashboard');
    expect(page.url()).toContain('/admin/dashboard');
  });

  test('should login as Manager and navigate to /manager/dashboard', async ({ page }) => {
    await page.goto('/login');

    const mgrQuickBtn = page.locator('button:has-text("Manager")');
    if (await mgrQuickBtn.isVisible()) {
      await mgrQuickBtn.click();
    } else {
      await page.fill('#username', 'manager');
      await page.fill('#password', 'manager123');
    }

    await page.click('button[type="submit"]');

    await page.waitForURL('**/manager/dashboard');
    expect(page.url()).toContain('/manager/dashboard');
  });

});
