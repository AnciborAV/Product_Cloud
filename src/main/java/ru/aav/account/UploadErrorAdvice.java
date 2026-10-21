package ru.aav.account;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Превышение лимита загрузки возвращает понятное сообщение, а не страницу с исключением.
 * {@code @ControllerAdvice} применяет общую подготовку модели или обработку исключений к
 * контроллерам.
 */
@ControllerAdvice
public class UploadErrorAdvice {
    /**
     * Перехватывает превышение лимита multipart-запроса и возвращает пользователя к форме с
     * понятным сообщением. {@code @ExceptionHandler} перехватывает указанный тип исключения и
     * выбирает ответ.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooLarge(HttpServletRequest request, RedirectAttributes redirect) {
        redirect.addFlashAttribute("formError", "Фото должно быть не больше 2 МБ");
        return request.getRequestURI().endsWith("/register")
                ? "redirect:/register"
                : "redirect:/account#photo";
    }
}
