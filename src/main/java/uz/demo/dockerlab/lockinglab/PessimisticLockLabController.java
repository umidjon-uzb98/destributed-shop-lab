
package uz.demo.dockerlab.lockinglab;

import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/locking-lab/pessimistic")
@ConditionalOnProperty(
        prefix = "lab.locking",
        name = "enabled",
        havingValue = "true"
)
public class PessimisticLockLabController {

    private final PessimisticLockLabService labService;

    public PessimisticLockLabController(
            PessimisticLockLabService labService
    ) {
        this.labService = labService;
    }

    @PostMapping("/race")
    public PessimisticLockLabService.RaceResult race() {

        return labService.runRace(false);
    }

    @PostMapping("/timeout")
    public PessimisticLockLabService.RaceResult timeout() {

        return labService.runRace(true);
    }
}
