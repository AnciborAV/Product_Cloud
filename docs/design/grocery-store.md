# Product Cloud — продуктовый магазин

Главная страница `/products` содержит продуктовый баннер и личный список,
который пользователь заполняет сам. Предустановленного ассортимента нет.
Форма создаёт личный продукт и сразу добавляет его в корзину. Название и цена
принадлежат записи пользователя, а изображение баннера служит оформлением.

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

## Текущее использование ресурсов

На страницах списка, входа и регистрации используется `grocery-hero.webp`.
Шесть фотографий категорий сохранены как материалы предыдущего варианта дизайна;
текущие шаблоны их не подключают. Неиспользуемое изображение дерева `1.png` удалено.

Текущее состояние проверки Java-кода, сборки и ограничений описано в
[обзоре проекта](../code-review.md). Старые замечания об отсутствии JDK 25
и незавершённой Maven-сборке относились к первоначальному этапу разработки.
