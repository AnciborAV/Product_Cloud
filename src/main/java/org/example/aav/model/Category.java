package org.example.aav.model;

public enum Category {

    VEGETABLES("Овощи", "🥕"),
    FRUITS("Фрукты", "🍎"),
    DAIRY("Молочные продукты", "🥛"),

    BAKERY("Хлеб и выпечка", "🥐"),
    PANTRY("Бакалея", "🍯"),

    JUICES("Соки", "🧃"),
    WATER("Вода", "💧"),
    MEAT("Мясо и птица", "🍗"),

    FISH("Рыба и морепродукты", "🐟"),
    SWEETS("Сладости и орехи", "🍫");

    private final String title;
    private final String icon;

    Category(String title, String icon) {
        this.title = title;
        this.icon = icon;
    }

    public String getTitle() { return title; }
    public String getIcon() { return icon; }
}
