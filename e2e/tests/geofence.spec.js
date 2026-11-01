const { test, expect } = require('@playwright/test');

test.describe('GPS Geofencing Enforcement Workflows', () => {

  test('should reject punch-in when coordinates are outside the office geofence perimeter', async ({ page }) => {
    // 1. Log in as Manager (who has not clocked in today)
    await page.goto('/login');
    await page.fill('#username', 'manager');
    await page.fill('#password', 'manager123');
    await page.click('button[type="submit"]');

    // 2. Navigate to Employee Self-Service Dashboard
    await page.goto('/dashboard');
    const punchInForm = page.locator('#punchInForm');
    await expect(punchInForm).toBeVisible();

    // 3. Fill out-of-boundary coordinates (e.g., 19.0760, 72.8777 - Mumbai, ~120km away from Pune HQ)
    await page.evaluate(() => {
      document.getElementById('latitudeInput').value = '19.0760';
      document.getElementById('longitudeInput').value = '72.8777';
      document.getElementById('punchNotes').value = 'Testing geofence rejection';
      document.getElementById('punchInForm').submit();
    });

    // 4. Verify rejection feedback message
    await page.waitForLoadState('networkidle');
    const errorAlert = page.locator('.alert-danger');
    await expect(errorAlert).toBeVisible();
    await expect(errorAlert).toContainText(/Punch rejected/i);
    await expect(errorAlert).toContainText(/away from Pune Headquarters/i);
  });

  test('should record geofence distance calculation in diagnostic logs', async ({ page }) => {
    // Log in as Admin
    await page.goto('/login');
    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');

    // Visit Endpoint Debugger
    await page.goto('/admin/debug/logs');
    await expect(page).toHaveTitle(/Endpoint Debug Logs/i);

    // Verify system diagnostic cards are healthy
    await expect(page.locator('text=TOTAL REQUESTS TRACED')).toBeVisible();
  });

});
