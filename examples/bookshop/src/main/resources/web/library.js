'use strict';
// Содержимое PDF отправляем отдельным телом: не превращаем бинарный файл в текст или Base64.
async function post(url, body) {
  const response = await fetch(url, {method: 'POST', headers: {'X-Bookshop': 'bookshop'}, body});
  const text = await response.text();
  if (response.status === 401) { window.location.assign('/login'); throw new Error('Войдите снова.'); }
  if (!response.ok) throw new Error(text);
  return text;
}
const showUpload = () => document.querySelector('#upload-dialog').showModal();
document.querySelector('#open-upload').onclick = showUpload;
document.querySelector('#empty-upload')?.addEventListener('click', showUpload);
document.querySelector('#new-shelf').onclick = () => document.querySelector('#shelf-dialog').showModal();
document.querySelectorAll('[data-close]').forEach(button => button.onclick = () => document.getElementById(button.dataset.close).close());
document.querySelector('#logout').onclick = async () => {
  try { await post('/logout', ''); window.location.assign('/login'); }
  catch (error) { document.querySelector('#library-error').textContent = error.message; }
};
function submit(form, action) {
  form.addEventListener('submit', async event => {
    event.preventDefault(); const button = form.querySelector('button');
    if (button.disabled) return;
    button.disabled = true; form.querySelector('.form-error').textContent = '';
    try { await action(new FormData(form)); window.location.reload(); }
    catch (error) { form.querySelector('.form-error').textContent = error.message || 'Нет связи с сервером.'; }
    finally { button.disabled = false; }
  });
}
submit(document.querySelector('#shelf-form'), data => post('/api/shelves', new URLSearchParams(data)));
submit(document.querySelector('#upload-form'), async data => {
  const file = document.querySelector('#pdf-file').files[0];
  if (!file || file.size > 20 * 1024 * 1024) throw new Error('Выберите PDF размером до 20 МБ.');
  const params = new URLSearchParams({title: data.get('title'), author: data.get('author'), shelf: data.get('shelf'), filename: file.name});
  await post('/api/documents?' + params, file);
});
document.querySelector('#pdf-file').onchange = event => {
  const file = event.target.files[0], title = document.querySelector('[name="title"]');
  if (file && !title.value) title.value = file.name.replace(/\.pdf$/i, '').slice(0, 200);
};
document.querySelectorAll('.move-shelf').forEach(select => {
  let previous = select.value;
  select.onchange = async () => {
    select.disabled = true;
    try { await post('/api/move', new URLSearchParams({id: select.dataset.id, shelf: select.value})); window.location.reload(); }
    catch (error) { select.value = previous; document.querySelector('#library-error').textContent = error.message; }
    finally { select.disabled = false; }
  };
});
let shelf = 'all';
function filter() {
  const query = document.querySelector('#library-search').value.trim().toLocaleLowerCase('ru');
  let count = 0;
  document.querySelectorAll('.document-card').forEach(card => {
    card.hidden = (shelf !== 'all' && card.dataset.shelf !== shelf) || !card.dataset.search.toLocaleLowerCase('ru').includes(query);
    if (!card.hidden) count++;
  });
  document.querySelector('#no-results').hidden = count > 0 || !!document.querySelector('.library-empty');
}
document.querySelector('#library-search').addEventListener('input', filter);
document.querySelectorAll('.shelf-filter').forEach(button => button.onclick = () => {
  shelf = button.dataset.shelf;
  document.querySelectorAll('.shelf-filter').forEach(item => item.classList.toggle('active', item === button)); filter();
});
