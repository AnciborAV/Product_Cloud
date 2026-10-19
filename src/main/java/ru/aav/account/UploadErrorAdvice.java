package ru.aav.account;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;

/** Превышение лимита загрузки возвращает понятное сообщение, а не страницу с исключением. */
@ControllerAdvice
public class UploadErrorAdvice {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooLarge(HttpServletRequest request, RedirectAttributes redirect) {
        redirect.addFlashAttribute("formError", "Фото должно быть не больше 2 МБ");
        return request.getRequestURI().endsWith("/register") ? "redirect:/register" : "redirect:/account#photo";
    }
}
