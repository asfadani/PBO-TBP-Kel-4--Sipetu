package com.sipetu.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic Repository interface (Abstraction for data persistence).
 */
public interface Repository<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    boolean deleteById(ID id);
}
