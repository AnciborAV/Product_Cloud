package ru.aav.account;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

/**
 * Проверяет реальное содержимое JPEG/PNG, ограничивает размеры и удаляет метаданные фото.
 * {@code @Service} регистрирует сервис как компонент Spring.
 */
@Service
public class AvatarService {
    /**
     * Проверяет размер и реальный формат файла, обрезает фото по центру, уменьшает до 384 × 384 и
     * перекодирует в JPEG без исходных метаданных.
     */
    public byte[] normalize(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > 2 * 1024 * 1024)
            throw new IllegalArgumentException("Фото должно быть не больше 2 МБ");
        try (var input =
                ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))) {
            // Подходящие декодировщики по реальному содержимому файла, а не по расширению имени.
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext())
                throw new IllegalArgumentException("Выберите изображение JPEG или PNG");
            // Декодировщик обнаруженного формата изображения; освобождается через dispose.
            ImageReader reader = readers.next();
            try {
                // Формат изображения либо форматтер представления цены для текущей операции.
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("JPEG") && !format.equalsIgnoreCase("PNG"))
                    throw new IllegalArgumentException("Поддерживаются только JPEG и PNG");
                reader.setInput(input);
                // Ширина исходного изображения до декодирования полного растра. Высота исходного
                // изображения; вместе с шириной ограничивает объём памяти.
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 4096 || height > 4096)
                    throw new IllegalArgumentException(
                            "Размер фото — не более 4096 × 4096 пикселей");
                // Декодированный исходный растр после проверки допустимых размеров.
                BufferedImage source = reader.read(0);
                // Сторона центрального квадрата, который будет вырезан из фото.
                int side = Math.min(width, height);
                // Новое RGB-изображение фиксированного размера, без метаданных исходного файла.
                BufferedImage output = new BufferedImage(384, 384, BufferedImage.TYPE_INT_RGB);
                // Графический контекст для масштабирования фото; dispose освобождает его ресурсы.
                Graphics2D graphics = output.createGraphics();
                try {
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, 384, 384);
                    graphics.setRenderingHint(
                            RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    // Левая граница центрального квадрата исходного фото. Верхняя граница
                    // центрального квадрата исходного фото.
                    int x = (width - side) / 2, y = (height - side) / 2;
                    graphics.drawImage(source, 0, 0, 384, 384, x, y, x + side, y + side, null);
                } finally {
                    graphics.dispose();
                }
                // Буфер байтов текущей операции; после подготовки преобразуется в ответ или
                // сохраняемое значение.
                var bytes = new ByteArrayOutputStream();
                if (!ImageIO.write(output, "JPEG", bytes))
                    throw new IOException("JPEG encoder is unavailable");
                return bytes.toByteArray();
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException(
                    "Не удалось прочитать фото. Выберите другой JPEG или PNG");
        }
    }
}
