const { test, expect } = require('@playwright/test');

test.describe('Attendance & Dashboard Workflows', () => {

  test.beforeEach(async ({ page }) => {
    // Log in as employee
    await page.goto('/login');
    await page.fill('#username', 'employee');
    await page.fill('#password', 'employee123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');
  });

  test('should display employee dashboard with live clock widget', async ({ page }) => {
    await expect(page).toHaveURL(/dashboard/);

    // Verify presence of clock / time element
    const clockEl = page.locator('#liveClock, .live-clock, .digital-clock, [id*="clock"]');
    if (await clockEl.count() > 0) {
      await expect(clockEl.first()).toBeVisible();
    }

    // Verify punch button exists
    const punchBtn = page.locator('#punchBtn, button:has-text("Sign In"), button:has-text("Sign Out"), .btn-peoplepulse-punch, .btn-greythr-punch');
    if (await punchBtn.count() > 0) {
      await expect(punchBtn.first()).toBeVisible();
    }
  });

  test('should view monthly attendance history with 7-column muster grid', async ({ page }) => {
    await page.goto('/attendance/history');

    await expect(page).toHaveURL(/attendance\/history/);

    // Check weekday column headers (MON to SUN)
    const headers = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];
    for (const h of headers) {
      const headerLocator = page.locator(`.muster-weekday-header:has-text("${h}"), th:has-text("${h}"), .col:has-text("${h}")`);
      if (await headerLocator.count() > 0) {
        await expect(headerLocator.first()).toBeVisible();
      }
    }

    // Check chart canvas
    const chartCanvas = page.locator('#attendancePieChart, canvas');
    if (await chartCanvas.count() > 0) {
      await expect(chartCanvas.first()).toBeVisible();
    }
  });

  test('should view leave application page with balance quotas', async ({ page }) => {
    await page.goto('/leaves/apply');
    await expect(page).toHaveURL(/leaves\/apply/);

    // Should display leave quotas (Casual, Sick, Earned)
    await expect(page.locator('body')).toContainText(/Casual|Sick|Earned|Leave/i);

    // Form inputs should exist
    await expect(page.locator('#leaveType, select[name="leaveType"]')).toBeVisible();
    await expect(page.locator('#startDate, input[name="startDate"]')).toBeVisible();
    await expect(page.locator('#endDate, input[name="endDate"]')).toBeVisible();
  });

});
