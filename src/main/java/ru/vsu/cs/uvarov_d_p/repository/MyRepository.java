package ru.vsu.cs.uvarov_d_p.repository;

import ru.vsu.cs.uvarov_d_p.entity.AbstractEntity;

import java.util.List;
import java.util.Optional;

public interface MyRepository<E extends AbstractEntity<ID>, ID> {

    void save(E entity);

    void delete(E entity);

    void deleteById(ID id);

    Optional<E> findById(ID id);

    List<E> findAll();
}