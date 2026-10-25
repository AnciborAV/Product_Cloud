package org.example.aav.model;

public enum Category {
    VEGETABLES("Овощи и фрукты", "🥑"), DAIRY("Молочные продукты", "🥛"),
    BAKERY("Хлеб и выпечка", "🥐"), PANTRY("Бакалея", "🍯");

    private final String title;
    private final String icon;

    Category(String title, String icon) {
        this.title = title;
        this.icon = icon;
    }

    public String getTitle() { return title; }
    public String getIcon() { return icon; }
}
