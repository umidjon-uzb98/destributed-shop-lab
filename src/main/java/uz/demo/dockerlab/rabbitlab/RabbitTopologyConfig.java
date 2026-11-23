
package uz.demo.dockerlab.rabbitlab;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {

    public static final String EXCHANGE =
            "shop.events";

    public static final String NOTIFICATION_QUEUE =
            "notification.orders";

    public static final String ROUTING_KEY =
            "order.created";

    // 526-dars: Dead Letter topology
    public static final String DLX =
            "shop.dlx";

    public static final String DLQ =
            "notification.orders.dlq";

    public static final String DEAD_ROUTING_KEY =
            "notification.dead";

    // 525-dars: Normal Exchange
    @Bean
    public TopicExchange shopEventsExchange() {

        return new TopicExchange(
                EXCHANGE,
                true,
                false
        );
    }

    // 525-dars: Asosiy Queue
    // Mavjud queue argumentlarini o'zgartirmaymiz.
    @Bean
    public Queue notificationOrdersQueue() {

        return new Queue(
                NOTIFICATION_QUEUE,
                true
        );
    }

    @Bean
    public Binding notificationOrdersBinding(
            @Qualifier("notificationOrdersQueue")
            Queue queue,

            TopicExchange shopEventsExchange
    ) {

        return BindingBuilder
                .bind(queue)
                .to(shopEventsExchange)
                .with(ROUTING_KEY);
    }

    // 526-dars: Dead Letter Exchange
    @Bean
    public DirectExchange notificationDlx() {

        return new DirectExchange(
                DLX,
                true,
                false
        );
    }

    // 526-dars: Dead Letter Queue
    @Bean
    public Queue notificationDeadLetterQueue() {

        return new Queue(
                DLQ,
                true
        );
    }

    // 526-dars: DLX -> DLQ binding
    @Bean
    public Binding notificationDeadLetterBinding(
            @Qualifier("notificationDeadLetterQueue")
            Queue dlq,

            DirectExchange notificationDlx
    ) {

        return BindingBuilder
                .bind(dlq)
                .to(notificationDlx)
                .with(DEAD_ROUTING_KEY);
    }

    // 525-darsdagi JSON converter saqlanadi
    @Bean
    public MessageConverter rabbitMessageConverter() {

        return new JacksonJsonMessageConverter(
                "uz.demo.dockerlab.rabbitlab"
        );
    }
}
