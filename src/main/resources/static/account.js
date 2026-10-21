// Предпросмотр фото выполняется только в браузере; окончательные проверки делает AvatarService.

/** Подключает предпросмотр к одному полю выбора файла и освобождает временные URL. */
function initializeAvatarPreview(input) {
    // Картинка находится рядом с input внутри блока загрузки фотографии.
    const preview = input.parentElement.querySelector('[data-avatar-preview]');
    if (!preview) return;

    // Object URL удерживает файл в памяти, поэтому прежний адрес нужно отзывать.
    let objectUrl;

    /** Освобождает ссылку на предыдущий файл; вызывать повторно безопасно. */
    function releasePreview() {
        if (objectUrl) URL.revokeObjectURL(objectUrl);
        objectUrl = undefined;
    }

    /** Проверяет удобные для пользователя ограничения и показывает выбранное фото. */
    function updatePreview() {
        releasePreview();
        preview.hidden = true;
        preview.removeAttribute('src');
        input.setCustomValidity('');

        // Файл ещё не отправлен на сервер: это локальный объект File.
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
        objectUrl = URL.createObjectURL(file);
        preview.src = objectUrl;
        preview.hidden = false;
    }

    input.addEventListener('change', updatePreview);
    // pageshow восстанавливает предпросмотр, если страница вернулась из кеша навигации.
    window.addEventListener('pagehide', releasePreview);
    window.addEventListener('pageshow', updatePreview);
}

// На странице без загрузки фото набор пуст: дополнительных действий не требуется.
document.querySelectorAll('[data-avatar-input]').forEach(initializeAvatarPreview);
