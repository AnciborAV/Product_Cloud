'use strict';
// JavaScript отвечает за интерактивность. Проверка заказа и запись на диск выполняются в Java.
// Карта доступных книг для интерфейса; сервер при оформлении повторно определяет цены.
const catalog = new Map([...document.querySelectorAll('.add')].map(button => [button.dataset.id, {
  id: button.dataset.id, title: button.dataset.title, price: Number(button.dataset.price)
}]));
// Ключ локальной корзины в браузере; это не серверная история заказов.
const storageKey = 'quiet-shelf-cart-v1';
let cart = {};
try {
  const saved = JSON.parse(localStorage.getItem(storageKey) || '{}');
  for (const [id, quantity] of Object.entries(saved || {})) {
    if (catalog.has(id) && Number.isInteger(quantity) && quantity > 0 && quantity <= 10) cart[id] = quantity;
  }
} catch (_) { /* Недоступное/повреждённое хранилище не мешает пользоваться магазином. */ }
const money = value => new Intl.NumberFormat('ru-RU', {style: 'currency', currency: 'RUB', maximumFractionDigits: 0}).format(value);
const dialog = document.querySelector('#cart-dialog');
let busy = false;
let toastTimer;
/** Показывает короткое уведомление и отменяет прежний таймер скрытия. */
function notify(message) {
  const toast = document.querySelector('#toast');
  toast.textContent = message;
  toast.classList.add('visible');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('visible'), 2400);
}
/** Сохраняет текущую корзину в localStorage; при запрете хранилища работа продолжается в памяти. */
function persist() {
  try { localStorage.setItem(storageKey, JSON.stringify(cart)); } catch (_) { /* Работаем в памяти. */ }
}
/** Перестраивает DOM корзины через textContent, пересчитывает отображаемые суммы и доступность кнопок. */
function renderCart() {
  const container = document.querySelector('#cart-items');
  container.replaceChildren();
  let total = 0, count = 0;
  for (const [id, quantity] of Object.entries(cart)) {
    const book = catalog.get(id);
    total += book.price * quantity; count += quantity;
    const row = document.createElement('div'); row.className = 'cart-row';
    const info = document.createElement('div');
    const title = document.createElement('strong'); title.textContent = book.title;
    const price = document.createElement('p'); price.textContent = money(book.price * quantity);
    info.append(title, price);
    const controls = document.createElement('div'); controls.className = 'quantity';
    for (const [label, delta] of [['−', -1], ['+', 1]]) {
      const button = document.createElement('button'); button.textContent = label;
      button.setAttribute('aria-label', (delta > 0 ? 'Добавить: ' : 'Убрать: ') + book.title);
      button.disabled = busy || (delta > 0 && quantity === 10);
      button.onclick = () => { cart[id] += delta; if (!cart[id]) delete cart[id]; persist(); renderCart(); };
      controls.append(button);
      if (delta < 0) { const number = document.createElement('span'); number.textContent = quantity; controls.append(number); }
    }
    row.append(info, controls); container.append(row);
  }
  if (!count) container.textContent = 'Здесь пока пусто. Выберите книгу в каталоге.';
  document.querySelector('#cart-count').textContent = count;
  document.querySelector('#cart-total').textContent = money(total);
  document.querySelector('#checkout').disabled = !count || busy;
}
document.querySelectorAll('.add').forEach(button => button.onclick = () => {
  if (busy) return;
  const id = button.dataset.id;
  if ((cart[id] || 0) >= 10) return notify('Не более 10 экземпляров одной книги');
  cart[id] = (cart[id] || 0) + 1;
  persist(); renderCart(); notify('Книга добавлена на вашу полку');
});
document.querySelector('#open-cart').onclick = () => { renderCart(); dialog.showModal(); };
document.querySelector('#close-cart').onclick = () => dialog.close();
document.querySelector('#checkout').onclick = async () => {
  if (busy) return;
  busy = true; renderCart();
  const error = document.querySelector('#cart-error'); error.textContent = '';
  try {
    const response = await fetch('/orders', {method: 'POST', headers: {'Content-Type': 'text/plain', 'X-Bookshop': 'bookshop'},
      body: Object.entries(cart).map(([id, quantity]) => `${id}=${quantity}`).join('&')});
    const result = await response.text();
    if (!response.ok) throw new Error(result);
    cart = {}; persist(); window.location.assign(result);
  } catch (exception) { error.textContent = exception.message || 'Нет связи с сервером. Корзина сохранена.'; }
  finally { busy = false; renderCart(); }
};
let category = 'Все';
/** Совмещает выбранную категорию и поисковую строку, не изменяя серверный каталог. */
function filterBooks() {
  const query = document.querySelector('#search').value.trim().toLocaleLowerCase('ru');
  let visible = 0;
  document.querySelectorAll('.card').forEach(card => {
    card.hidden = !(category === 'Все' || card.dataset.category === category) || !card.dataset.search.toLocaleLowerCase('ru').includes(query);
    if (!card.hidden) visible++;
  });
  document.querySelector('#empty-search').hidden = visible !== 0;
}
document.querySelector('#search').addEventListener('input', filterBooks);
document.querySelectorAll('[data-filter]').forEach(button => button.onclick = () => {
  category = button.dataset.filter;
  document.querySelectorAll('[data-filter]').forEach(item => {
    const active = item === button; item.classList.toggle('active', active); item.setAttribute('aria-pressed', active);
  });
  filterBooks();
});
renderCart();
