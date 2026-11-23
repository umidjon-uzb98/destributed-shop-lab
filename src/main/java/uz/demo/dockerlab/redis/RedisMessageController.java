package uz.demo.dockerlab.redis;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/redis-lab/messages")
public class RedisMessageController {

    private final RedisMessageService service;

    public RedisMessageController(
            RedisMessageService service
    ) {
        this.service = service;
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(
            @PathVariable Long id,
            @RequestBody String message
    ) {
        service.save(id, message);
    }

    @GetMapping("/{id}")
    public String findById(@PathVariable Long id) {
        String value = service.findById(id);

        if (value == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Redis key not found"
            );
        }

        return value;
    }

    @GetMapping("/{id}/ttl")
    public Long ttl(@PathVariable Long id) {
        return service.ttl(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean deleted = service.delete(id);

        return deleted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}

