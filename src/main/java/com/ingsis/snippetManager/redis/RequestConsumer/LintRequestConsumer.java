package com.ingsis.snippetManager.redis.RequestConsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingsis.engine.versions.Version;
import com.ingsis.snippetManager.engine.SnippetRunnerService;
import com.ingsis.snippetManager.redis.ResultProducer.LintResultProducer;
import com.ingsis.snippetManager.redis.dto.lint.LintRequestEvent;
import com.ingsis.snippetManager.redis.dto.lint.LintResultEvent;
import com.ingsis.snippetManager.redis.dto.lint.LintStatus;
import com.ingsis.snippetManager.status.SnippetStatusService;
import com.ingsis.utils.result.Result;
import jakarta.annotation.PreDestroy;
import org.austral.ingsis.redis.RedisStreamConsumer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Profile("!test")
public class LintRequestConsumer extends RedisStreamConsumer<String> {

    private static final Logger logger = LoggerFactory.getLogger(LintRequestConsumer.class);

    private final SnippetRunnerService service;
    private final LintResultProducer lintResultProducer;
    private final SnippetStatusService snippetStatusService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final RedisTemplate<String, String> redisTemplate;

    public LintRequestConsumer(@Value("${redis.streams.lintRequest}") String streamName,
                               @Value("${redis.groups.lint}") String groupName, RedisTemplate<String, String> redisTemplate,
                               SnippetRunnerService lintingService,
                               LintResultProducer lintResultProducer, ObjectMapper objectMapper,SnippetStatusService snippetStatusService) {

        super(streamName, groupName, redisTemplate);
        this.service = lintingService;
        this.redisTemplate = redisTemplate;
        this.lintResultProducer = lintResultProducer;
        this.objectMapper = objectMapper;
        this.snippetStatusService = snippetStatusService;
    }

    @Override
    public void onMessage(@NotNull ObjectRecord<String, String> record) {
        executor.submit(() -> {
            LintRequestEvent event = null;
            try {
                event = objectMapper.readValue(record.getValue(), LintRequestEvent.class);

                UUID snippetId = event.snippetId();
                String ownerId = event.ownerId();

                logger.info("[LINT] Processing Snippet({})", snippetId);

                snippetStatusService.markLintPending(snippetId);

                Version version = Version.fromString(event.version());

                Result<String> response = service.analyze(
                        snippetId,
                        version,
                        event.supportedRules(),
                        event.language()
                );

                if (response.isCorrect()) {
                    snippetStatusService.markLinted(snippetId);
                } else {
                    snippetStatusService.markLintFailed(snippetId, "LINT_ERRORS");
                }

                LintStatus finalStatus = response.isCorrect()
                        ? LintStatus.PASSED
                        : LintStatus.FAILED;
                redisTemplate.opsForStream().acknowledge(
                        getStreamKey(),
                        getGroupId(),
                        record.getId()
                );
                publishWithRetry(ownerId, snippetId, finalStatus);
            } catch (Exception e) {
                logger.error("[LINT] Fatal error processing record", e);

                if (event != null) {
                    snippetStatusService.markLintFailed(
                            event.snippetId(),
                            "EXCEPTION: " + e.getClass().getSimpleName()
                    );
                }
            }
        });
    }

    private void publishWithRetry(String ownerId, UUID snippetId, LintStatus status) {
        for (int i = 1; i <= 3; i++) {
            try {
                lintResultProducer.publish(
                        new LintResultEvent(ownerId, snippetId, status)
                );
                return;
            } catch (Exception e) {
                logger.warn("[LINT] Publish retry {} failed for Snippet({})", i, snippetId);

                try {
                    Thread.sleep(1000L * i);
                } catch (InterruptedException ignored) {}
            }
        }
        logger.error("[LINT] Result publish FAILED after 3 retries for Snippet({})", snippetId);
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
