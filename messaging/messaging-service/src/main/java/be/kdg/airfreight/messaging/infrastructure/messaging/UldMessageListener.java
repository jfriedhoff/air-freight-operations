package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.nio.charset.StandardCharsets;

import be.kdg.airfreight.messaging.application.BookingService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
class UldMessageListener {
    private final BookingService bookings;

    UldMessageListener(BookingService bookings) {
        this.bookings = bookings;
    }

    @RabbitListener(queues = MessagingConfig.BOOKINGS_QUEUE)
    void on(Message message) {
        bookings.receiveUldMessage(new String(message.getBody(), StandardCharsets.UTF_8));
    }
}
