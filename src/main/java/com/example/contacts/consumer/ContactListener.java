package com.example.contacts.consumer;

import com.example.contacts.avro.ContactProjected;
import com.example.contacts.avro.ContactRaw;
import com.example.contacts.config.KafkaTopicsProperties;
import com.example.contacts.mapper.ContactMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class ContactListener {

    private static final Logger log = LoggerFactory.getLogger(ContactListener.class);

    private final KafkaTemplate<String, ContactProjected> template;
    private final KafkaTopicsProperties topics;
    private final Timer projectionTimer;

    public ContactListener(KafkaTemplate<String, ContactProjected> template,
                           KafkaTopicsProperties topics,
                           MeterRegistry registry) {
        this.template = template;
        this.topics = topics;
        this.projectionTimer = Timer.builder("contacts.projection.latency")
                .description("End-to-end time from consumer record received to producer ack")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    @KafkaListener(
            topics = "${app.kafka.topics.source}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "contactKafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, ContactRaw> record, Acknowledgment ack) {
        Timer.Sample sample = Timer.start();
        ContactRaw raw = record.value();
        if (raw == null) {
            sendTombstone(record.key(), ack, sample);
            return;
        }

        ContactProjected projected = ContactMapper.toProjected(raw);

        CompletableFuture<SendResult<String, ContactProjected>> future =
                template.send(topics.sink(), record.key(), projected);

        future.whenComplete((res, ex) -> {
            if (ex != null) {
                throw new RuntimeException("Producer ack failed for key=" + record.key(), ex);
            }
            ack.acknowledge();
            sample.stop(projectionTimer);
            if (log.isDebugEnabled()) {
                log.debug("Projected contactId={} to {}-{}@{}",
                        projected.getContactId(),
                        res.getRecordMetadata().topic(),
                        res.getRecordMetadata().partition(),
                        res.getRecordMetadata().offset());
            }
        });
    }

    private void sendTombstone(String key, Acknowledgment ack, Timer.Sample sample) {
        template.send(topics.sink(), key, null).whenComplete((r, ex) -> {
            if (ex != null) {
                throw new RuntimeException("Tombstone send failed for key=" + key, ex);
            }
            ack.acknowledge();
            sample.stop(projectionTimer);
        });
    }
}
