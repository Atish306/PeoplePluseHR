const { test, expect } = require('@playwright/test');

test.describe('People+ HR Mobile App Tabs (Home, Action, Explore, Engage)', () => {

  // Test with standard mobile viewport (iPhone 14 / Pixel: 390x844)
  test.use({ viewport: { width: 390, height: 844 } });

  test.beforeEach(async ({ page }) => {
    // Log in as employee
    await page.goto('/login');
    await page.fill('#username', 'employee');
    await page.fill('#password', 'employee123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');
  });

  test('mobile login displays fixed bottom navigation bar with 4 tabs (Home, Action, Explore, Engage)', async ({ page }) => {
    const bottomNav = page.locator('#mobileAppBottomNav');
    await expect(bottomNav).toBeVisible();

    const homeTab = page.locator('#mobileAppBottomNav [data-tab="home"]');
    const actionTab = page.locator('#mobileAppBottomNav [data-tab="action"]');
    const exploreTab = page.locator('#mobileAppBottomNav [data-tab="explore"]');
    const engageTab = page.locator('#mobileAppBottomNav [data-tab="engage"]');

    await expect(homeTab).toBeVisible();
    await expect(actionTab).toBeVisible();
    await expect(exploreTab).toBeVisible();
    await expect(engageTab).toBeVisible();

    await expect(homeTab.locator('.mobile-nav-label')).toHaveText('Home');
    await expect(actionTab.locator('.mobile-nav-label')).toHaveText('Action');
    await expect(exploreTab.locator('.mobile-nav-label')).toHaveText('Explore');
    await expect(engageTab.locator('.mobile-nav-label')).toHaveText('Engage');

    // Home is active by default
    await expect(homeTab).toHaveClass(/active/);
    await expect(page.locator('#mobileTab-home')).toBeVisible();
    await expect(page.locator('#mobileTab-action')).not.toBeVisible();
    await expect(page.locator('#mobileTab-explore')).not.toBeVisible();
    await expect(page.locator('#mobileTab-engage')).not.toBeVisible();

    // Verify employee welcome banner
    await expect(page.locator('.mobile-banner-card')).toBeVisible();
    await expect(page.locator('.mobile-banner-card')).toContainText('Alex Smith');

    // Verify live clock sign-in widget on Home
    await expect(page.locator('#liveClock')).toBeVisible();
  });

  test('switching to Action tab displays quick punch desk, leave apply, and regularization', async ({ page }) => {
    const actionTab = page.locator('#mobileAppBottomNav [data-tab="action"]');
    await actionTab.click();

    // Tab state updates
    await expect(actionTab).toHaveClass(/active/);
    await expect(page.locator('#mobileAppBottomNav [data-tab="home"]')).not.toHaveClass(/active/);
    await expect(page.locator('#mobileTab-action')).toBeVisible();
    await expect(page.locator('#mobileTab-home')).not.toBeVisible();

    // Header title updates
    await expect(page.locator('#mobilePageTitle')).toHaveText('Action Center');

    // Verify Action Center elements
    await expect(page.locator('#mobileTab-action')).toContainText('Instant Self-Service & Attendance Desk');
    await expect(page.locator('#mobileTab-action')).toContainText('Quick Punch Desk');
    await expect(page.locator('#mobileTab-action')).toContainText('Attendance Regularization');
    await expect(page.locator('#mobileTab-action')).toContainText('Remote Duty & WFH');
    await expect(page.locator('#mobileTab-action')).toContainText('My Recent Requests & Approvals');

    // Test regularization form submission feedback
    await page.fill('#regularizeDate', '2026-10-25');
    await page.fill('#regularizeReason', 'Punched card did not register');
    await page.click('#mobileTab-action button:has-text("Submit Regularization Request")');

    // Verify toast notification
    const toast = page.locator('#mobileAppToast');
    await expect(toast).toBeVisible();
    await expect(page.locator('#mobileToastMessage')).toContainText('Regularization request for 2026-10-25 submitted');
  });

  test('switching to Explore tab displays directory catalog with search filter', async ({ page }) => {
    const exploreTab = page.locator('#mobileAppBottomNav [data-tab="explore"]');
    await exploreTab.click();

    // Tab state updates
    await expect(exploreTab).toHaveClass(/active/);
    await expect(page.locator('#mobileTab-explore')).toBeVisible();
    await expect(page.locator('#mobileTab-home')).not.toBeVisible();

    // Header title updates
    await expect(page.locator('#mobilePageTitle')).toHaveText('Explore Directory');

    // Verify Explore categories
    await expect(page.locator('#mobileTab-explore')).toContainText('Attendance Muster Log');
    await expect(page.locator('#mobileTab-explore')).toContainText('Holiday Calendar 2026');
    await expect(page.locator('#mobileTab-explore')).toContainText('Shift Roster & Rules');
    await expect(page.locator('#mobileTab-explore')).toContainText('Leave Balances & Apply');
    await expect(page.locator('#mobileTab-explore')).toContainText('Book Platform Demo');

    // Test interactive search filter
    const searchInput = page.locator('#exploreSearchInput');
    await searchInput.fill('muster');
    await expect(page.locator('.explore-item:has-text("Attendance Muster Log")')).toBeVisible();
    await expect(page.locator('.explore-item:has-text("Holiday Calendar 2026")')).not.toBeVisible();

    // Clear search filter
    await searchInput.fill('');
    await expect(page.locator('.explore-item:has-text("Holiday Calendar 2026")')).toBeVisible();
  });

  test('switching to Engage tab displays daily pulse, celebrations, announcements, and kudos', async ({ page }) => {
    const engageTab = page.locator('#mobileAppBottomNav [data-tab="engage"]');
    await engageTab.click();

    // Tab state updates
    await expect(engageTab).toHaveClass(/active/);
    await expect(page.locator('#mobileTab-engage')).toBeVisible();
    await expect(page.locator('#mobileTab-home')).not.toBeVisible();

    // Header title updates
    await expect(page.locator('#mobilePageTitle')).toHaveText('Engage & Connect');

    // Verify daily mood pulse options
    await expect(page.locator('.engage-pulse-btn:has-text("Energized")')).toBeVisible();
    await expect(page.locator('.engage-pulse-btn:has-text("Productive")')).toBeVisible();
    await expect(page.locator('.engage-pulse-btn:has-text("Need Coffee")')).toBeVisible();
    await expect(page.locator('.engage-pulse-btn:has-text("Focused")')).toBeVisible();

    // Click mood button and verify live feedback
    await page.click('.engage-pulse-btn:has-text("Energized")');
    await expect(page.locator('#pulseRecordedAlert')).toBeVisible();
    await expect(page.locator('#pulseRecordedAlert')).toContainText('Energized');

    // Verify celebrations (birthdays & anniversaries)
    await expect(page.locator('#mobileTab-engage')).toContainText('Sarah Jenkins');
    await expect(page.locator('#mobileTab-engage')).toContainText('Birthday Today! 🎉');
    await expect(page.locator('#mobileTab-engage')).toContainText('Priya Sharma');

    // Click wish button and verify toast
    await page.click('#mobileTab-engage button:has-text("Wish 🎉")');
    const toast = page.locator('#mobileAppToast');
    await expect(toast).toBeVisible();
    await expect(page.locator('#mobileToastMessage')).toContainText('Sent warm Birthday wishes to Sarah Jenkins');

    // Verify announcements and kudos wall
    await expect(page.locator('#mobileTab-engage')).toContainText('Annual Q4 Townhall Meeting');
    await expect(page.locator('#mobileTab-engage')).toContainText('Peer Kudos & High-Fives');
  });

  test('switching back to Home tab restores primary dashboard', async ({ page }) => {
    // Switch to Action then back to Home
    await page.click('#mobileAppBottomNav [data-tab="action"]');
    await expect(page.locator('#mobileTab-action')).toBeVisible();

    await page.click('#mobileAppBottomNav [data-tab="home"]');
    await expect(page.locator('#mobileTab-home')).toBeVisible();
    await expect(page.locator('#mobileTab-action')).not.toBeVisible();
    await expect(page.locator('#mobileAppBottomNav [data-tab="home"]')).toHaveClass(/active/);
  });

  test('direct URL hash navigation activates corresponding mobile tab', async ({ page }) => {
    await page.goto('/dashboard#explore');
    await expect(page.locator('#mobileTab-explore')).toBeVisible();
    await expect(page.locator('#mobileAppBottomNav [data-tab="explore"]')).toHaveClass(/active/);

    await page.goto('/dashboard#engage');
    await expect(page.locator('#mobileTab-engage')).toBeVisible();
    await expect(page.locator('#mobileAppBottomNav [data-tab="engage"]')).toHaveClass(/active/);
  });
});

test.describe('Desktop Layout Verification (min-width: 992px)', () => {
  test.use({ viewport: { width: 1280, height: 800 } });

  test('desktop screens hide bottom navigation bar and maintain full desktop sidebar layout', async ({ page }) => {
    await page.goto('/login');
    await page.fill('#username', 'employee');
    await page.fill('#password', 'employee123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');

    // Bottom nav bar must be hidden on desktop
    const bottomNav = page.locator('#mobileAppBottomNav');
    await expect(bottomNav).not.toBeVisible();

    // Desktop sidebar must be visible
    const sidebar = page.locator('.peoplepulse-sidebar');
    await expect(sidebar).toBeVisible();

    // Home content is visible on desktop
    await expect(page.locator('#mobileTab-home')).toBeVisible();

    // Mobile-only tabs are not displayed on desktop
    await expect(page.locator('#mobileTab-action')).not.toBeVisible();
    await expect(page.locator('#mobileTab-explore')).not.toBeVisible();
    await expect(page.locator('#mobileTab-engage')).not.toBeVisible();
  });
});
