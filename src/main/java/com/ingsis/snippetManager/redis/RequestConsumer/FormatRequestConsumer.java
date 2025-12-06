package com.ingsis.snippetManager.redis.RequestConsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingsis.snippetManager.engine.SnippetRunnerService;
import com.ingsis.snippetManager.intermediate.azureStorageConfig.AssetService;
import com.ingsis.snippetManager.redis.ResultProducer.FormatResultProducer;
import com.ingsis.snippetManager.redis.dto.format.FormatRequestEvent;
import com.ingsis.snippetManager.redis.dto.format.FormatResultEvent;
import com.ingsis.snippetManager.redis.dto.format.FormatStatus;
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Profile("!test")
public class FormatRequestConsumer extends RedisStreamConsumer<String> {

    private static final Logger logger = LoggerFactory.getLogger(FormatRequestConsumer.class);

    private final FormatResultProducer formatResultProducer;
    private final AssetService assetService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final SnippetRunnerService service;

    public FormatRequestConsumer(@Value("${redis.streams.formatRequest}") String streamName,
                                 @Value("${redis.groups.format}") String groupName,
                                 RedisTemplate<String, String> redisTemplate,FormatResultProducer formatResultProducer,
                                 ObjectMapper objectMapper,SnippetRunnerService service,
                                 AssetService assetService) {

        super(streamName, groupName, redisTemplate);
        this.formatResultProducer = formatResultProducer;
        this.objectMapper = objectMapper;
        this.service = service;
        this.assetService = assetService;
    }

    @Override
    public void onMessage(@NotNull ObjectRecord<String, String> record) {
        executor.submit(() -> {
            try {
                FormatRequestEvent event = objectMapper.readValue(record.getValue(), FormatRequestEvent.class);
                logger.info("Processing lint request for Snippet({}) from User({})", event.snippetId().toString(),
                        event.ownerId());

                ResponseEntity<Result> response = service.format(assetService.getSnippet(event.snippetId()), event.ownerId());
                Result results = response.getBody();
                FormatStatus status = (results != null) ? FormatStatus.PASSED : FormatStatus.FAILED;

                formatResultProducer.publish(new FormatResultEvent(event.ownerId(), event.snippetId(), status));
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
