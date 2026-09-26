package com.pcanalyzer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// Holds a full PC build with its selected parts and calculations
public class Build {

    // Store the build name and the parts chosen for each slot
    private Integer id;
    private String name;
    private Cpu cpu;
    private Gpu gpu;
    private Motherboard motherboard;
    private Ram ram;
    private Psu psu;
    private final List<Component> extraComponents = new ArrayList<>();

    // Estimated baseline power used by motherboard, fans, and storage
    public static final int BASE_SYSTEM_OVERHEAD_WATTS = 70;

    // Start with a default build name
    public Build() {
        this("Custom Build");
    }

    // Start a build with a custom name
    public Build(String name) {
        this.name = Objects.requireNonNullElse(name, "Custom Build");
    }

    // Getters and setters for the build ID and name
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

    // Getters and setters for each hardware component slot
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

    // Manage any extra components added to the build
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

    // Put a component into its matching slot automatically based on what it is
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

    // Remove this specific component from whichever slot it is currently occupying
    public void removeComponent(Component component) {
        if (component == null) return;
        if (Objects.equals(cpu, component)) cpu = null;
        else if (Objects.equals(gpu, component)) gpu = null;
        else if (Objects.equals(motherboard, component)) motherboard = null;
        else if (Objects.equals(ram, component)) ram = null;
        else if (Objects.equals(psu, component)) psu = null;
        else extraComponents.remove(component);
    }

    // Empty out all parts from the build
    public void clear() {
        cpu = null;
        gpu = null;
        motherboard = null;
        ram = null;
        psu = null;
        extraComponents.clear();
    }

    // Collect all chosen parts into a single list
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

    // Calculate the total price of all parts in this build
    public double getTotalPrice() {
        return getAllComponents().stream()
                .mapToDouble(Component::getPrice)
                .sum();
    }

    // Add up the wattage for the CPU and GPU combined
    public int getTotalTdpWatts() {
        int cpuWatts = cpu != null ? cpu.getTdpWatts() : 0;
        int gpuWatts = gpu != null ? Math.max(gpu.getTdpWatts(), gpu.getBoardPowerWatts()) : 0;
        return cpuWatts + gpuWatts;
    }

    // Calculate maximum expected power usage including system baseline
    public int getEstimatedPeakPowerWatts() {
        return getTotalTdpWatts() + BASE_SYSTEM_OVERHEAD_WATTS;
    }

    // Recommend a power supply wattage with a 20% safety buffer
    public int getRecommendedPsuWatts() {
        return (int) Math.ceil(getEstimatedPeakPowerWatts() * 1.20);
    }
}
