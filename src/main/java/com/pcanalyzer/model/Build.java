package com.pcanalyzer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a complete or in-progress PC build configuration.
 *
 * Design Decision:
 * 1. Slot-Based Aggregation: Standard PC builds consist of dedicated slots
 *    (CPU, GPU, Motherboard, RAM, PSU). Modeling these as distinct typed slots
 *    guarantees type safety while allowing an optional list of extra components
 *    (such as additional storage or cooling).
 * 2. Domain Calculations: Encapsulates total price, TDP, and estimated power
 *    within the domain entity so that business rules stay cohesive and testable.
 */
public class Build {

    private Integer id;
    private String name;
    private Cpu cpu;
    private Gpu gpu;
    private Motherboard motherboard;
    private Ram ram;
    private Psu psu;
    private final List<Component> extraComponents = new ArrayList<>();

    // Fixed overhead in watts for motherboard chipset, RAM sticks, storage drives, and cooling fans
    public static final int BASE_SYSTEM_OVERHEAD_WATTS = 70;

    public Build() {
        this("Custom Build");
    }

    public Build(String name) {
        this.name = Objects.requireNonNullElse(name, "Custom Build");
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Cpu getCpu() {
        return cpu;
    }

    public void setCpu(Cpu cpu) {
        this.cpu = cpu;
    }

    public Gpu getGpu() {
        return gpu;
    }

    public void setGpu(Gpu gpu) {
        this.gpu = gpu;
    }

    public Motherboard getMotherboard() {
        return motherboard;
    }

    public void setMotherboard(Motherboard motherboard) {
        this.motherboard = motherboard;
    }

    public Ram getRam() {
        return ram;
    }

    public void setRam(Ram ram) {
        this.ram = ram;
    }

    public Psu getPsu() {
        return psu;
    }

    public void setPsu(Psu psu) {
        this.psu = psu;
    }

    public List<Component> getExtraComponents() {
        return Collections.unmodifiableList(extraComponents);
    }

    public void addExtraComponent(Component component) {
        if (component != null) {
            extraComponents.add(component);
        }
    }

    public void removeExtraComponent(Component component) {
        extraComponents.remove(component);
    }

    /**
     * Polymorphically assign a component to its corresponding slot based on its type.
     *
     * @param component The component to install
     */
    public void setComponent(Component component) {
        if (component == null) return;
        switch (component.getType()) {
            case CPU -> setCpu((Cpu) component);
            case GPU -> setGpu((Gpu) component);
            case MOTHERBOARD -> setMotherboard((Motherboard) component);
            case RAM -> setRam((Ram) component);
            case PSU -> setPsu((Psu) component);
        }
    }

    /**
     * Remove component from its slot if it matches.
     *
     * @param component Component to remove
     */
    public void removeComponent(Component component) {
        if (component == null) return;
        if (Objects.equals(cpu, component)) cpu = null;
        else if (Objects.equals(gpu, component)) gpu = null;
        else if (Objects.equals(motherboard, component)) motherboard = null;
        else if (Objects.equals(ram, component)) ram = null;
        else if (Objects.equals(psu, component)) psu = null;
        else extraComponents.remove(component);
    }

    /**
     * Clear all components from the build.
     */
    public void clear() {
        cpu = null;
        gpu = null;
        motherboard = null;
        ram = null;
        psu = null;
        extraComponents.clear();
    }

    /**
     * Returns a list of all currently assigned components.
     *
     * @return List of non-null components in this build
     */
    public List<Component> getAllComponents() {
        List<Component> list = new ArrayList<>();
        if (cpu != null) list.add(cpu);
        if (gpu != null) list.add(gpu);
        if (motherboard != null) list.add(motherboard);
        if (ram != null) list.add(ram);
        if (psu != null) list.add(psu);
        list.addAll(extraComponents);
        return list;
    }

    /**
     * Calculates the total cost of all components in this build.
     *
     * @return Sum of component prices in USD
     */
    public double getTotalPrice() {
        return getAllComponents().stream()
                .mapToDouble(Component::getPrice)
                .sum();
    }

    /**
     * Calculates raw TDP sum of the CPU and GPU.
     *
     * @return Total TDP in Watts
     */
    public int getTotalTdpWatts() {
        int cpuWatts = cpu != null ? cpu.getTdpWatts() : 0;
        int gpuWatts = gpu != null ? Math.max(gpu.getTdpWatts(), gpu.getBoardPowerWatts()) : 0;
        return cpuWatts + gpuWatts;
    }

    /**
     * Calculates the estimated peak power draw under full system load,
     * adding baseline motherboard/RAM/cooling overhead.
     *
     * @return Estimated power consumption in Watts
     */
    public int getEstimatedPeakPowerWatts() {
        return getTotalTdpWatts() + BASE_SYSTEM_OVERHEAD_WATTS;
    }

    /**
     * Calculates the recommended PSU wattage providing ~20% safety headroom
     * to prevent shutdowns during transient power spikes.
     *
     * @return Recommended minimum PSU wattage
     */
    public int getRecommendedPsuWatts() {
        return (int) Math.ceil(getEstimatedPeakPowerWatts() * 1.20);
    }
}
