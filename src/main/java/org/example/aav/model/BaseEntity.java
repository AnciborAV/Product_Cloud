package org.example.aav.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {
  @Id protected UUID id = UUID.randomUUID();

  @Column(nullable = false, updatable = false)
  protected Instant createdAt = Instant.now();

  @Column(nullable = false)
  protected Instant updatedAt = Instant.now();

  @Version protected long version;

  @PreUpdate
  protected void touch() {
    updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
