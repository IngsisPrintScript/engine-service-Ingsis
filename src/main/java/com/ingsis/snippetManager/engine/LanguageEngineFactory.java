package com.ingsis.snippetManager.engine;

import com.ingsis.engine.Engine;
import com.ingsis.engine.InMemoryEngine;
import com.ingsis.snippetManager.engine.supportedLanguage.SupportedLanguage;
import org.springframework.stereotype.Component;

@Component
public class LanguageEngineFactory {

    public Engine getEngine(SupportedLanguage language) {
        return switch (language.name()) {
            case "printscript" -> new InMemoryEngine();
            default -> throw new IllegalArgumentException(
                    "Language not supported: " + language
            );
        };
    }
}
