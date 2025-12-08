package com.ingsis.snippetManager.redis.dto.testing;

import com.ingsis.snippetManager.engine.supportedLanguage.SupportedLanguage;

import java.util.List;
import java.util.UUID;

public record TestRequestEvent(String ownerId,UUID testId, UUID snippetId, SupportedLanguage language,String version,List<String> inputs,
                               List<String> expectedOutputs) {}
