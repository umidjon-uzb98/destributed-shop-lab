
package uz.demo.dockerlab.product;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;
    private final ProductQueryService queryService;

    public ProductController(
            ProductService service,
            ProductQueryService queryService
    ) {
        this.service = service;
        this.queryService = queryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductView create(
            @RequestBody ProductRequest request
    ) {
        Product product = service.create(request.name());

        return new ProductView(
                product.getId(),
                product.getName()
        );
    }

    @GetMapping
    public List<ProductView> findAll() {
        return queryService.findAll().items();
    }

    @GetMapping("/{id}")
    public ProductView findById(
            @PathVariable Long id
    ) {
        return queryService.findById(id);
    }

    @PutMapping("/{id}")
    public ProductView update(
            @PathVariable Long id,
            @RequestBody ProductRequest request
    ) {
        return service.update(id, request.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }



    public record ProductRequest(String name) {}


}
