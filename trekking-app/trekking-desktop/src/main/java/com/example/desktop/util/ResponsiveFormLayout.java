package com.example.desktop.util;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.layout.GridPane;

/** Keeps paired form fields side by side on wide screens and stacks them when narrow. */
public final class ResponsiveFormLayout {

    private static final double STACK_THRESHOLD = 600;

    private ResponsiveFormLayout() {
    }

    public static void adapt(GridPane grid) {
        List<Position> positions = new ArrayList<>();
        for (Node child : grid.getChildren()) {
            Integer column = GridPane.getColumnIndex(child);
            Integer row = GridPane.getRowIndex(child);
            Integer columnSpan = GridPane.getColumnSpan(child);
            positions.add(new Position(child, column == null ? 0 : column,
                    row == null ? 0 : row, columnSpan == null ? 1 : columnSpan));
        }
        Runnable update = () -> {
            boolean stacked = grid.getWidth() < STACK_THRESHOLD;
            int row = 0;
            for (Position position : positions) {
                GridPane.setColumnIndex(position.node(), stacked ? 0 : position.column());
                GridPane.setColumnSpan(position.node(), stacked ? 1 : position.columnSpan());
                GridPane.setRowIndex(position.node(), stacked ? row++ : position.row());
            }
        };
        grid.widthProperty().addListener((observable, previous, current) -> update.run());
        update.run();
    }

    private record Position(Node node, int column, int row, int columnSpan) { }
}
