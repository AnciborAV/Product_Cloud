// Предпросмотр фото работает локально: файл отправляется только при отправке формы.
document.querySelectorAll('[data-avatar-input]').forEach(input => {
    let previous;
    input.addEventListener('change', () => {
        const preview = input.parentElement.querySelector('[data-avatar-preview]');
        if (previous) URL.revokeObjectURL(previous);
        preview.hidden = true;
        preview.removeAttribute('src');
        input.setCustomValidity('');
        const file = input.files[0];
        if (!file) return;
        if (file.size > 2 * 1024 * 1024) {
            input.setCustomValidity('Выберите фото не больше 2 МБ');
            input.reportValidity();
            return;
        }
        if (!['image/jpeg', 'image/png'].includes(file.type)) {
            input.setCustomValidity('Выберите фотографию JPEG или PNG');
            input.reportValidity();
            return;
        }
        previous = URL.createObjectURL(file);
        preview.src = previous;
        preview.hidden = false;
    });
});
