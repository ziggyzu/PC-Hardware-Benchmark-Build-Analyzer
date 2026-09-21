package com.pcanalyzer.model;

import java.util.Objects;

/**
 * Represents Random Access Memory (RAM).
 *
 * Design Decision:
 * Encapsulates the memory standard (DDR4 vs DDR5), capacity in GB, and frequency in MHz.
 * Compatibility rules verify that ramType matches the motherboard.
 */
public class Ram extends Component {

    private String ramType;
    private int capacityGb;
    private int speedMhz;

    public Ram(Integer id, String brand, String name, double price, int tdpWatts,
               String ramType, int capacityGb, int speedMhz) {
        super(id, ComponentType.RAM, brand, name, price, tdpWatts);
        this.ramType = Objects.requireNonNull(ramType, "RAM type must not be null");
        this.capacityGb = Math.max(1, capacityGb);
        this.speedMhz = Math.max(1, speedMhz);
    }

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

    @Override
    public String getKeySpecs() {
        return String.format("%s %dGB (%d MHz)", ramType, capacityGb, speedMhz);
    }
}
