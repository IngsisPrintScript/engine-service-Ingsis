//package com.ingsis.snippetManager.redis.testing;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.ingsis.snippetManager.redis.dto.TestResultEvent;
//import jakarta.annotation.PreDestroy;
//import org.austral.ingsis.redis.RedisStreamConsumer;
//import org.jetbrains.annotations.NotNull;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Profile;
//import org.springframework.data.redis.connection.stream.ObjectRecord;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.stream.StreamReceiver;
//import org.springframework.stereotype.Component;
//
//import java.util.Optional;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//@Component
//@Profile("!test")
//public class TestResultConsumer extends RedisStreamConsumer<String> {
//
//    private static final Logger logger = LoggerFactory.getLogger(TestResultConsumer.class);
//    private final ObjectMapper objectMapper;
//    private final SnippetRepo snippetRepository;
//    private final ExecutorService executor = Executors.newFixedThreadPool(10);
//
//    public TestResultConsumer(@Value("${redis.streams.testResult}") String streamName,
//            @Value("${redis.groups.test}") String groupName, RedisTemplate<String, String> redisTemplate,
//            ObjectMapper objectMapper, SnippetRepo snippetRepository) {
//        super(streamName, groupName, redisTemplate);
//        this.objectMapper = objectMapper;
//        this.snippetRepository = snippetRepository;
//    }
//
//    @Override
//    public void onMessage(@NotNull ObjectRecord<String, String> record) {
//        executor.submit(() -> {
//            try {
//                TestResultEvent event = objectMapper.readValue(record.getValue(), TestResultEvent.class);
//                logger.info("Received Test result for Snippet({}) - Test({}) - Status: {}", event.snippetId(),
//                        event.testId(), event.status());
//
//                Optional<Snippet> snippetOpt = snippetRepository.findById(event.snippetId());
//                if (snippetOpt.isEmpty()) {
//                    logger.warn("Snippet {} not found for test result", event.snippetId());
//                    return;
//                }
//
//                Snippet snippet = snippetOpt.get();
//                Optional<TestStatus> existing = snippet.getTestStatusList().stream()
//                        .filter(t -> t.getTestId().equals(event.testId())).findFirst();
//
//                if (existing.isPresent()) {
//                    existing.get().setTestStatus(event.status());
//                } else {
//                    TestStatus newStatus = new TestStatus();
//                    newStatus.setTestId(event.testId());
//                    newStatus.setTestStatus(event.status());
//                    snippet.getTestStatusList().add(newStatus);
//                }
//
//                snippetRepository.save(snippet);
//                logger.info("Updated test ({}) status for Snippet({}) -> {}", event.testId(), event.snippetId(),
//                        event.status());
//
//            } catch (Exception e) {
//                logger.error("Error processing test result: {}", e.getMessage(), e);
//            }
//        });
//    }
//
//    @Override
//    public @NotNull StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, String>> options() {
//        return StreamReceiver.StreamReceiverOptions.builder().pollTimeout(java.time.Duration.ofSeconds(10))
//                .targetType(String.class).build();
//    }
//
//    @PreDestroy
//    public void shutdown() {
//        executor.shutdown();
//    }
//}
