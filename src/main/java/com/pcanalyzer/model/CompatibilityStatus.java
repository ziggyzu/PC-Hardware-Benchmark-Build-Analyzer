package com.pcanalyzer.model;

/**
 * Severity level of compatibility evaluation.
 */
public enum CompatibilityStatus {
    COMPATIBLE("Compatible", "All selected components are fully compatible."),
    WARNING("Warning", "Components are functional, but with potential limitations or tight headroom."),
    INCOMPATIBLE("Incompatible", "Hardware conflict detected. System cannot operate safely or fit together.");

    private final String label;
    private final String description;

    CompatibilityStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
