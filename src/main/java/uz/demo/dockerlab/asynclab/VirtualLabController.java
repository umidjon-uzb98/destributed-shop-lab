
package uz.demo.dockerlab.asynclab;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
public class VirtualLabController {

    private final VirtualLabService service;

    public VirtualLabController(
            VirtualLabService service
    ) {
        this.service = service;
    }

    @GetMapping("/api/async-lab/virtual")
    public CompletableFuture<VirtualLabService.VirtualResult>
    execute() {

        return service.execute();
    }
}
