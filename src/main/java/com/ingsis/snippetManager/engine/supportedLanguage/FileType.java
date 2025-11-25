package com.ingsis.snippetManager.engine.supportedLanguage;

public record FileType(String language, String extension) {

    public FileType(String language, String extension) {
        this.language = language;
        this.extension = extension;
    }
}
