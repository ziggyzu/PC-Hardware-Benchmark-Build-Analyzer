package com.pcanalyzer.model;

import java.util.Objects;

// Storage drive (SSD or Hard Disk) details like capacity, speeds, and interface type
public class Storage extends Component {

    // Storage drive specifications
    private String storageType;
    private int capacityGb;
    private int readSpeedMbS;
    private int writeSpeedMbS;

    // Create a new storage drive entry (NVMe SSD, SATA SSD, or HDD)
    public Storage(Integer id, String brand, String name, double price, int tdpWatts,
                   String storageType, int capacityGb, int readSpeedMbS, int writeSpeedMbS) {
        super(id, ComponentType.STORAGE, brand, name, price, tdpWatts);
        this.storageType = Objects.requireNonNull(storageType, "Storage type must not be null");
        this.capacityGb = Math.max(1, capacityGb);
        this.readSpeedMbS = Math.max(0, readSpeedMbS);
        this.writeSpeedMbS = Math.max(0, writeSpeedMbS);
    }

    // Getters and setters for storage specs
    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public int getCapacityGb() {
        return capacityGb;
    }

    public void setCapacityGb(int capacityGb) {
        this.capacityGb = Math.max(1, capacityGb);
    }

    public int getReadSpeedMbS() {
        return readSpeedMbS;
    }

    public void setReadSpeedMbS(int readSpeedMbS) {
        this.readSpeedMbS = Math.max(0, readSpeedMbS);
    }

    public int getWriteSpeedMbS() {
        return writeSpeedMbS;
    }

    public void setWriteSpeedMbS(int writeSpeedMbS) {
        this.writeSpeedMbS = Math.max(0, writeSpeedMbS);
    }

    // Display formatted capacity, drive technology, and read/write speeds
    @Override
    public String getKeySpecs() {
        String capacityStr = capacityGb >= 1000 ? (capacityGb / 1000) + "TB" : capacityGb + "GB";
        return String.format("%s %s | Read: %d MB/s, Write: %d MB/s",
                capacityStr, storageType, readSpeedMbS, writeSpeedMbS);
    }
}
