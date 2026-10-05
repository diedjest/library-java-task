package ru.vsu.cs.uvarov_d_p.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Тестирование базовой сущности AbstractEntity")
class AbstractEntityTest {

    private static class TestEntity extends AbstractEntity<UUID> {

        TestEntity(UUID id) {
            super(id);
        }

        TestEntity(UUID id, LocalDateTime createdAt, LocalDateTime updatedAt) {
            super(id, createdAt, updatedAt);
        }

        void touch() {
            markAsUpdated();
        }
    }

    @Test
    @DisplayName("Конструктор с одним параметром выбрасывает NullPointerException при null id")
    void shouldThrowNullPointerExceptionWhenIdIsNull() {
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> new TestEntity(null)
        );
        assertEquals("Идентификатор не может быть null", ex.getMessage());
    }

    @Test
    @DisplayName("Полный конструктор выбрасывает NullPointerException при null createdAt")
    void shouldThrowNullPointerExceptionWhenCreatedAtIsNull() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> new TestEntity(id, null, now)
        );
        assertEquals("Дата создания не может быть null", ex.getMessage());
    }

    @Test
    @DisplayName("Полный конструктор выбрасывает NullPointerException при null updatedAt")
    void shouldThrowNullPointerExceptionWhenUpdatedAtIsNull() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> new TestEntity(id, now, null)
        );
        assertEquals("Дата обновления не может быть null", ex.getMessage());
    }

    @Test
    @DisplayName("markAsUpdated обновляет updatedAt и не затрагивает id и createdAt")
    void shouldUpdateOnlyUpdatedAtWhenMarkAsUpdatedCalled() {
        UUID id = UUID.randomUUID();
        LocalDateTime past = LocalDateTime.now().minusHours(1);
        TestEntity entity = new TestEntity(id, past, past);

        entity.touch();

        assertEquals(id, entity.getId());
        assertEquals(past, entity.getCreatedAt());
        assertTrue(entity.getUpdatedAt().isAfter(past), "markAsUpdated должен сдвинуть updatedAt вперёд");
    }

    @Test
    @DisplayName("equals и hashCode работают только на основе id")
    void shouldVerifyEqualsAndHashCodeBasedOnlyOnId() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        TestEntity entity1 = new TestEntity(id1);
        TestEntity entity1SameId = new TestEntity(id1, LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1));
        TestEntity entity2 = new TestEntity(id2);

        assertEquals(entity1, entity1SameId);
        assertEquals(entity1.hashCode(), entity1SameId.hashCode());
        assertNotEquals(entity1, entity2);
        assertNotEquals(null, entity1);
        assertNotEquals("строка", entity1);
    }
}