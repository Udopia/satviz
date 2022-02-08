package edu.kit.satviz.consumer.gui.config;

import edu.kit.satviz.consumer.config.ConsumerConfig;
import edu.kit.satviz.consumer.config.ConsumerMode;
import edu.kit.satviz.consumer.config.HeatmapColors;
import edu.kit.satviz.consumer.config.WeightFactor;
import java.io.File;
import java.io.IOException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

public class GeneralConfigController extends ConfigController {

  // ATTRIBUTES (FXML)

  @FXML
  private Button loadSettingsButton;
  @FXML
  private Button saveSettingsButton;
  @FXML
  private Button recordingFileButton;
  @FXML
  private Label recordingFileLabel;
  @FXML
  private CheckBox showLiveVisualizationCheckBox;
  @FXML
  private CheckBox recordFromStartCheckBox;
  @FXML
  private ChoiceBox<WeightFactor> weightFactorChoiceBox;
  @FXML
  private Spinner<Integer> windowSizeSpinner;
  @FXML
  private ColorPicker coldColorColorPicker;
  @FXML
  private ColorPicker hotColorColorPicker;
  @FXML
  private Button satInstanceFileButton;
  @FXML
  private Label satInstanceFileLabel;
  @FXML
  private ChoiceBox<ConsumerMode> modeChoiceBox;
  @FXML
  private VBox modeVbox;
  @FXML
  private Label errorLabel;
  @FXML
  private Button runButton;


  // ATTRIBUTES (OTHER)

  private ConfigController modeConfigController;
  private String recordingFile;
  private File satInstanceFile;

  private ConsumerConfig config;


  // METHODS (FXML)

  @FXML
  private void initialize() {
    recordingFileLabel.setText(ConsumerConfig.DEFAULT_VIDEO_TEMPLATE_PATH);

    weightFactorChoiceBox.setItems(FXCollections.observableArrayList(WeightFactor.values()));
    weightFactorChoiceBox.setValue(ConsumerConfig.DEFAULT_WEIGHT_FACTOR);

    initializeWindowSizeSpinner();

    coldColorColorPicker.setValue(intToColor(HeatmapColors.DEFAULT_FROM_COLOR));
    hotColorColorPicker.setValue(intToColor(HeatmapColors.DEFAULT_TO_COLOR));

    modeChoiceBox.setItems(FXCollections.observableArrayList(ConsumerMode.values()));
    modeChoiceBox.setValue(ConsumerConfig.DEFAULT_CONSUMER_MODE);
  }

  private void initializeWindowSizeSpinner() {
    SpinnerValueFactory<Integer> windowSizeSpinnerValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
        ConsumerConfig.MIN_WINDOW_SIZE, ConsumerConfig.MAX_WINDOW_SIZE, ConsumerConfig.DEFAULT_WINDOW_SIZE);

    // catch exception when (value is null & (enter/arrow up/arrow down) is pressed)
    windowSizeSpinnerValueFactory.setConverter(new StringConverter<>() {
      @Override
      public String toString(Integer object) {
        return object.toString();
      }

      @Override
      public Integer fromString(String string) {
        try {
          return Integer.parseInt(string);
        } catch (NumberFormatException e) {
          windowSizeSpinner.getEditor().setText("" + ConsumerConfig.DEFAULT_WINDOW_SIZE);
          return ConsumerConfig.DEFAULT_WINDOW_SIZE;
        }
      }
    });

    windowSizeSpinner.setValueFactory(windowSizeSpinnerValueFactory);

    windowSizeSpinner.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue.equals("")) {
        try {
          Integer.parseInt(newValue);
        } catch (NumberFormatException e) {
          windowSizeSpinner.getEditor().setText(oldValue);
        }
      }
    });
  }

  @FXML
  private void loadSettings() {

  }

  @FXML
  private void saveSettings() {

  }

  @FXML
  private void selectRecordingFile() {
    FileChooser fileChooser = new FileChooser();
    FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Video Files", "*.ogv");
    fileChooser.getExtensionFilters().add(filter);

    File file = fileChooser.showSaveDialog(null);
    if (file != null) {
      recordingFile = file.getAbsolutePath();
      recordingFileLabel.setText(file.getName());
    }
  }

  @FXML
  private void setLiveVisualization() {
    if (showLiveVisualizationCheckBox.isSelected()) {
      recordFromStartCheckBox.setDisable(false);
    } else {
      recordFromStartCheckBox.setDisable(true);
      recordFromStartCheckBox.setSelected(true);
    }
  }

  @FXML
  private void selectSatInstanceFile() {
    FileChooser fileChooser = new FileChooser();
    FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("SAT Instance", "*.cnf");
    fileChooser.getExtensionFilters().add(filter);

    File file = fileChooser.showOpenDialog(null);
    if (file != null) {
      satInstanceFile = file;
      satInstanceFileLabel.setText(file.getName());
    }
  }

  @FXML
  private void updateMode() {
    // retrieve name of new fxml-file for mode specific input
    String modeString = modeChoiceBox.getValue().toString().toLowerCase() + "-config.fxml";
    // set vbox content to fxml-file for mode specific input
    FXMLLoader loader = new FXMLLoader(getClass().getResource(modeString));
    modeVbox.getChildren().clear();
    try {
      modeVbox.getChildren().add(loader.load());
    } catch (IOException e) {
      e.printStackTrace();
    }
    // remember controller of current mode
    modeConfigController = loader.getController();
  }

  @FXML
  private void run() {
    try {
      config = createConsumerConfig();
      Platform.exit();
    } catch (ConfigArgumentException e) {
      errorLabel.setText(e.getMessage());
    }
  }

  // METHODS (OTHER)

  @Override
  protected ConsumerConfig createConsumerConfig() throws ConfigArgumentException {
    return null;
  }

  public ConsumerConfig getConsumerConfig() {
    return config;
  }

  private Color intToColor(int color) {
    int red = (color >>> 16) & 0xFF;
    int green = (color >>> 8) & 0xFF;
    int blue = color & 0xFF;
    return new Color(red / 255.0, green / 255.0, blue / 255.0, 1.0);
  }

  private int colorToInt(Color color) {
    int red = (int) Math.round(color.getRed() * 255);
    int green = (int) Math.round(color.getGreen() * 255);
    int blue = (int) Math.round(color.getBlue() * 255);
    return (red << 16) | (green << 8) | blue;
  }

}
