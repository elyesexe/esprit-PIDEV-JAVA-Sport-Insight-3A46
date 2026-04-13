module untitled {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires itextpdf;

    opens tn.esprit.mains       to javafx.graphics;
    opens tn.esprit.controllers to javafx.fxml;
    opens tn.esprit.entities    to javafx.base, javafx.fxml;

    exports tn.esprit.mains;
    exports tn.esprit.controllers;
    exports tn.esprit.entities;
    exports tn.esprit.services;
    exports tn.esprit.tools;
}
