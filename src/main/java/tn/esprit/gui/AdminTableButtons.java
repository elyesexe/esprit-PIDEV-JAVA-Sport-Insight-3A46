package tn.esprit.gui;

import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;

public final class AdminTableButtons {
    private static final String TRASH_ICON_RESOURCE = "/tn/esprit/icons/trash.png";
    private static final double TRASH_ICON_SIZE = 46;
    private static final double BUTTON_SIZE = 56;

    private AdminTableButtons() {
    }

    public static Button createTrashButton() {
        Button button = new Button();
        button.setMnemonicParsing(false);
        button.setFocusTraversable(false);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setAccessibleText("Supprimer");
        button.getStyleClass().add("table-row-delete-icon-button");
        button.setMinSize(BUTTON_SIZE, BUTTON_SIZE);
        button.setPrefSize(BUTTON_SIZE, BUTTON_SIZE);
        button.setMaxSize(BUTTON_SIZE, BUTTON_SIZE);

        Image image = loadTrashIcon();
        if (image == null) {
            button.setText("Supprimer");
            return button;
        }

        ImageView iconView = new ImageView(image);
        iconView.setFitWidth(TRASH_ICON_SIZE);
        iconView.setFitHeight(TRASH_ICON_SIZE);
        iconView.setPreserveRatio(true);
        iconView.setSmooth(true);
        button.setGraphic(iconView);
        return button;
    }

    private static Image loadTrashIcon() {
        URL resource = AdminTableButtons.class.getResource(TRASH_ICON_RESOURCE);
        if (resource == null) {
            return null;
        }
        return new Image(resource.toExternalForm(), TRASH_ICON_SIZE, TRASH_ICON_SIZE, true, true);
    }
}
