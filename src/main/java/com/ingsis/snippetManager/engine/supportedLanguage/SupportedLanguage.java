package com.ingsis.snippetManager.engine.supportedLanguage;

public enum SupportedLanguage {
    PRINTSCRIPT(new FileType("pisp", "PrintScript")), JAVASCRIPT(new FileType("js", "JavaScript"));

    private final FileType extensionType;

    SupportedLanguage(FileType extension) {
        this.extensionType = extension;
    }

    public FileType getFileType() {
        return extensionType;
    }
}
