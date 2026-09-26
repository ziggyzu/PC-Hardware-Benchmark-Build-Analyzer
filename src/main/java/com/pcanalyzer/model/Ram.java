package com.pcanalyzer.model;

import java.util.Objects;

// System memory (RAM) details like type, size, and speed
public class Ram extends Component {

    // Memory generation, capacity in gigabytes, and speed in MHz
    private String ramType;
    private int capacityGb;
    private int speedMhz;

    // Create a new RAM stick entry
    public Ram(Integer id, String brand, String name, double price, int tdpWatts,
               String ramType, int capacityGb, int speedMhz) {
        super(id, ComponentType.RAM, brand, name, price, tdpWatts);
        this.ramType = Objects.requireNonNull(ramType, "RAM type must not be null");
        this.capacityGb = Math.max(1, capacityGb);
        this.speedMhz = Math.max(1, speedMhz);
    }

    // Getters and setters for RAM specs
    public String getRamType() {
        return ramType;
    }

    public void setRamType(String ramType) {
        this.ramType = ramType;
    }

    public int getCapacityGb() {
        return capacityGb;
    }

    public void setCapacityGb(int capacityGb) {
        this.capacityGb = Math.max(1, capacityGb);
    }

    public int getSpeedMhz() {
        return speedMhz;
    }

    public void setSpeedMhz(int speedMhz) {
        this.speedMhz = Math.max(1, speedMhz);
    }

    // Show RAM generation, capacity, and frequency in a clean string
    @Override
    public String getKeySpecs() {
        return String.format("%s %dGB (%d MHz)", ramType, capacityGb, speedMhz);
    }
}
