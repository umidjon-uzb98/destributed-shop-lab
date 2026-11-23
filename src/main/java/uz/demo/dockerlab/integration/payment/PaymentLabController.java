
package uz.demo.dockerlab.integration.payment;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integration-lab/payments")
public class PaymentLabController {

    private final PaymentClient paymentClient;

    public PaymentLabController(
            PaymentClient paymentClient
    ) {
        this.paymentClient = paymentClient;
    }

    @GetMapping("/{id}")
    public PaymentClient.PaymentResult findPayment(
            @PathVariable String id
    ) {

        return paymentClient.findPayment(id);
    }
}
