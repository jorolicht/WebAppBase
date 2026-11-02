import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object representing UserRegistration (Neuregistrierung).
 */
export class UserRegistrationPage {
  readonly page: Page;
  readonly emailInput: Locator;
  readonly passwordlessSwitch: Locator;
  readonly passwordContainer: Locator;
  readonly passwordInput: Locator;
  readonly submitBtn: Locator;
  readonly successView: Locator;

  constructor(page: Page) {
    this.page = page;
    this.emailInput = page.locator('input[id*="EmailId"]');
    this.passwordlessSwitch = page.locator('input[id*="PasswordlessSwitch"]');
    this.passwordContainer = page.locator('div[id*="PasswordContainer"]');
    this.passwordInput = page.locator('input[id*="PasswordId"]');
    this.submitBtn = page.locator('button[id*="BtnSubmit"]');
    this.successView = page.locator('.registration-success-view');
  }

  async goto() {
    await this.page.goto('/registration/');
    await expect(this.emailInput).toBeVisible({ timeout: 10000 });
  }

  async register(options: {
    email: string;
    password?: string;
  }) {
    await this.emailInput.fill(options.email);

    if (options.password) {
      if (await this.passwordlessSwitch.isChecked()) {
        await this.passwordlessSwitch.click();
      }
      await expect(this.passwordInput).toBeVisible({ timeout: 3000 });
      await this.passwordInput.fill(options.password);
    }

    // Ensure Cloudflare Turnstile test token is present
    await this.page.evaluate(() => {
      let input = document.getElementById('cf-dummy-token') as HTMLInputElement;
      if (!input) {
        input = document.createElement('input');
        input.id = 'cf-dummy-token';
        input.type = 'hidden';
        input.name = 'cf-turnstile-response';
        document.body.insertBefore(input, document.body.firstChild);
      }
      input.value = 'XXXX.DUMMY.TOKEN.XXXX';
      document.querySelectorAll('input[name="cf-turnstile-response"]').forEach((el: any) => {
        el.value = 'XXXX.DUMMY.TOKEN.XXXX';
      });
    });

    await this.submitBtn.click();
  }
}
