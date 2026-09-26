package com.pcanalyzer.model;

// Levels of compatibility status for a build
public enum CompatibilityStatus {
    COMPATIBLE("Compatible", "All selected components are fully compatible."),
    WARNING("Warning", "Components are functional, but with potential limitations or tight headroom."),
    INCOMPATIBLE("Incompatible", "Hardware conflict detected. System cannot operate safely or fit together.");

    // User-friendly label and description
    private final String label;
    private final String description;

    // Attach label and description to each status level
    CompatibilityStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }

    // Get the display label or full explanation
    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
