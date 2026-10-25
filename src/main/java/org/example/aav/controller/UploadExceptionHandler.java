package org.example.aav.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class UploadExceptionHandler {
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public String uploadTooLarge(RedirectAttributes flash) {
    flash.addFlashAttribute(
        "error", "Файл слишком большой. Выберите JPEG или PNG размером до 2 МБ.");
    return "redirect:/account";
  }
}
