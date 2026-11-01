const { test, expect } = require('@playwright/test');

test.describe('Book a Demo Pipeline', () => {

  test('should display split-pane booking form with branding and controls', async ({ page }) => {
    await page.goto('/book-demo');

    // Verify title and main branding
    await expect(page).toHaveTitle(/Book a Demo/i);
    await expect(page.locator('h1')).toContainText(/See PeoplePulse HR in Action/i);
    await expect(page.locator('h3')).toContainText(/Schedule Your Demo/i);

    // Verify form fields
    await expect(page.locator('#firstName')).toBeVisible();
    await expect(page.locator('#lastName')).toBeVisible();
    await expect(page.locator('#workEmail')).toBeVisible();
    await expect(page.locator('#phone')).toBeVisible();
    await expect(page.locator('#companyName')).toBeVisible();
    await expect(page.locator('#country')).toBeVisible();
    await expect(page.locator('#companySize')).toBeVisible();
    await expect(page.locator('#preferredDate')).toBeVisible();
    await expect(page.locator('#preferredTimeSlot')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
  });

  test('should submit a demo booking request and display confirmation page', async ({ page }) => {
    await page.goto('/book-demo');

    // Fill form
    await page.fill('#firstName', 'Bruce');
    await page.fill('#lastName', 'Wayne');
    await page.fill('#workEmail', 'bruce@wayne-enterprises.com');
    await page.fill('#phone', '+91 98888 77777');
    await page.fill('#companyName', 'Wayne Enterprises');
    await page.fill('#jobTitle', 'CEO & Founder');
    await page.selectOption('#country', 'United Kingdom');
    await page.selectOption('#companySize', '501 - 1,000');
    await page.selectOption('#preferredTimeSlot', 'Morning (10:00 - 11:00 AM)');
    await page.fill('#notes', 'Need multi-location attendance and geofenced perimeter verification.');

    // Submit
    await page.click('button[type="submit"]');

    // Should redirect to confirmation page
    await page.waitForURL(/\/book-demo\/confirmation\?ref=DEMO-/);
    expect(page.url()).toContain('/book-demo/confirmation?ref=DEMO-');

    // Verify confirmation content
    await expect(page.locator('h2')).toContainText(/Your Demo Walkthrough is Confirmed/i);
    await expect(page.locator('.ref-pill')).toContainText(/DEMO-/);
    await expect(page.locator('.summary-info-card')).toContainText(/Wayne Enterprises/i);
    await expect(page.locator('.summary-info-card')).toContainText(/United Kingdom/i);
    await expect(page.locator('.confirmation-box')).toContainText(/bruce@wayne-enterprises.com/i);
  });

  test('should allow admin to review and confirm demo booking request', async ({ page }) => {
    // 1. Login as admin
    await page.goto('/login');
    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/admin/dashboard');

    // 2. Navigate to demo bookings admin portal
    await page.goto('/admin/demo-bookings');
    await expect(page).toHaveTitle(/Demo Requests/i);

    // Verify lead table contains Wayne Enterprises
    const wayneRow = page.locator('tr:has-text("Wayne Enterprises")').first();
    await expect(wayneRow).toBeVisible();

    // 3. Confirm the demo
    const confirmBtn = wayneRow.locator('button[title="Confirm Demo"]');
    if (await confirmBtn.isVisible()) {
      await confirmBtn.click();
      await page.waitForURL('**/admin/demo-bookings');
      await expect(page.locator('.alert-success')).toBeVisible();
    }
  });

});
