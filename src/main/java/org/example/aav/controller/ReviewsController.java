package org.example.aav.controller;

import org.example.aav.repository.ReviewRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ReviewsController {
  private final ReviewRepository reviews;

  public ReviewsController(ReviewRepository reviews) {
    this.reviews = reviews;
  }

  @GetMapping("/reviews")
  public String reviews(@RequestParam(defaultValue = "0") int page, Model model) {
    model.addAttribute(
        "reviewPage", reviews.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(0, page), 12)));
    return "reviews";
  }
}
