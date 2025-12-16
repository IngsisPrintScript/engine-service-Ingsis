package com.ingsis.snippetManager.engine;

import com.ingsis.engine.DefaultEngine;
import com.ingsis.engine.Engine;
import com.ingsis.engine.services.ExecuteService;
import com.ingsis.engine.services.FormatService;
import com.ingsis.engine.services.LintService;
import com.ingsis.snippetManager.engine.supportedLanguage.SupportedLanguage;
import org.springframework.stereotype.Component;

@Component
public class LanguageEngineFactory {

    public Engine getEngine(SupportedLanguage language) {
        return switch (language.name().toLowerCase()) {
            case "printscript" -> new DefaultEngine(new ExecuteService(), new FormatService(), new LintService());
            default -> throw new IllegalArgumentException("Language not supported: " + language);
        };
    }
}
