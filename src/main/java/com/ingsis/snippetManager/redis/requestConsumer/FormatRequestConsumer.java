package com.ingsis.snippetManager.redis.requestConsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingsis.engine.versions.Version;
import com.ingsis.snippetManager.engine.SnippetRunnerService;
import com.ingsis.snippetManager.redis.dto.format.FormatRequestEvent;
import com.ingsis.snippetManager.redis.dto.format.FormatResultEvent;
import com.ingsis.snippetManager.redis.dto.format.FormatStatus;
import com.ingsis.snippetManager.redis.resultProducer.FormatResultProducer;
import com.ingsis.snippetManager.status.SnippetStatusService;
import com.ingsis.utils.result.Result;
import jakarta.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.austral.ingsis.redis.RedisStreamConsumer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class FormatRequestConsumer extends RedisStreamConsumer<String> {

    private static final Logger logger = LoggerFactory.getLogger(FormatRequestConsumer.class);

    private final FormatResultProducer formatResultProducer;
    private final SnippetStatusService snippetStatusService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final SnippetRunnerService service;
    private final StringRedisTemplate redisTemplate;

    public FormatRequestConsumer(@Value("${redis.streams.formatRequest}") String streamName,
            @Value("${redis.groups.format}") String groupName, StringRedisTemplate redisTemplate,
            FormatResultProducer formatResultProducer, ObjectMapper objectMapper, SnippetRunnerService service,
            SnippetStatusService snippetStatusService) {

        super(streamName, groupName, redisTemplate);
        this.redisTemplate = redisTemplate;
        this.formatResultProducer = formatResultProducer;
        this.objectMapper = objectMapper;
        this.service = service;
        this.snippetStatusService = snippetStatusService;
    }

    @Override
    public void onMessage(@NotNull ObjectRecord<String, String> record) {
        executor.submit(() -> {
            FormatRequestEvent event = null;

            try {
                event = objectMapper.readValue(record.getValue(), FormatRequestEvent.class);

                UUID snippetId = event.snippetId();
                String ownerId = event.ownerId();

                logger.info("[FORMAT] Processing Snippet({})", snippetId);

                snippetStatusService.markFormatPending(snippetId);

                Version version = Version.fromString(event.version());

                Result<UUID> formatted = service.format(snippetId,event.formatId(), version, event.rules(), event.language());

                FormatStatus finalStatus;

                if (formatted.isCorrect()) {
                    snippetStatusService.markFormatted(snippetId);
                    finalStatus = FormatStatus.PASSED;
                } else {
                    snippetStatusService.markFormatFailed(snippetId, "FORMAT_ERROR");
                    finalStatus = FormatStatus.FAILED;
                }
                redisTemplate.opsForStream().acknowledge(getStreamKey(), getGroupId(), record.getId());
                publishWithRetry(ownerId, snippetId, finalStatus);

            } catch (Exception e) {
                logger.error("[FORMAT] Fatal error processing record", e);

                if (event != null) {
                    snippetStatusService.markFormatFailed(event.snippetId(),
                            "EXCEPTION: " + e.getClass().getSimpleName());
                }
            }
        });
    }

    private void publishWithRetry(String ownerId, UUID snippetId, FormatStatus status) {
        for (int i = 1; i <= 3; i++) {
            try {
                formatResultProducer.publish(new FormatResultEvent(ownerId, snippetId, status));
                return;
            } catch (Exception e) {
                logger.warn("[FORMAT] Publish retry {} failed for Snippet({})", i, snippetId);

                try {
                    Thread.sleep(1000L * i);
                } catch (InterruptedException ignored) {
                }
            }
        }

        logger.error("[FORMAT] Result publish FAILED after 3 retries for Snippet({})", snippetId);
    }

    @Override
    public @NotNull StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, String>> options() {
        return StreamReceiver.StreamReceiverOptions.builder().pollTimeout(java.time.Duration.ofSeconds(10))
                .targetType(String.class).build();
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
