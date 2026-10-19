# Product Cloud — продуктовый магазин

Главная страница `/products`: продуктовый баннер, шесть иллюстрированных категорий,
блок о повседневном меню и существующий серверный каталог с формой добавления.
Категории — визуальное представление ассортимента, а не фильтры базы данных.
Фотографии иллюстрируют продуктовые группы; они не привязаны к отдельным товарам.
Наличие товара и его цена берутся из существующего каталога.

Ориентиры: https://vkusvill.ru/ и https://www.wholefoodsmarket.com/ —
понятная группировка продуктов, заметная фотография еды, спокойная зелёная палитра.
Макеты, логотипы и фотографии сетей не копировались.

## Изображения

Созданы встроенным ImageGen. Файлы в `src/main/resources/static/images/`:
`grocery-hero.webp`, `vegetables.webp`, `fruit.webp`, `dairy.webp`,
`bakery.webp`, `fish.webp`, `pantry.webp`.
Все изображения локальны, внешние CDN и шрифты не требуются.
Для категорий исходный лист 3×2 разделён на шесть квадратных фотографий.

Промпт главного баннера:

> Use case: product-mockup. Asset type: grocery store website hero photograph.
> Create a beautiful premium editorial food photograph, landscape 3:2.
> A generous arrangement of fresh groceries on a pale warm sage green seamless
> studio backdrop: ripe tomatoes on vine, orange carrots with tops, avocado
> halves, lemons, broccoli, leafy lettuce, rustic sourdough loaf, glass bottle
> of milk, brown eggs in paper carton. Grocery still life, natural delicious
> textures, soft daylight from upper left, subtle grounded shadows,
> sophisticated organic supermarket campaign, restrained styling, rich true
> colors. All items centered with breathing room, no people, no lettering,
> no logos, no watermark. Save image for use as a local website asset.

Промпт категорий:

> Use case: product-mockup. Grocery website category photography asset sheet.
> A precise 3 columns by 2 rows grid, six equally sized square photographs,
> no gaps, no borders, no text. Each tile studio food still life on identical
> pale cream background, soft sunlight, appetizing realistic premium photography.
> Top left: red tomatoes on vine, broccoli and carrots. Top middle: oranges,
> green pear, red apple and sliced lemon. Top right: glass milk bottle, brown
> eggs, chunk of cheese. Bottom left: rustic sourdough bread and croissant.
> Bottom middle: raw salmon fillet with lemon and herbs on ceramic plate.
> Bottom right: pantry items olive oil glass bottle, pasta in glass jar and
> bag of rice. Every subject contained within its tile with generous safe
> margin, no overlap between tiles, no logos. Overall landscape 3:2 aspect ratio.

## Проверка

Проверены локальные пути всех изображений, уникальность HTML id, цели ссылок,
alt-тексты и отсутствие книжной тематики. `git diff --check` проходит.
Изображения просмотрены перед включением. Адаптивная сетка: шесть, три или две
колонки категорий; на телефоне баннер и форма располагаются вертикально.

`./mvnw clean verify` не завершён: Maven Central недоступен (ошибка DNS).
В среде JDK 17, проект требует JDK 25. Chromium отсутствует; попытка установить
браузер завершилась ошибкой загрузки. Поэтому фактический рендер страницы на
компьютере и телефоне здесь не проверен. Нужны запуск и проверка в браузере
на машине с JDK 25 и доступом к зависимостям.
