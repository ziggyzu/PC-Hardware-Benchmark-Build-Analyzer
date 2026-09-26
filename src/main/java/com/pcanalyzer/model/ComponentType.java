package com.pcanalyzer.model;

// List of all component categories we support in the application
public enum ComponentType {
    CPU("CPU (Processor)"),
    GPU("GPU (Graphics Card)"),
    MOTHERBOARD("Motherboard"),
    RAM("RAM (Memory)"),
    PSU("Power Supply (PSU)");

    // Human readable name for the UI
    private final String displayName;

    // Attach display name to each category
    ComponentType(String displayName) {
        this.displayName = displayName;
    }

    // Get the display name for UI dropdowns and labels
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    // Match a string from user input or database back to the enum category
    public static ComponentType fromString(String value) {
        if (value == null) {
            return null;
        }
        for (ComponentType type : values()) {
            if (type.name().equalsIgnoreCase(value) || type.displayName.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
