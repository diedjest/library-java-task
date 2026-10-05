package ru.vsu.cs.uvarov_d_p.entity;

import java.time.LocalDateTime;
import java.util.Objects;

public abstract class AbstractEntity<ID> {
    private final ID id;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected AbstractEntity(ID id) {
        this(id, LocalDateTime.now(), LocalDateTime.now());
    }

    protected AbstractEntity(ID id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "Идентификатор не может быть null");
        this.createdAt = Objects.requireNonNull(createdAt, "Дата создания не может быть null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Дата обновления не может быть null");
    }

    public ID getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    protected void markAsUpdated() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractEntity<?> that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
