package org.example.aav.service;

import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Этап 5: проверяем и уменьшаем фотографии перед сохранением, чтобы хранить только подходящие изображения. Учебная последовательность. */
@Component
public class AvatarProcessor {
  /**
   * Проверяет JPEG или PNG и превращает его в квадратную фотографию JPEG 400 × 400.
   * @param file загруженный пользователем файл
   */
  public byte[] process(MultipartFile file) {
    if (file.isEmpty() || file.getSize() > 2 * 1024 * 1024)
      throw new IllegalArgumentException("Выберите JPEG или PNG размером до 2 МБ");
    // Поток чтения изображения; закроется после блока try.
    try (var input = ImageIO.createImageInputStream(file.getInputStream())) {
      // Доступные средства чтения формата изображения.
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext())
        throw new IllegalArgumentException("Файл не является изображением JPEG или PNG");
      // Средство чтения выбранного изображения.
      var reader = readers.next();
      try {
        // Фактический формат изображения.
        String format = reader.getFormatName();
        if (!format.equalsIgnoreCase("JPEG") && !format.equalsIgnoreCase("PNG"))
          throw new IllegalArgumentException("Разрешены только JPEG и PNG");
        reader.setInput(input);
        // Ширина изображения в пикселях.
        // Height — высота исходного изображения в пикселях.
        int width = reader.getWidth(0), height = reader.getHeight(0);
        if (width < 1
            || height < 1
            || width > 6000
            || height > 6000
            || (long) width * height > 16000000)
          throw new IllegalArgumentException(
              "Фото должно содержать не более 16 мегапикселей, сторона — до 6000 пикселей");
        // Исходное изображение до уменьшения.
        BufferedImage original = reader.read(0);
        // Сторона квадрата для обрезки фотографии по центру.
        int side = Math.min(width, height);
        // Новое квадратное изображение: сюда рисуем уменьшенную фотографию.
        BufferedImage avatar = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        // Инструмент рисования уменьшенной фотографии.
        var graphics = avatar.createGraphics();
        try {
          graphics.setColor(java.awt.Color.WHITE);
          graphics.fillRect(0, 0, 400, 400);
          graphics.setRenderingHint(
              java.awt.RenderingHints.KEY_INTERPOLATION,
              java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
          graphics.drawImage(
              original,
              0,
              0,
              400,
              400,
              (width - side) / 2,
              (height - side) / 2,
              (width + side) / 2,
              (height + side) / 2,
              null);
        } finally {
          graphics.dispose();
        }
        // Поток, в котором собираются байты изображения.
        var output = new ByteArrayOutputStream();
        ImageIO.write(avatar, "jpeg", output);
        return output.toByteArray();
      } finally {
        reader.dispose();
      }
    // ex — ошибка, которую перехватываем, чтобы обработать её здесь.
    } catch (IOException ex) {
      throw new IllegalArgumentException("Не удалось прочитать фото. Загрузите другой файл");
    }
  }
}
