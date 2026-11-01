const { defineConfig, devices } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './tests',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: 0,
  workers: 1,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }]
  ],
  use: {
    baseURL: 'http://localhost:8085',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'off',
    headless: true,
    launchOptions: {
      slowMo: process.env.SLOWMO ? parseInt(process.env.SLOWMO, 10) : 0,
    },
  },

  projects: [
    {
      name: 'Google Chrome',
      use: { 
        ...devices['Desktop Chrome'],
        channel: 'chrome' 
      },
    },
    {
      name: 'Microsoft Edge',
      use: { 
        ...devices['Desktop Edge'],
        channel: 'msedge' 
      },
    },
    {
      name: 'Mobile Chrome (Pixel 7)',
      use: { 
        ...devices['Pixel 7'],
        channel: 'chrome' 
      },
    },
  ],

  webServer: {
    command: 'cmd.exe /c "cd /d D:\\Atish2\\PeoplePulseHR && set \"JAVA_HOME=C:\\Java\\jdk1.8\"&& set \"PATH=C:\\Java\\jdk1.8\\bin;%PATH%\"&& tools\\maven\\bin\\mvn.cmd spring-boot:run"',
    port: 8085,
    timeout: 120000,
    reuseExistingServer: true,
  },
});
