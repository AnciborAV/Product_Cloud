package ru.aav.account;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;

/** Проверяет реальное содержимое JPEG/PNG, ограничивает размеры и удаляет метаданные фото. */
@Service
public class AvatarService {
    public byte[] normalize(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > 2 * 1024 * 1024) throw new IllegalArgumentException("Фото должно быть не больше 2 МБ");
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Выберите изображение JPEG или PNG");
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("JPEG") && !format.equalsIgnoreCase("PNG"))
                    throw new IllegalArgumentException("Поддерживаются только JPEG и PNG");
                reader.setInput(input);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 4096 || height > 4096)
                    throw new IllegalArgumentException("Размер фото — не более 4096 × 4096 пикселей");
                BufferedImage source = reader.read(0);
                int side = Math.min(width, height);
                BufferedImage output = new BufferedImage(384, 384, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = output.createGraphics();
                try {
                    g.setColor(Color.WHITE); g.fillRect(0, 0, 384, 384);
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    int x = (width - side) / 2, y = (height - side) / 2;
                    g.drawImage(source, 0, 0, 384, 384, x, y, x + side, y + side, null);
                } finally { g.dispose(); }
                var bytes = new ByteArrayOutputStream();
                ImageIO.write(output, "JPEG", bytes);
                return bytes.toByteArray();
            } finally { reader.dispose(); }
        } catch (IOException ex) { throw new IllegalArgumentException("Не удалось прочитать фото. Выберите другой JPEG или PNG"); }
    }
}
