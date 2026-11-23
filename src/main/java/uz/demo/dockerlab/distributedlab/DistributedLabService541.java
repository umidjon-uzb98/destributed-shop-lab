
package uz.demo.dockerlab.distributedlab;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class DistributedLabService541 {

    private final CheckoutOrderRepository541 orderRepository;
    private final InventoryStockRepository541 stockRepository;

    public DistributedLabService541(
            CheckoutOrderRepository541 orderRepository,
            InventoryStockRepository541 stockRepository
    ) {
        this.orderRepository = orderRepository;
        this.stockRepository = stockRepository;
    }

    @Transactional
    public OrderView createOrder(long amountUzs) {

        if (amountUzs <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Amount must be positive"
            );
        }

        CheckoutOrder541 order = new CheckoutOrder541(
                UUID.randomUUID(),
                amountUzs
        );

        orderRepository.saveAndFlush(order);

        return new OrderView(
                order.getOrderId(),
                order.getAmountUzs(),
                order.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public OrderView getOrder(UUID orderId) {

        CheckoutOrder541 order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Order not found"
                                )
                        );

        return new OrderView(
                order.getOrderId(),
                order.getAmountUzs(),
                order.getStatus()
        );
    }

    @Transactional
    public InventoryView reserve(
            UUID orderId,
            Long productId
    ) {

        if (orderId == null || productId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "orderId and productId are required"
            );
        }

        InventoryStock541 stock =
                stockRepository.findForUpdate(productId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Product not found"
                                )
                        );

        if (stock.getAvailable() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "OUT_OF_STOCK"
            );
        }

        stock.reserveOne();

        stockRepository.saveAndFlush(stock);

        return new InventoryView(
                stock.getProductId(),
                stock.getAvailable()
        );
    }

    @Transactional(readOnly = true)
    public InventoryView getStock(Long productId) {

        InventoryStock541 stock =
                stockRepository.findById(productId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Product not found"
                                )
                        );

        return new InventoryView(
                stock.getProductId(),
                stock.getAvailable()
        );
    }

    public record OrderView(
            UUID orderId,
            long amountUzs,
            String status
    ) {}

    public record InventoryView(
            Long productId,
            int available
    ) {}
}
