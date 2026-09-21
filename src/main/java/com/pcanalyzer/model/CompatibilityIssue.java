package com.pcanalyzer.model;

import java.util.Objects;

/**
 * Represents a single compatibility diagnostic issue.
 */
public class CompatibilityIssue {

    private final String ruleName;
    private final String message;
    private final CompatibilityStatus severity;

    public CompatibilityIssue(String ruleName, String message, CompatibilityStatus severity) {
        this.ruleName = Objects.requireNonNull(ruleName, "Rule name cannot be null");
        this.message = Objects.requireNonNull(message, "Message cannot be null");
        this.severity = Objects.requireNonNull(severity, "Severity cannot be null");
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getMessage() {
        return message;
    }

    public CompatibilityStatus getSeverity() {
        return severity;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s", severity, ruleName, message);
    }
}
