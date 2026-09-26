package com.pcanalyzer.model;

// Graphics card (GPU) details and gaming framerate performance
public class Gpu extends Component {

    // Extra GPU specs like video memory, wattage, and average FPS
    private int vramGb;
    private int boardPowerWatts;
    private int recommendedPsuWatts;
    private double fps1080p;
    private double fps1440p;
    private double fps4k;

    // Create a new GPU with its specs and gaming benchmark numbers
    public Gpu(Integer id, String brand, String name, double price, int tdpWatts,
               int vramGb, int boardPowerWatts, int recommendedPsuWatts,
               double fps1080p, double fps1440p, double fps4k) {
        super(id, ComponentType.GPU, brand, name, price, tdpWatts);
        this.vramGb = Math.max(1, vramGb);
        this.boardPowerWatts = Math.max(0, boardPowerWatts);
        this.recommendedPsuWatts = Math.max(0, recommendedPsuWatts);
        this.fps1080p = Math.max(0.0, fps1080p);
        this.fps1440p = Math.max(0.0, fps1440p);
        this.fps4k = Math.max(0.0, fps4k);
    }

    // Getters and setters for GPU specs
    public int getVramGb() {
        return vramGb;
    }

    public void setVramGb(int vramGb) {
        this.vramGb = Math.max(1, vramGb);
    }

    public int getBoardPowerWatts() {
        return boardPowerWatts;
    }

    public void setBoardPowerWatts(int boardPowerWatts) {
        this.boardPowerWatts = Math.max(0, boardPowerWatts);
    }

    public int getRecommendedPsuWatts() {
        return recommendedPsuWatts;
    }

    public void setRecommendedPsuWatts(int recommendedPsuWatts) {
        this.recommendedPsuWatts = Math.max(0, recommendedPsuWatts);
    }

    public double getFps1080p() {
        return fps1080p;
    }

    public void setFps1080p(double fps1080p) {
        this.fps1080p = Math.max(0.0, fps1080p);
    }

    public double getFps1440p() {
        return fps1440p;
    }

    public void setFps1440p(double fps1440p) {
        this.fps1440p = Math.max(0.0, fps1440p);
    }

    public double getFps4k() {
        return fps4k;
    }

    public void setFps4k(double fps4k) {
        this.fps4k = Math.max(0.0, fps4k);
    }

    // Look up the framerate for a given screen resolution
    public double getFpsForResolution(String resolution) {
        if (resolution == null) return 0.0;
        return switch (resolution.trim().toUpperCase()) {
            case "1080P", "FHD" -> fps1080p;
            case "1440P", "QHD", "2K" -> fps1440p;
            case "4K", "UHD", "2160P" -> fps4k;
            default -> 0.0;
        };
    }

    // Format the VRAM, power, and FPS stats into a clean summary
    @Override
    public String getKeySpecs() {
        return String.format("%dGB VRAM | %dW Board | FPS: %.0f (1080p) / %.0f (1440p) / %.0f (4K)",
                vramGb, boardPowerWatts, fps1080p, fps1440p, fps4k);
    }
}
