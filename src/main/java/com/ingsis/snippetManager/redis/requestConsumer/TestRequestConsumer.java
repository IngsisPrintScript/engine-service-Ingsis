package com.ingsis.snippetManager.redis.requestConsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingsis.snippetManager.engine.SnippetRunnerService;
import com.ingsis.snippetManager.engine.dto.request.TestRequestDTO;
import com.ingsis.snippetManager.engine.dto.response.RunSnippetResponseDTO;
import com.ingsis.snippetManager.redis.resultProducer.TestResultProducer;
import com.ingsis.snippetManager.redis.dto.testing.SnippetTestStatus;
import com.ingsis.snippetManager.redis.dto.testing.TestRequestEvent;
import com.ingsis.snippetManager.redis.dto.testing.TestResultEvent;
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
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class TestRequestConsumer extends RedisStreamConsumer<String> {

    private static final Logger logger = LoggerFactory.getLogger(TestRequestConsumer.class);

    private final SnippetRunnerService service;
    private final TestResultProducer runResultProducer;
    private final SnippetStatusService snippetStatusService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final RedisTemplate<String, String> redisTemplate;

    public TestRequestConsumer(@Value("${redis.streams.runRequest}") String streamName,
            @Value("${redis.groups.run}") String groupName, RedisTemplate<String, String> redisTemplate,
            SnippetRunnerService service, TestResultProducer runResultProducer, ObjectMapper objectMapper,
            SnippetStatusService snippetStatusService) {
        super(streamName, groupName, redisTemplate);
        this.redisTemplate = redisTemplate;
        this.service = service;
        this.runResultProducer = runResultProducer;
        this.objectMapper = objectMapper;
        this.snippetStatusService = snippetStatusService;
    }

    @Override
    public void onMessage(@NotNull ObjectRecord<String, String> record) {
        executor.submit(() -> {
            TestRequestEvent event = null;
            try {
                event = objectMapper.readValue(record.getValue(), TestRequestEvent.class);

                UUID snippetId = event.snippetId();
                UUID testId = event.testId();
                String ownerId = event.ownerId();

                logger.info("[TEST] Processing Snippet({}) Test({})", snippetId, testId);

                snippetStatusService.markTestPending(snippetId);

                TestRequestDTO dto = new TestRequestDTO(snippetId, event.inputs(), event.expectedOutputs(),
                        event.language(), event.version());

                Result<RunSnippetResponseDTO> result = service.test(dto);

                boolean success = result.isCorrect();

                if (success) {
                    snippetStatusService.markTested(snippetId);
                } else {
                    snippetStatusService.markTestFailed(snippetId, "TEST_FAILED");
                }

                SnippetTestStatus finalStatus = success ? SnippetTestStatus.PASSED : SnippetTestStatus.FAILED;

                redisTemplate.opsForStream().acknowledge(getStreamKey(), getGroupId(), record.getId());

                publishWithRetry(ownerId, snippetId, event.testId(), finalStatus);
            } catch (Exception e) {
                logger.error("[RUN] Fatal error processing record", e);

                if (event != null) {
                    snippetStatusService.markTestFailed(event.snippetId(),
                            "EXCEPTION: " + e.getClass().getSimpleName());
                }
            }
        });
    }

    private void publishWithRetry(String ownerId, UUID snippetId, UUID testId, SnippetTestStatus status) {
        for (int i = 1; i <= 3; i++) {
            try {
                runResultProducer.publish(new TestResultEvent(ownerId, testId, snippetId, status));
                return;
            } catch (Exception e) {
                logger.warn("[RUN] Publish retry {} failed for Snippet({})", i, snippetId);
                try {
                    Thread.sleep(1000L * i);
                } catch (InterruptedException ignored) {
                }
            }
        }
        logger.error("[RUN] Result publish FAILED after 3 retries for Snippet({})", snippetId);
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
