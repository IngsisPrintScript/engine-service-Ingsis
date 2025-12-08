package com.ingsis.snippetManager.redis.dto.format;

import com.ingsis.snippetManager.engine.supportedLanguage.SupportedLanguage;
import com.ingsis.snippetManager.engine.supportedRules.FormatterSupportedRules;
import java.util.UUID;

public record FormatRequestEvent(String ownerId, UUID snippetId, SupportedLanguage language, String version,
        FormatterSupportedRules rules) {
}
