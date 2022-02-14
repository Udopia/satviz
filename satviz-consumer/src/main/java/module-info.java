module edu.kit.satviz.consumer {

  requires com.fasterxml.jackson.databind;
  requires edu.kit.satviz.network;
  requires edu.kit.satviz.serial;
  requires edu.kit.satviz.parsers;
  requires net.sourceforge.argparse4j;
  requires javafx.base;
  requires javafx.fxml;
  requires javafx.controls;

  requires jdk.incubator.foreign;

  opens edu.kit.satviz.consumer.config;

}