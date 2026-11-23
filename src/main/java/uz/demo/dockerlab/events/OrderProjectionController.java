
package uz.demo.dockerlab.events;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/event-lab/order-projections")
public class OrderProjectionController {

    private final AsyncOrderProjectionListener projections;

    private final EventLabOrderService orders;

    public OrderProjectionController(
            AsyncOrderProjectionListener projections,
            EventLabOrderService orders
    ) {
        this.projections = projections;
        this.orders = orders;
    }

    @GetMapping("/{id}")
    public AsyncOrderProjectionListener.ProjectionView find(
            @PathVariable long id
    ) {

        // Avval asosiy order mavjudligini tekshiramiz
        orders.findOrder(id);

        return projections.find(id);
    }
}
