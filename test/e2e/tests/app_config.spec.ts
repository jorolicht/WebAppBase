import { test, expect, Page } from '@playwright/test';

/**
 * Menü, Karten und Mockup-Seite aus der App-Konfiguration (wp-plugin/config/app.json).
 * Die Tests nutzen die Beispielkonfiguration der Vorlage.
 */

/** Öffnet die Startseite und wartet, bis der Client Navbar und Karten gerendert hat. */
async function openHome(page: Page, path = '/') {
  await page.goto(path);
  await expect(page.locator('#Navbar_Menu-features')).toBeVisible({ timeout: 15000 });
}

test.describe('App-Konfiguration: Menü, Karten, Mockup', () => {

  test('TC-CFG-01 [🌐]: Navbar und Karten kommen aus config/app.json', async ({ page, request }) => {
    const config = await (await request.get('/wp-content/plugins/webapp/config/app.json')).json();
    await openHome(page);
    for (const item of config.menu) {
      const id = item.children?.length ? `#Navbar_Dropdown-${item.id}` : `#Navbar_Menu-${item.id}`;
      await expect(page.locator(id)).toContainText(item.label.de);
    }
    await expect(page.locator('.main-multi-landing .card')).toHaveCount(config.cards.length);
    for (const card of config.cards) {
      await expect(page.locator(`#MainView_Card-${card.id}`)).toHaveText(card.btn.de);
    }
  });

  test('TC-CFG-02 [🌐]: Startseite steht schon im Server-HTML (SEO)', async ({ request }) => {
    const html = await (await request.get('/')).text();
    expect(html).toContain('Willkommen bei WebAppBase');
    expect(html).toContain('Demo starten');
  });

  test('TC-CFG-03 [🌐]: Menüeintrag ohne Aktion öffnet die Mockup-Seite', async ({ page }) => {
    await openHome(page);
    await page.click('#Navbar_Dropdown-data');
    await page.click('#Navbar_Menu-dataList');
    await expect(page.locator('[data-mockup="dataList"]')).toBeVisible();
    await expect(page.locator('h1')).toContainText('Übersicht');
    await expect(page.locator('[data-mockup="dataList"] tbody tr')).toHaveCount(5);

    // Aktion der Mockup-Seite zeigt den Platzhalter-Dialog
    await page.click('#Mockup_BtnAction');
    await expect(page.locator('#DlgMsgbox_BodyId')).toContainText('noch nicht umgesetzt');
  });

  test('TC-CFG-04 [🌐]: Sichtbarkeit "user" blendet Einträge für Gäste aus', async ({ page }) => {
    await openHome(page);
    await page.click('#Navbar_Dropdown-data');
    await expect(page.locator('#Navbar_Menu-dataList')).toBeVisible();
    await expect(page.locator('#Navbar_Menu-dataNew')).toHaveCount(0);
    await expect(page.locator('#Navbar_Menu-dataImport')).toHaveCount(0);
  });

  test('TC-CFG-05 [🌐]: Aktion static: öffnet die statische Seite mit eigener URL', async ({ page }) => {
    await openHome(page);
    await page.click('#Navbar_Menu-features');
    await expect(page.locator('#wp-content-body')).toBeVisible();
    await expect(page).toHaveURL(/\/funktionen\/$/);
  });

  test('TC-CFG-06 [🌐]: Aktion url: verlinkt extern in neuem Tab', async ({ page }) => {
    await openHome(page);
    const link = page.locator('#Navbar_Menu-helpWp');
    await expect(link).toHaveAttribute('href', 'https://wordpress.org');
    await expect(link).toHaveAttribute('target', '_blank');
  });

  test('TC-CFG-07 [🌐]: Aktion page: der Karte öffnet die Seite', async ({ page }) => {
    await openHome(page);
    await page.click('#MainView_Card-cardLogin');
    await expect(page.locator('#UserLogin_BtnLogin')).toBeVisible({ timeout: 10000 });
  });

  test('TC-CFG-08 [🌐]: Englische Texte unter /en/', async ({ page }) => {
    await openHome(page, '/en/');
    await expect(page.locator('#Navbar_Menu-features')).toHaveText('Features');
    await expect(page.locator('#MainView_Card-cardDemo')).toHaveText('Start demo');
  });

  test('TC-CFG-09 [👤]: Verwaltung zeigt Play-Server-Version und WebSocket-Antwort', async ({ page }) => {
    const user = process.env.WEBAPP_ADMIN_USER;
    const password = process.env.WEBAPP_ADMIN_PASSWORD;
    test.skip(!user || !password, 'WEBAPP_ADMIN_USER/WEBAPP_ADMIN_PASSWORD nicht gesetzt');

    await page.goto('/login/');
    await page.fill('#UserLogin_LoginId', user!);
    await page.fill('#UserLogin_PasswordId', password!);
    await page.click('#UserLogin_BtnLogin');
    await expect(page.locator('#LoginSuccessAlert')).toBeVisible({ timeout: 10000 });

    await openHome(page);
    await page.click('#navbarManagement');
    await expect(page.locator('#Management_ServerVersionId')).toHaveText(/^\d+\.\d+\.\d+$/, { timeout: 10000 });
    await page.click('#Management_BtnWsConnect');
    await page.waitForTimeout(1000);
    await page.click('#Management_BtnWsSend');
    await expect(page.locator('#Management_WsLogId')).toContainText('<- Hallo hier ist der Server', { timeout: 10000 });
  });
});
