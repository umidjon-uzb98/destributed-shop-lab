package uz.demo.dockerlab;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private final String message;

    public HelloController(
            @Value("${app.message}") String message
    ) {
        this.message = message;
    }

    @GetMapping("/api/hello")
    public String hello() {
        return message;
    }
}