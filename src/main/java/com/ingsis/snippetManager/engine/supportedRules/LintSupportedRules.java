package com.ingsis.snippetManager.engine.supportedRules;

import org.springframework.web.bind.annotation.RequestAttribute;

public record LintSupportedRules(@RequestAttribute(required = false) boolean mandatoryVariableOrLiteralInPrintln,
        @RequestAttribute(required = false) boolean mandatoryVariableOrLiteralInReadInput,
        @RequestAttribute(required = false) String identifierFormat) {
}
