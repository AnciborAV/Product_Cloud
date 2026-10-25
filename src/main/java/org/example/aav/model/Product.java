package org.example.aav.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

/** Неизменяемый товар; денежные значения хранятся без ошибок double. */
public record Product(UUID id, String name, BigDecimal price, Category category, String unit) {
    /** Иллюстрации начального каталога; для остальных товаров — нейтральная корзина. */
    public String getIllustrationIcon() {
        return switch (name) {
            case "Авокадо Хасс" -> "🥑";
            case "Томаты на ветке" -> "🍅";
            case "Молоко фермерское 3,2%" -> "🥛";
            case "Круассан сливочный" -> "🥐";
            case "Яблоки Гала" -> "🍎";
            case "Йогурт натуральный" -> "🥣";
            case "Хлеб на закваске" -> "🍞";
            case "Мёд цветочный" -> "🍯";
            default -> "🛒";
        };
    }

    /** Local photographs matched explicitly to each seeded product. */
    public String getImageUrl() {
        return switch (name) {
            case "Авокадо Хасс" -> "/images/products/avocado.jpg";
            case "Яблоки Гала" -> "/images/products/gala_apple.jpg";
            case "Бананы" -> "/images/products/banana.jpg";
            case "Апельсины" -> "/images/products/orange_fruit.jpg";
            case "Груши Конференция" -> "/images/products/conference_pear.jpg";
            case "Томаты на ветке" -> "/images/products/tomato.jpg";
            case "Огурцы" -> "/images/products/cucumber.jpg";
            case "Морковь" -> "/images/products/carrot.jpg";
            case "Картофель" -> "/images/products/potato.jpg";
            case "Брокколи" -> "/images/products/broccoli.jpg";
            case "Молоко фермерское 3,2%" -> "/images/products/milk.jpg";
            case "Йогурт натуральный" -> "/images/products/yogurt.jpg";
            case "Кефир" -> "/images/products/kefir.jpg";
            case "Творог" -> "/images/products/quark_dairy_product.jpg";
            case "Сыр Гауда" -> "/images/products/gouda_cheese.jpg";
            case "Круассан сливочный" -> "/images/products/croissant.jpg";
            case "Хлеб на закваске" -> "/images/products/sourdough.jpg";
            case "Багет французский" -> "/images/products/baguette.jpg";
            case "Бублик с кунжутом" -> "/images/products/bagel.jpg";
            case "Маффин шоколадный" -> "/images/products/chocolate_muffin.jpg";
            case "Сок апельсиновый" -> "/images/products/orange_juice.jpg";
            case "Сок яблочный" -> "/images/products/apple_juice.jpg";
            case "Сок томатный" -> "/images/products/tomato_juice.jpg";
            case "Сок гранатовый" -> "/images/products/pomegranate_juice.jpg";
            case "Сок морковный" -> "/images/products/carrot_juice.jpg";
            case "Вода питьевая" -> "/images/products/bottled_water.jpg";
            case "Вода газированная" -> "/images/products/carbonated_water.jpg";
            case "Вода минеральная" -> "/images/products/mineral_water.jpg";
            case "Вода Боржоми" -> "/images/products/borjomi_water.jpg";
            case "Вода Эвиан" -> "/images/products/evian.jpg";
            case "Мёд цветочный" -> "/images/products/honey.jpg";
            case "Рис белый" -> "/images/products/white_rice.jpg";
            case "Гречневая крупа" -> "/images/products/buckwheat.jpg";
            case "Спагетти" -> "/images/products/spaghetti.jpg";
            case "Масло оливковое" -> "/images/products/olive_oil.jpg";
            case "Курица запечённая" -> "/images/products/chicken_as_food.jpg";
            case "Говядина" -> "/images/products/beef.jpg";
            case "Свинина" -> "/images/products/pork.jpg";
            case "Индейка запечённая" -> "/images/products/turkey_as_food.jpg";
            case "Бекон" -> "/images/products/bacon.jpg";
            case "Лосось, филе" -> "/images/products/salmon_as_food.jpg";
            case "Форель запечённая" -> "/images/products/trout.jpg";
            case "Треска сушёная" -> "/images/products/cod_as_food.jpg";
            case "Креветки" -> "/images/products/shrimp_and_prawn_as_food.jpg";
            case "Мидии" -> "/images/products/mussel.jpg";
            case "Шоколад тёмный" -> "/images/products/dark_chocolate.jpg";
            case "Печенье овсяное" -> "/images/products/oatmeal_raisin_cookie.jpg";
            case "Мармелад" -> "/images/products/gummi_candy.jpg";
            case "Мороженое ванильное" -> "/images/products/vanilla_ice_cream.jpg";
            case "Миндаль" -> "/images/products/almond.jpg";
            default -> null;
        };
    }

    public Product {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(unit, "unit");
        name = name.strip();
        unit = unit.strip();
        if (name.isBlank() || name.length() > 100 || unit.isBlank() || unit.length() > 30) {
            throw new IllegalArgumentException("Некорректное название или единица продажи");
        }
        if (price.signum() <= 0 || price.compareTo(new BigDecimal("999999.99")) > 0) {
            throw new IllegalArgumentException("Цена должна быть от 0.01 до 999999.99");
        }
        price = price.setScale(2, RoundingMode.UNNECESSARY);
    }
}
