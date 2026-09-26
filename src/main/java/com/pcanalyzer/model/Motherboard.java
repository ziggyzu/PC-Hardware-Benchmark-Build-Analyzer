package com.pcanalyzer.model;

import java.util.Objects;

// Motherboard details including processor socket and supported RAM type
public class Motherboard extends Component {

    // Socket standard and supported memory generation
    private String socket;
    private String ramType;

    // Create a new motherboard entry
    public Motherboard(Integer id, String brand, String name, double price, int tdpWatts,
                       String socket, String ramType) {
        super(id, ComponentType.MOTHERBOARD, brand, name, price, tdpWatts);
        this.socket = Objects.requireNonNull(socket, "Socket must not be null");
        this.ramType = Objects.requireNonNull(ramType, "RAM type must not be null");
    }

    // Getters and setters for motherboard specs
    public String getSocket() {
        return socket;
    }

    public void setSocket(String socket) {
        this.socket = socket;
    }

    public String getRamType() {
        return ramType;
    }

    public void setRamType(String ramType) {
        this.ramType = ramType;
    }

    // Display the socket and RAM compatibility requirements
    @Override
    public String getKeySpecs() {
        return String.format("Socket: %s | RAM: %s", socket, ramType);
    }
}
