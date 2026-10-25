(() => {
  const form = document.getElementById('selection-form');
  if (!form) return;
  const all = document.getElementById('select-all-products');
  const items = [...document.querySelectorAll('.product-checkbox')];
  const button = form.querySelector('button');
  const count = document.getElementById('selection-count');
  const update = () => {
    const selected = items.filter(item => item.checked).length;
    all.checked = items.length > 0 && selected === items.length;
    all.indeterminate = selected > 0 && selected < items.length;
    button.disabled = selected === 0;
    count.textContent = `Выбрано: ${selected} из ${items.length}`;
    items.forEach(item => item.closest('.saved-row, .product-card').classList.toggle('is-selected', item.checked));
  };
  all.addEventListener('change', () => {
    items.forEach(item => { item.checked = all.checked; });
    update();
  });
  items.forEach(item => item.addEventListener('change', update));
  form.addEventListener('submit', event => {
    if (!items.some(item => item.checked)) event.preventDefault();
  });
  window.addEventListener('pageshow', update);
  update();
})();
