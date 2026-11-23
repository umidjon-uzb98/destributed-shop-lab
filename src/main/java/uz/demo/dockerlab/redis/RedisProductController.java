package uz.demo.dockerlab.redis;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/redis-lab/products")
public class RedisProductController {

    private final RedisProductService service;

    public RedisProductController(
            RedisProductService service
    ) {
        this.service = service;
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(
            @PathVariable Long id,
            @RequestBody RedisProductService.ProductData product
    ) {
        service.save(id, product);
    }

    @GetMapping("/{id}")
    public RedisProductService.ProductData findById(
            @PathVariable Long id
    ) {
        var product = service.findById(id);

        if (product == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product not found in Redis"
            );
        }

        return product;
    }
}

