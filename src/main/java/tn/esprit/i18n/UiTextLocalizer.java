package tn.esprit.i18n;

import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Control;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.Pane;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class UiTextLocalizer {
    private static final Set<Object> INSTALLED = Collections.newSetFromMap(new WeakHashMap<>());

    private UiTextLocalizer() {
    }

    public static void install(Parent root) {
        if (root == null) {
            return;
        }
        localizeNode(root);
    }

    private static void localizeNode(Node node) {
        if (node == null) {
            return;
        }

        if (node instanceof Labeled labeled) {
            bindStringProperty(labeled.textProperty());
        }

        if (node instanceof TextInputControl textInputControl) {
            bindStringProperty(textInputControl.promptTextProperty());
        }

        if (node instanceof ComboBoxBase<?> comboBoxBase) {
            bindStringProperty(comboBoxBase.promptTextProperty());
        }

        if (node instanceof Control control && control.getTooltip() != null) {
            localizeTooltip(control.getTooltip());
        }

        if (node instanceof TabPane tabPane) {
            installTabs(tabPane);
        }

        if (node instanceof TableView<?> tableView) {
            for (TableColumn<?, ?> column : tableView.getColumns()) {
                localizeTableColumn(column);
            }
        }

        if (node instanceof TreeTableView<?> treeTableView) {
            for (TreeTableColumn<?, ?> column : treeTableView.getColumns()) {
                localizeTreeTableColumn(column);
            }
        }

        if (node instanceof MenuButton menuButton) {
            installMenuItems(menuButton.getItems());
        }

        if (node instanceof SplitMenuButton splitMenuButton) {
            installMenuItems(splitMenuButton.getItems());
        }

        if (node instanceof TitledPane titledPane) {
            bindStringProperty(titledPane.textProperty());
            if (titledPane.getContent() != null) {
                localizeNode(titledPane.getContent());
            }
        }

        if (node instanceof Parent parent) {
            installChildren(parent);
        }
    }

    private static void installChildren(Parent parent) {
        if (parent == null || !INSTALLED.add(parent)) {
            return;
        }
        parent.getChildrenUnmodifiable().forEach(UiTextLocalizer::localizeNode);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Node added : change.getAddedSubList()) {
                        localizeNode(added);
                    }
                }
            }
        });

        if (parent instanceof Pane pane) {
            pane.getChildren().forEach(UiTextLocalizer::localizeNode);
        }
    }

    private static void installTabs(TabPane tabPane) {
        if (tabPane == null || !INSTALLED.add(tabPane.getTabs())) {
            return;
        }
        for (Tab tab : tabPane.getTabs()) {
            localizeTab(tab);
        }
        tabPane.getTabs().addListener((ListChangeListener<Tab>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Tab tab : change.getAddedSubList()) {
                        localizeTab(tab);
                    }
                }
            }
        });
    }

    private static void localizeTab(Tab tab) {
        if (tab == null) {
            return;
        }
        bindStringProperty(tab.textProperty());
        if (tab.getContent() != null) {
            localizeNode(tab.getContent());
        }
    }

    private static void installMenuItems(javafx.collections.ObservableList<MenuItem> items) {
        if (items == null || !INSTALLED.add(items)) {
            return;
        }
        for (MenuItem item : items) {
            localizeMenuItem(item);
        }
        items.addListener((ListChangeListener<MenuItem>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (MenuItem item : change.getAddedSubList()) {
                        localizeMenuItem(item);
                    }
                }
            }
        });
    }

    private static void localizeMenuItem(MenuItem item) {
        if (item == null) {
            return;
        }
        bindStringProperty(item.textProperty());
    }

    private static void localizeTooltip(Tooltip tooltip) {
        if (tooltip == null) {
            return;
        }
        bindStringProperty(tooltip.textProperty());
    }

    private static void localizeTableColumn(TableColumn<?, ?> column) {
        if (column == null) {
            return;
        }
        bindStringProperty(column.textProperty());
        for (TableColumn<?, ?> nestedColumn : column.getColumns()) {
            localizeTableColumn(nestedColumn);
        }
    }

    private static void localizeTreeTableColumn(TreeTableColumn<?, ?> column) {
        if (column == null) {
            return;
        }
        bindStringProperty(column.textProperty());
        for (TreeTableColumn<?, ?> nestedColumn : column.getColumns()) {
            localizeTreeTableColumn(nestedColumn);
        }
    }

    private static void bindStringProperty(StringProperty property) {
        if (property == null) {
            return;
        }
        localizeProperty(property);
        if (!INSTALLED.add(property)) {
            return;
        }
        property.addListener((observable, oldValue, newValue) -> localizeProperty(property));
    }

    private static void localizeProperty(StringProperty property) {
        String currentValue = property.get();
        String localizedValue = I18n.translateLiteral(currentValue);
        if (localizedValue != null && !localizedValue.equals(currentValue)) {
            property.set(localizedValue);
        }
    }
}
