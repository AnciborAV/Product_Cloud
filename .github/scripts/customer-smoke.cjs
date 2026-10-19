// Сквозная браузерная проверка кабинета и корзины; запускается только в CI на временной demo-БД.
const { chromium } = require(process.env.PLAYWRIGHT_PACKAGE);
const fs = require('node:fs/promises');
const assert = require('node:assert/strict');
(async () => {
    const baseURL = 'http://127.0.0.1:8080';
    for (let attempt = 0; attempt < 60; attempt++) {
        try { if ((await fetch(baseURL + '/products')).ok) break; } catch {}
        if (attempt === 59) throw new Error('Приложение не запустилось за 60 секунд');
        await new Promise(resolve => setTimeout(resolve, 1000));
    }
    await fs.mkdir('target/ui', { recursive: true });
    const browser = await chromium.launch();
    try {
        const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
        const page = await context.newPage();
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        await page.goto(baseURL + '/products');
        const photo = await page.locator('.hero-art').screenshot({ type: 'png' });
        await page.goto(baseURL + '/register');
        await page.getByLabel('Как вас зовут').fill('Андрей');
        await page.getByLabel('Электронная почта', { exact: true }).fill('browser-test@example.com');
        await page.getByLabel('Пароль', { exact: true }).fill('browser-password-123');
        await page.getByLabel('Повторите пароль', { exact: true }).fill('browser-password-123');
        await page.locator('#avatar').setInputFiles({ name: 'avatar.png', mimeType: 'image/png', buffer: photo });
        await page.screenshot({ path: 'target/ui/register-desktop.png', fullPage: true });
        await page.getByRole('button', { name: 'Создать мой кабинет' }).click();
        await page.waitForURL('**/login');
        await page.getByLabel('Электронная почта', { exact: true }).fill('browser-test@example.com');
        await page.getByLabel('Пароль', { exact: true }).fill('browser-password-123');
        await page.getByRole('button', { name: 'Войти в кабинет' }).click();
        await page.waitForURL('**/account');
        await page.getByLabel('Телефон', { exact: true }).fill('+7 999 123-45-67');
        await page.getByLabel('Адрес', { exact: true }).fill('Москва, ул. Садовая, д. 10');
        await page.getByRole('button', { name: 'Сохранить данные' }).click();
        await page.waitForURL('**/account');
        await page.screenshot({ path: 'target/ui/account-desktop.png', fullPage: true });
        await page.goto(baseURL + '/products');
        await page.getByLabel('Название продукта', { exact: true }).fill('Томаты сливовидные');
        await page.getByLabel('Цена, ₽', { exact: true }).fill('119.90');
        await page.getByRole('button', { name: 'Добавить товар', exact: true }).click();
        await page.getByRole('button', { name: 'Добавить в корзину: Томаты сливовидные', exact: true }).click();
        await page.goto(baseURL + '/cart');
        await page.getByRole('button', { name: 'Увеличить количество: Томаты сливовидные', exact: true }).click();
        assert.equal(await page.locator('.quantity-edit input[type=number]').inputValue(), '2');
        await page.screenshot({ path: 'target/ui/cart-desktop.png', fullPage: true });
        await page.getByRole('button', { name: 'Удалить из корзины: Томаты сливовидные', exact: true }).click();
        await page.getByRole('heading', { name: 'Впереди что-то вкусное' }).waitFor();
        await page.goto(baseURL + '/products');
        await page.getByRole('button', { name: 'Добавить в корзину: Томаты сливовидные', exact: true }).click();
        await page.goto(baseURL + '/cart');
        await page.getByRole('button', { name: 'Оформить заказ' }).click();
        await page.waitForURL(/\/orders\/\d+$/);
        await page.getByRole('heading', { name: 'Состав заказа' }).waitFor();
        await page.screenshot({ path: 'target/ui/order-desktop.png', fullPage: true });
        await page.getByRole('button', { name: 'Повторить заказ' }).click();
        await page.waitForURL('**/cart');
        await page.setViewportSize({ width: 390, height: 844 });
        await page.screenshot({ path: 'target/ui/cart-mobile.png', fullPage: true });
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false, 'Переполнение корзины на телефоне');
        await page.goto(baseURL + '/account');
        await page.screenshot({ path: 'target/ui/account-mobile.png', fullPage: true });
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false, 'Переполнение кабинета на телефоне');
        assert.deepEqual(errors, [], 'Ошибки JavaScript');
        await context.close();
        console.log('Регистрация с фото, вход, профиль, корзина, удаление, оформление и повтор заказа: OK');
    } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
