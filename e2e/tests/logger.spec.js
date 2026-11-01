const { test, expect } = require('@playwright/test');

test.describe('Custom Logger & Endpoint Diagnostics', () => {

  test('should return X-Trace-ID header on HTTP requests', async ({ request }) => {
    const response = await request.get('/login');
    expect(response.status()).toBe(200);
    const traceId = response.headers()['x-trace-id'];
    expect(traceId).toBeDefined();
    expect(traceId).toMatch(/^REQ-[A-Z0-9]+$/);
  });

  test('should allow admin to access Endpoint Debugger and view diagnostic KPI cards', async ({ page }) => {
    // 1. Log in as admin
    await page.goto('/login');
    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');

    await page.waitForURL('**/admin/dashboard');

    // 2. Navigate to Endpoint Debugger
    await page.goto('/admin/debug/logs');
    await expect(page).toHaveTitle(/Endpoint Debug Logs/i);

    // Verify KPI summary metrics
    await expect(page.locator('text=TOTAL REQUESTS TRACED')).toBeVisible();
    await expect(page.locator('text=BUGS / 500 ERRORS')).toBeVisible();
    await expect(page.locator('text=SLOW ENDPOINTS')).toBeVisible();

    // Verify Captured Table
    await expect(page.locator('table')).toBeVisible();
    await expect(page.locator('th:has-text("Trace ID")')).toBeVisible();
  });

  test('should execute printSuccessLog and printErrorLog test actions', async ({ page }) => {
    // Log in as admin
    await page.goto('/login');
    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');

    // Trigger test printSuccessLog
    await page.goto('/admin/debug/test-custom-logger?action=success');
    await expect(page).toHaveURL(/admin\/debug\/logs/);

    // Trigger test printErrorLog
    await page.goto('/admin/debug/test-custom-logger?action=error');
    await expect(page).toHaveURL(/admin\/debug\/logs/);
    await expect(page.locator('body')).toContainText(/printErrorLog|IllegalArgumentException/i);
  });

  test('should return JSON diagnostics from /admin/debug/api/logs', async ({ page, request }) => {
    // Log in session via browser
    await page.goto('/login');
    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');

    // Direct fetch of JSON API
    const response = await page.goto('/admin/debug/api/logs');
    expect(response.status()).toBe(200);
    const content = await page.textContent('body');
    const logs = JSON.parse(content);
    expect(Array.isArray(logs)).toBeTruthy();
    expect(logs.length).toBeGreaterThan(0);
    expect(logs[0].traceId).toBeDefined();
  });

});
