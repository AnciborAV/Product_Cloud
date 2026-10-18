'use strict';
const form = document.querySelector('#auth-form');
form.addEventListener('submit', async event => {
  event.preventDefault();
  const button = form.querySelector('button');
  if (button.disabled) return;
  button.disabled = true;
  const error = document.querySelector('#form-error'); error.textContent = '';
  try {
    const response = await fetch(form.dataset.action, {method: 'POST', headers: {'X-Bookshop': 'bookshop'}, body: new URLSearchParams(new FormData(form))});
    const result = await response.text();
    if (!response.ok) throw new Error(result);
    window.location.assign(result);
  } catch (exception) { error.textContent = exception.message || 'Нет связи с сервером.'; }
  finally { button.disabled = false; }
});
