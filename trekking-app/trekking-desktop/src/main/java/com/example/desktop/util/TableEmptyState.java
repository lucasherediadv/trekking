package com.example.desktop.util;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public final class TableEmptyState {
    private TableEmptyState() { }

    public static Node create(String title, String description, String actionText,
            EventHandler<ActionEvent> action) {
        return create(null, title, description, actionText, action);
    }

    public static Node create(String iconFileName, String title, String description,
            String actionText, EventHandler<ActionEvent> action) {
        Node icon;
        if (iconFileName == null || iconFileName.isBlank()) {
            Label defaultIcon = new Label("▤");
            defaultIcon.getStyleClass().add("table-empty-icon");
            icon = defaultIcon;
        } else {
            var graphic = SidebarIcon.load(iconFileName + ".svg");
            graphic.setScaleX(1.7);
            graphic.setScaleY(1.7);
            graphic.getStyleClass().add("table-empty-icon");
            icon = graphic;
        }
        Label heading = new Label(title);
        heading.getStyleClass().add("table-empty-title");
        heading.setAlignment(Pos.CENTER);
        Label detail = new Label(description);
        detail.getStyleClass().add("table-empty-description");
        detail.setWrapText(true);
        detail.setMaxWidth(420);
        detail.setAlignment(Pos.CENTER);
        detail.setTextAlignment(TextAlignment.CENTER);

        VBox content = new VBox(8, icon, heading, detail);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("table-empty-state");
        if (action != null && actionText != null && !actionText.isBlank()) {
            Button button = new Button(actionText);
            button.setOnAction(action);
            button.getStyleClass().add("table-empty-action");
            content.getChildren().add(button);
        }
        return content;
    }

    public static void update(TableView<?> table, boolean hasRecords, boolean filtersActive,
            String emptyTitle, String emptyDescription,
            String noResultsTitle, String noResultsDescription,
            String actionText, EventHandler<ActionEvent> action) {
        update(table, hasRecords, filtersActive, emptyTitle, emptyDescription,
                noResultsTitle, noResultsDescription, null, actionText, action);
    }

    public static void update(TableView<?> table, boolean hasRecords, boolean filtersActive,
            String emptyTitle, String emptyDescription,
            String noResultsTitle, String noResultsDescription,
            String iconFileName, String actionText, EventHandler<ActionEvent> action) {
        boolean noMatches = hasRecords && filtersActive;
        table.setPlaceholder(create(iconFileName,
                noMatches ? noResultsTitle : emptyTitle,
                noMatches ? noResultsDescription : emptyDescription,
                actionText,
                action));
    }
}
