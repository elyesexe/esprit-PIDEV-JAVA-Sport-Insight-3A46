package tn.esprit.assistant;

import javafx.stage.Stage;

@FunctionalInterface
public interface AssistantCommand {
    void execute(Stage stage);
}
