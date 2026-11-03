package com.example.desktop.util;

import java.util.IdentityHashMap;
import java.util.Map;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.scene.layout.HBox;
import javafx.beans.value.ChangeListener;

/** Shared layout for right-aligned action buttons in table rows. */
public final class RowActionCell {
    private static final Object VISIBILITY_TRACKER_KEY = RowActionCell.class;

    private RowActionCell() { }

    public static void showCentered(TableCell<?, ?> cell, HBox actions) {
        detachVisibilityTracker(cell);
        if (!cell.getStyleClass().contains("row-actions-cell")) {
            cell.getStyleClass().add("row-actions-cell");
        }
        actions.getStyleClass().add("row-actions");
        actions.setMaxWidth(Double.MAX_VALUE);
        actions.setAlignment(Pos.CENTER_RIGHT);
        cell.setAlignment(Pos.CENTER_RIGHT);
        cell.setGraphic(actions);

        TableRow<?> row = cell.getTableRow();
        if (row != null) {
            VisibilityTracker tracker = new VisibilityTracker(row, actions);
            cell.getProperties().put(VISIBILITY_TRACKER_KEY, tracker);
        }
    }

    public static void clear(TableCell<?, ?> cell) {
        detachVisibilityTracker(cell);
        cell.setGraphic(null);
    }

    private static void detachVisibilityTracker(TableCell<?, ?> cell) {
        Object tracker = cell.getProperties().remove(VISIBILITY_TRACKER_KEY);
        if (tracker instanceof VisibilityTracker visibilityTracker) {
            visibilityTracker.dispose();
        }
    }

    private static final class VisibilityTracker {
        private final TableRow<?> row;
        private final HBox actions;
        private final ChangeListener<Boolean> rowStateListener = (observable, previous, current) -> update();
        private final Map<Node, ChangeListener<Boolean>> focusListeners = new IdentityHashMap<>();

        private VisibilityTracker(TableRow<?> row, HBox actions) {
            this.row = row;
            this.actions = actions;
            row.hoverProperty().addListener(rowStateListener);
            row.selectedProperty().addListener(rowStateListener);
            for (Node child : actions.getChildren()) {
                ChangeListener<Boolean> focusListener = (observable, previous, current) -> update();
                child.focusedProperty().addListener(focusListener);
                focusListeners.put(child, focusListener);
            }
            update();
        }

        private void update() {
            boolean focusedAction = focusListeners.keySet().stream().anyMatch(Node::isFocused);
            boolean visible = row.isHover() || row.isSelected() || focusedAction;
            actions.setOpacity(visible ? 1.0 : 0.0);
            actions.setMouseTransparent(!visible);
        }

        private void dispose() {
            row.hoverProperty().removeListener(rowStateListener);
            row.selectedProperty().removeListener(rowStateListener);
            focusListeners.forEach((node, listener) -> node.focusedProperty().removeListener(listener));
            actions.setOpacity(1.0);
            actions.setMouseTransparent(false);
            focusListeners.clear();
        }
    }
}
