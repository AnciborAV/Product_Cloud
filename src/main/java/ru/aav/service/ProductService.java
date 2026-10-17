package ru.aav.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aav.model.Product;
import ru.aav.model.ProductView;
import ru.aav.repository.ProductRepository;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private static final int PAGE_SIZE = 20;
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public Page<ProductView> getProducts(int page) {
        return repository.findAll(PageRequest.of(page, PAGE_SIZE, Sort.by("id")))
                .map(ProductView::from);
    }

    public ProductView getProductById(int id) {
        return ProductView.from(requireProduct(id));
    }

    @Transactional
    public ProductView createProduct(String name, BigDecimal price) {
        return ProductView.from(repository.save(new Product(name, price)));
    }

    @Transactional
    public ProductView updateProduct(int id, String name, BigDecimal price) {
        var product = requireProduct(id);
        product.update(name, price);
        return ProductView.from(product);
    }

    @Transactional
    public void deleteProduct(int id) {
        repository.delete(requireProduct(id));
    }

    private Product requireProduct(int id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
