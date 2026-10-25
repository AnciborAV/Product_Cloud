package org.example.aav.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Этап 7: перехватываем слишком большие загрузки, чтобы показать понятную ошибку. Учебная последовательность. */
@ControllerAdvice
public class UploadExceptionHandler {
  /**
   * Показывает понятное сообщение, если загруженный файл превышает допустимый размер.
   * @param flash сообщения, доступные после перенаправления браузера
   */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public String uploadTooLarge(RedirectAttributes flash) {
    flash.addFlashAttribute(
        "error", "Файл слишком большой. Выберите JPEG или PNG размером до 2 МБ.");
    return "redirect:/account";
  }
}
