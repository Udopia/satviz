package edu.kit.satviz.consumer.gui.config;

import edu.kit.satviz.consumer.config.ConsumerConfig;
import edu.kit.satviz.consumer.config.EmbeddedModeConfig;
import edu.kit.satviz.consumer.config.EmbeddedModeSource;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

import java.io.File;

public class EmbeddedConfigController extends ConfigController {

  // ATTRIBUTES (FXML)

  @FXML
  private ChoiceBox<EmbeddedModeSource> producerModeChoiceBox;
  @FXML
  private Button producerModeFileButton;
  @FXML
  private Label producerModeFileLabel;

  // ATTRIBUTES (OTHER)

  private File producerModeFile;


  // METHODS (FXML)

  @FXML
  private void initialize() {
    producerModeChoiceBox.setItems(FXCollections.observableArrayList(EmbeddedModeSource.values()));
    producerModeChoiceBox.setValue(EmbeddedModeConfig.DEFAULT_EMBEDDED_MODE_SOURCE);
  }

  @FXML
  private void selectProducerModeFile() {

  }

  // METHODS (OTHER)

  @Override
  protected void run() throws ConfigArgumentException {
    EmbeddedModeConfig modeConfig = new EmbeddedModeConfig();
    modeConfig.setSource(producerModeChoiceBox.getValue());
    if (producerModeFile == null) {
      throw new ConfigArgumentException("Please select a valid "
          + producerModeChoiceBox.getValue().name() + " file");
    }
    modeConfig.setSourcePath(producerModeFile.toPath());

    ConsumerConfig config = new ConsumerConfig();
    config.setModeConfig(modeConfig);

    setConsumerConfig(config);
  }

}
