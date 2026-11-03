package com.example.desktop.util;

import javafx.scene.control.TableView;

public final class TableHeight {
    private static final double HEADER_HEIGHT = 40;
    private static final double ROW_HEIGHT = 36;

    private TableHeight() { }

    public static void update(TableView<?> table, int visibleRows, double emptyHeight) {
        table.setFixedCellSize(ROW_HEIGHT);
        double height = visibleRows > 0
                ? HEADER_HEIGHT + visibleRows * ROW_HEIGHT + 2
                : emptyHeight;
        table.setPrefHeight(height);
    }
}
