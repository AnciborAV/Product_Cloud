package org.example.aav.service;

import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Validates image headers before decoding and stores only a re-encoded thumbnail. */
@Component
public class AvatarProcessor {
  public byte[] process(MultipartFile file) {
    if (file.isEmpty() || file.getSize() > 2 * 1024 * 1024)
      throw new IllegalArgumentException("Выберите JPEG или PNG размером до 2 МБ");
    try (var input = ImageIO.createImageInputStream(file.getInputStream())) {
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext())
        throw new IllegalArgumentException("Файл не является изображением JPEG или PNG");
      var reader = readers.next();
      try {
        String format = reader.getFormatName();
        if (!format.equalsIgnoreCase("JPEG") && !format.equalsIgnoreCase("PNG"))
          throw new IllegalArgumentException("Разрешены только JPEG и PNG");
        reader.setInput(input);
        int width = reader.getWidth(0), height = reader.getHeight(0);
        if (width < 1
            || height < 1
            || width > 6000
            || height > 6000
            || (long) width * height > 16000000)
          throw new IllegalArgumentException(
              "Фото должно содержать не более 16 мегапикселей, сторона — до 6000 пикселей");
        BufferedImage original = reader.read(0);
        int side = Math.min(width, height);
        BufferedImage avatar = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
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
        var output = new ByteArrayOutputStream();
        ImageIO.write(avatar, "jpeg", output);
        return output.toByteArray();
      } finally {
        reader.dispose();
      }
    } catch (IOException ex) {
      throw new IllegalArgumentException("Не удалось прочитать фото. Загрузите другой файл");
    }
  }
}
