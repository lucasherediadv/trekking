package com.example.desktop.util;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

public final class ComboBoxDisplayLabel {
    private ComboBoxDisplayLabel() { }

    public static void prefixValue(ComboBox<String> comboBox, String label) {
        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : label + ": " + (item == null ? "" : item));
            }
        });
    }
}
