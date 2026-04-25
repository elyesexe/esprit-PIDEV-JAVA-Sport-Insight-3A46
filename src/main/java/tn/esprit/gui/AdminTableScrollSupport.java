package tn.esprit.gui;

import javafx.geometry.Orientation;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableView;
import javafx.scene.input.ScrollEvent;

public final class AdminTableScrollSupport {
    private static final double MIN_HEIGHT = 320;
    private static final double PREF_HEIGHT = 430;
    private static final double FIXED_ROW_HEIGHT = 56;
    private static final String STYLE_CLASS = "admin-two-axis-table";

    private AdminTableScrollSupport() {
    }

    public static void enable(TableView<?> tableView) {
        if (tableView == null) {
            return;
        }

        tableView.setMinHeight(MIN_HEIGHT);
        tableView.setPrefHeight(PREF_HEIGHT);
        tableView.setMaxHeight(PREF_HEIGHT);
        tableView.setFixedCellSize(FIXED_ROW_HEIGHT);
        if (!tableView.getStyleClass().contains(STYLE_CLASS)) {
            tableView.getStyleClass().add(STYLE_CLASS);
        }

        tableView.addEventFilter(ScrollEvent.SCROLL, event -> {
            boolean horizontalGesture = Math.abs(event.getDeltaX()) > Math.abs(event.getDeltaY());
            boolean shiftWheel = event.isShiftDown() && Math.abs(event.getDeltaY()) > 0;
            if (!horizontalGesture && !shiftWheel) {
                return;
            }

            ScrollBar horizontalBar = findScrollBar(tableView, Orientation.HORIZONTAL);
            if (horizontalBar == null || !horizontalBar.isVisible() || horizontalBar.isDisabled()) {
                return;
            }

            double delta = horizontalGesture ? event.getDeltaX() : event.getDeltaY();
            horizontalBar.setValue(clamp(horizontalBar.getValue() - delta, horizontalBar.getMin(), horizontalBar.getMax()));
            event.consume();
        });
    }

    private static ScrollBar findScrollBar(TableView<?> tableView, Orientation orientation) {
        return tableView.lookupAll(".scroll-bar").stream()
                .filter(ScrollBar.class::isInstance)
                .map(ScrollBar.class::cast)
                .filter(scrollBar -> scrollBar.getOrientation() == orientation)
                .findFirst()
                .orElse(null);
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
