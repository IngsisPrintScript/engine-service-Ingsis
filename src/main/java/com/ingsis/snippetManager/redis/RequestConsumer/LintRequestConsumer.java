package com.ingsis.snippetManager.redis.RequestConsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingsis.snippetManager.engine.SnippetRunnerService;
import com.ingsis.snippetManager.intermediate.azureStorageConfig.AssetService;
import com.ingsis.snippetManager.redis.ResultProducer.LintResultProducer;
import com.ingsis.snippetManager.redis.dto.lint.LintRequestEvent;
import com.ingsis.snippetManager.redis.dto.lint.LintResultEvent;
import com.ingsis.snippetManager.redis.dto.lint.LintStatus;
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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Profile("!test")
public class LintRequestConsumer extends RedisStreamConsumer<String> {

    private static final Logger logger = LoggerFactory.getLogger(LintRequestConsumer.class);

    private final SnippetRunnerService service;
    private final LintResultProducer lintResultProducer;
    private final ObjectMapper objectMapper;
    private final AssetService assetService;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public LintRequestConsumer(@Value("${redis.streams.lintRequest}") String streamName,
                               @Value("${redis.groups.lint}") String groupName, RedisTemplate<String, String> redisTemplate,
                               SnippetRunnerService lintingService,AssetService assetService ,LintResultProducer lintResultProducer, ObjectMapper objectMapper) {

        super(streamName, groupName, redisTemplate);
        this.service = lintingService;
        this.lintResultProducer = lintResultProducer;
        this.objectMapper = objectMapper;
        this.assetService = assetService;
    }

    @Override
    public void onMessage(@NotNull ObjectRecord<String, String> record) {
        executor.submit(() -> {
            try {
                LintRequestEvent event = objectMapper.readValue(record.getValue(), LintRequestEvent.class);
                logger.info("Processing lint request for Snippet({}) from User({})", event.snippetId().toString(),
                        event.ownerId());

                ResponseEntity<List<Result>> response = service.analyze(event.snippetId(),event.version(),event.supportedRules(),event.language());
                List<Result> results = response.getBody();
                LintStatus status = (results == null || results.isEmpty()) ? LintStatus.PASSED : LintStatus.FAILED;

                lintResultProducer.publish(new LintResultEvent(event.ownerId(), event.snippetId(), status));
            } catch (Exception ex) {
                logger.error("Error processing lint request: {}", ex.getMessage());
            }
            return null;
        });
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
