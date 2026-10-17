package ru.aav.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.aav.model.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {
}
