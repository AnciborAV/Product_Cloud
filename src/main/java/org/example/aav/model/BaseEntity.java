package org.example.aav.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Этап 2: выносим общие поля записей в базовый класс, чтобы не повторять номер и даты. Учебная последовательность. */
@MappedSuperclass
public abstract class BaseEntity {
  // Уникальный номер записи.
  @Id protected UUID id = UUID.randomUUID();

  // Время создания записи.
  @Column(nullable = false, updatable = false)
  protected Instant createdAt = Instant.now();

  // Время последнего изменения.
  @Column(nullable = false)
  protected Instant updatedAt = Instant.now();

  // Номер версии: помогает заметить одновременное изменение записи.
  @Version protected long version;

  /**
   * Обновляет время изменения перед сохранением записи в базе.
   */
  @PreUpdate
  protected void touch() {
    updatedAt = Instant.now();
  }

  /**
   * Возвращает уникальный номер записи.
   */
  public UUID getId() {
    return id;
  }

  /**
   * Возвращает время создания записи.
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Возвращает время последнего изменения.
   */
  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
