package com.pcanalyzer.ui;

import com.pcanalyzer.db.ComponentDao;
import com.pcanalyzer.model.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

/**
 * Controller for the Add Component modal dialog.
 *
 * Design Decision:
 * 1. Thin Controller: Validates form input, creates the domain entity, and delegates persistence to ComponentDao.
 * 2. Dynamic Spec Form: Switches visible input fields based on the selected ComponentType.
 */
public class AddComponentDialogController {

    @FXML private ComboBox<ComponentType> typeComboBox;
    @FXML private TextField brandField;
    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private TextField tdpField;

    // CPU Fields
    @FXML private GridPane cpuForm;
    @FXML private TextField cpuSocketField;
    @FXML private TextField cpuCoresField;
    @FXML private TextField cpuThreadsField;
    @FXML private TextField cpuBaseClockField;
    @FXML private TextField cpuBoostClockField;
    @FXML private TextField cpuScoreField;

    // GPU Fields
    @FXML private GridPane gpuForm;
    @FXML private TextField gpuVramField;
    @FXML private TextField gpuBoardPowerField;
    @FXML private TextField gpuRecPsuField;
    @FXML private TextField gpuFps1080Field;
    @FXML private TextField gpuFps1440Field;
    @FXML private TextField gpuFps4kField;

    // Motherboard Fields
    @FXML private GridPane moboForm;
    @FXML private TextField moboSocketField;
    @FXML private TextField moboRamField;

    // RAM Fields
    @FXML private GridPane ramForm;
    @FXML private TextField ramTypeField;
    @FXML private TextField ramCapacityField;
    @FXML private TextField ramSpeedField;

    // PSU Fields
    @FXML private GridPane psuForm;
    @FXML private TextField psuWattageField;
    @FXML private TextField psuRatingField;

    // Security Field
    @FXML private PasswordField adminPasswordField;

    @FXML private Label errorLabel;

    private ComponentDao componentDao;
    private Stage dialogStage;
    private boolean componentAdded = false;

    @FXML
    public void initialize() {
        typeComboBox.setItems(FXCollections.observableArrayList(ComponentType.values()));
        typeComboBox.getSelectionModel().select(ComponentType.CPU);

        // Switch visible form when category changes
        typeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            switchSpecForm(newVal);
        });

        switchSpecForm(ComponentType.CPU);
    }

    public void setComponentDao(ComponentDao componentDao) {
        this.componentDao = componentDao;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isComponentAdded() {
        return componentAdded;
    }

    private void switchSpecForm(ComponentType type) {
        cpuForm.setVisible(type == ComponentType.CPU);
        gpuForm.setVisible(type == ComponentType.GPU);
        moboForm.setVisible(type == ComponentType.MOTHERBOARD);
        ramForm.setVisible(type == ComponentType.RAM);
        psuForm.setVisible(type == ComponentType.PSU);
    }

    @FXML
    private void handleSave() {
        errorLabel.setText("");
        try {
            ComponentType type = typeComboBox.getValue();
            String brand = brandField.getText() != null ? brandField.getText().trim() : "";
            String name = nameField.getText() != null ? nameField.getText().trim() : "";

            if (brand.isEmpty() || name.isEmpty()) {
                throw new IllegalArgumentException("Brand and Model Name are required.");
            }

            // Security check using PasswordField
            String passkey = adminPasswordField.getText() != null ? adminPasswordField.getText().trim() : "";
            if (!passkey.isEmpty() && !passkey.equals("admin123")) {
                throw new IllegalArgumentException("Invalid Admin Passkey. Enter 'admin123' or leave blank for guest access.");
            }

            double price = Double.parseDouble(priceField.getText().trim());
            int tdp = Integer.parseInt(tdpField.getText().trim());

            Component created;
            switch (type) {
                case CPU -> {
                    String socket = cpuSocketField.getText().trim();
                    if (socket.isEmpty()) throw new IllegalArgumentException("CPU socket is required.");
                    int cores = Integer.parseInt(cpuCoresField.getText().trim());
                    int threads = Integer.parseInt(cpuThreadsField.getText().trim());
                    double base = Double.parseDouble(cpuBaseClockField.getText().trim());
                    double boost = Double.parseDouble(cpuBoostClockField.getText().trim());
                    int score = Integer.parseInt(cpuScoreField.getText().trim());
                    created = new Cpu(null, brand, name, price, tdp, socket, cores, threads, base, boost, score);
                }
                case GPU -> {
                    int vram = Integer.parseInt(gpuVramField.getText().trim());
                    int boardPower = Integer.parseInt(gpuBoardPowerField.getText().trim());
                    int recPsu = Integer.parseInt(gpuRecPsuField.getText().trim());
                    double fps1080 = Double.parseDouble(gpuFps1080Field.getText().trim());
                    double fps1440 = Double.parseDouble(gpuFps1440Field.getText().trim());
                    double fps4k = Double.parseDouble(gpuFps4kField.getText().trim());
                    created = new Gpu(null, brand, name, price, tdp, vram, boardPower, recPsu, fps1080, fps1440, fps4k);
                }
                case MOTHERBOARD -> {
                    String socket = moboSocketField.getText().trim();
                    String ramType = moboRamField.getText().trim();
                    if (socket.isEmpty() || ramType.isEmpty()) {
                        throw new IllegalArgumentException("Motherboard socket and RAM support are required.");
                    }
                    created = new Motherboard(null, brand, name, price, tdp, socket, ramType);
                }
                case RAM -> {
                    String ramType = ramTypeField.getText().trim();
                    int capacity = Integer.parseInt(ramCapacityField.getText().trim());
                    int speed = Integer.parseInt(ramSpeedField.getText().trim());
                    if (ramType.isEmpty()) throw new IllegalArgumentException("RAM standard is required.");
                    created = new Ram(null, brand, name, price, tdp, ramType, capacity, speed);
                }
                case PSU -> {
                    int wattage = Integer.parseInt(psuWattageField.getText().trim());
                    String rating = psuRatingField.getText().trim();
                    created = new Psu(null, brand, name, price, tdp, wattage, rating);
                }
                default -> throw new IllegalStateException("Unexpected component type: " + type);
            }

            componentDao.save(created);
            componentAdded = true;
            dialogStage.close();
        } catch (NumberFormatException nfe) {
            errorLabel.setText("Please enter valid numeric values for price, TDP, and hardware specifications.");
        } catch (IllegalArgumentException iae) {
            errorLabel.setText(iae.getMessage());
        } catch (Exception e) {
            errorLabel.setText("Error saving component: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}
