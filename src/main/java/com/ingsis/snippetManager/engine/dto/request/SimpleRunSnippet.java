package com.ingsis.snippetManager.engine.dto.request;

import com.ingsis.snippetManager.engine.supportedLanguage.SupportedLanguage;
import java.util.UUID;

public record SimpleRunSnippet(UUID snippetId, SupportedLanguage language, String version) {
}
