package uz.demo.dockerlab.product;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public ProductService(
            ProductRepository repository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Product create(String name) {
        Product product = repository.save(new Product(name));

        eventPublisher.publishEvent(
                new ProductChangedEvent(product.getId())
        );

        return product;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return repository.findAll();
    }

    @Transactional
    public ProductView update(Long id, String name) {
        Product product = findProduct(id);
        product.rename(name);

        eventPublisher.publishEvent(
                new ProductChangedEvent(id)
        );

        return new ProductView(
                product.getId(),
                product.getName()
        );
    }

    @Transactional
    public void delete(Long id) {
        Product product = findProduct(id);
        repository.delete(product);

        eventPublisher.publishEvent(
                new ProductChangedEvent(id)
        );
    }

    private Product findProduct(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found"
                        )
                );
    }
}
