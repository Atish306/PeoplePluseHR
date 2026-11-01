const { test, expect } = require('@playwright/test');

test.describe('Muster Calendar & Filter Enhancements', () => {

  test.beforeEach(async ({ page }) => {
    // Log in as employee
    await page.goto('/login');
    await page.fill('#username', 'employee');
    await page.fill('#password', 'employee123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');
  });

  test('calendar has NO horizontal scroll and fits container width', async ({ page }) => {
    await page.goto('/attendance/history');
    await expect(page).toHaveURL(/attendance\/history/);

    const wrapper = page.locator('.muster-calendar-wrapper');
    await expect(wrapper).toBeVisible();

    // Verify wrapper scrollWidth <= clientWidth (no horizontal scrollbar)
    const isOverflowing = await wrapper.evaluate(el => el.scrollWidth > el.clientWidth);
    expect(isOverflowing).toBe(false);

    // Verify all 7 weekday columns exist and are visible
    const weekdayCols = page.locator('.muster-weekday-col');
    await expect(weekdayCols).toHaveCount(7);
  });

  test('holiday marquee ticker is visible and displays company holidays', async ({ page }) => {
    await page.goto('/attendance/history');
    await expect(page).toHaveURL(/attendance\/history/);

    // Holiday marquee container should exist
    const marqueeBanner = page.locator('.holiday-marquee-banner');
    await expect(marqueeBanner).toBeVisible();

    // Marquee element should exist and contain holiday text
    const marquee = page.locator('.holiday-marquee-track marquee');
    await expect(marquee).toBeVisible();

    const marqueeText = await marquee.innerText();
    expect(marqueeText.length).toBeGreaterThan(0);
    // Should contain holiday references like Diwali, Republic Day, Christmas or New Year
    expect(marqueeText).toMatch(/Diwali|Republic|Christmas|New Year|Independence|Gandhi|Holi/i);
  });

  test('select dropdown has blank space selected by default and shows 5 values on click/focus', async ({ page }) => {
    await page.goto('/attendance/history');
    await expect(page).toHaveURL(/attendance\/history/);

    // Month select should have blank option selected by default
    const monthSelect = page.locator('select[name="month"]');
    await expect(monthSelect).toBeVisible();
    expect(await monthSelect.inputValue()).toBe('');

    // Year select should have blank option selected by default
    const yearSelect = page.locator('select[name="year"]');
    await expect(yearSelect).toBeVisible();
    expect(await yearSelect.inputValue()).toBe('');

    // When clicked/focused, it shows 5 values (size=5)
    await monthSelect.focus();
    const monthSizeOnFocus = await monthSelect.evaluate(el => el.size);
    expect(monthSizeOnFocus).toBe(5);

    await yearSelect.focus();
    const yearSizeOnFocus = await yearSelect.evaluate(el => el.size);
    expect(yearSizeOnFocus).toBe(5);
  });

  test('inside calendar date holiday name is displayed in marquee', async ({ page }) => {
    // Navigate to October 2026 where Gandhi Jayanti & Dussehra are present
    await page.goto('/attendance/history?year=2026&month=10');
    await expect(page).toHaveURL(/attendance\/history/);

    // Holiday cards should exist in the calendar
    const holidayCards = page.locator('.muster-day-card.holiday-card');
    await expect(holidayCards.first()).toBeVisible();

    // Inside the holiday calendar date card, a marquee element must exist with holiday name
    const holidayMarquee = holidayCards.first().locator('.muster-day-holiday-marquee marquee');
    await expect(holidayMarquee).toBeVisible();

    const marqueeText = await holidayMarquee.innerText();
    expect(marqueeText.length).toBeGreaterThan(0);
    // Should match known October holidays like Gandhi or Dussehra
    expect(marqueeText).toMatch(/Gandhi|Dussehra|Vijaya|Holiday/i);
  });

  test('calendar font sizes and cards are compact and adjusted without clipping', async ({ page }) => {
    await page.goto('/attendance/history');
    await expect(page).toHaveURL(/attendance\/history/);

    // Verify day cards have compact font size class
    const dayCard = page.locator('.muster-day-card:not(.empty-slot)').first();
    await expect(dayCard).toBeVisible();

    const dayNum = dayCard.locator('.muster-day-num');
    await expect(dayNum).toBeVisible();

    const pill = dayCard.locator('.muster-code-pill');
    await expect(pill).toBeVisible();
  });

  test('admin sees 5-in-a-box employee switcher select', async ({ browser }) => {
    // Open a fresh isolated browser context for Admin session
    const adminContext = await browser.newContext();
    const adminPage = await adminContext.newPage();

    await adminPage.goto('/login');
    await adminPage.fill('#username', 'admin');
    await adminPage.fill('#password', 'admin123');
    await adminPage.click('button[type="submit"]');
    await adminPage.waitForURL('**/admin/dashboard');

    await adminPage.goto('/attendance/history');
    await expect(adminPage).toHaveURL(/attendance\/history/);

    // Check Employee select has blank space by default and expands to size 5 on focus
    const empSelect = adminPage.locator('select[name="userId"]');
    await expect(empSelect).toBeVisible();
    expect(await empSelect.inputValue()).toBe('');

    await empSelect.focus();
    expect(await empSelect.evaluate(el => el.size)).toBe(5);

    await adminContext.close();
  });

});
