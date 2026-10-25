package org.example.aav.controller;

import org.example.aav.repository.ReviewRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** Этап 7: показываем посетителям отзывы с разделением на страницы. Учебная последовательность. */
@Controller
public class ReviewsController {
  // Отзывы пользователя или доступ к ним в базе.
  private final ReviewRepository reviews;

  /**
   * Создаёт объект и получает необходимые зависимости через конструктор; Spring передаёт их при создании.
   * @param reviews отзывы пользователя или доступ к ним в базе
   */
  public ReviewsController(ReviewRepository reviews) {
    this.reviews = reviews;
  }

  /**
   * Загружает отзывы для пользователя или страницы отзывов.
   * @param page номер страницы, начиная с нуля
   * @param model данные, которые Spring передаст HTML-шаблону
   */
  @GetMapping("/reviews")
  public String reviews(@RequestParam(defaultValue = "0") int page, Model model) {
    model.addAttribute(
        "reviewPage", reviews.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(0, page), 12)));
    return "reviews";
  }
}
