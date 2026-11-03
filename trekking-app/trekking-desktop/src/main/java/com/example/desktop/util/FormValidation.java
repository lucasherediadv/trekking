package com.example.desktop.util;

import java.util.function.Supplier;

import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

public final class FormValidation {

    private static final PseudoClass INVALID =
            PseudoClass.getPseudoClass("invalid");
    private static final String STATE_KEY =
            FormValidation.class.getName() + ".state";

    private FormValidation() {
    }

    public static void watch(
            Control control,
            ObservableValue<?> value,
            Supplier<String> errorMessage) {
        ValidationState state = new ValidationState(control, errorMessage);
        control.getProperties().put(STATE_KEY, state);

        value.addListener((observable, previous, current) -> {
            state.markInteracted();
            state.update();
        });

        control.focusedProperty().addListener((observable, wasFocused, focused) -> {
            if (!focused && wasFocused) {
                state.markInteracted();
                state.update();
            }
        });
    }

    public static boolean validateNow(Control control) {
        Object value = control.getProperties().get(STATE_KEY);
        if (!(value instanceof ValidationState state)) {
            return true;
        }
        state.markInteracted();
        return state.update();
    }

    public static void showError(Control control, String message) {
        control.pseudoClassStateChanged(INVALID, true);
        ValidationState state = getOrCreateState(control);
        Tooltip tooltip = new Tooltip(message);
        control.setTooltip(tooltip);
    }

    public static void clearError(Control control) {
        control.pseudoClassStateChanged(INVALID, false);
        Object value = control.getProperties().get(STATE_KEY);
        if (value instanceof ValidationState state) {
            control.setTooltip(state.originalTooltip);
        }
    }

    public static void reset(Control control) {
        Object value = control.getProperties().get(STATE_KEY);
        if (!(value instanceof ValidationState state)) {
            return;
        }
        control.pseudoClassStateChanged(INVALID, false);
        control.setTooltip(state.originalTooltip);
    }

    private static ValidationState getOrCreateState(Control control) {
        Object current = control.getProperties().get(STATE_KEY);
        if (current instanceof ValidationState state) {
            return state;
        }
        ValidationState state = new ValidationState(control, () -> null);
        control.getProperties().put(STATE_KEY, state);
        return state;
    }

    private static final class ValidationState {

        private final Control control;
        private final Supplier<String> errorMessage;
        private final Tooltip originalTooltip;
        private final Label inlineError;
        private boolean interacted;

        private ValidationState(
                Control control,
                Supplier<String> errorMessage) {
            this.control = control;
            this.errorMessage = errorMessage;
            this.originalTooltip = control.getTooltip();
            this.inlineError = crearErrorInline(control);
        }

        private void markInteracted() {
            interacted = true;
        }

        private boolean update() {
            String message = errorMessage.get();
            if (inlineError != null) {
                boolean show = interacted && message != null && !message.isBlank();
                inlineError.setText(show ? message : "");
                inlineError.setVisible(show);
                inlineError.setManaged(show);
            }
            if (message == null || message.isBlank()) {
                control.pseudoClassStateChanged(INVALID, false);
                control.setTooltip(originalTooltip);
                return true;
            }
            control.pseudoClassStateChanged(INVALID, true);
            control.setTooltip(new Tooltip(message));
            return false;
        }

        private static Label crearErrorInline(Control control) {
            if (!(control.getParent() instanceof VBox parent)) {
                return null;
            }
            Label label = new Label();
            label.getStyleClass().add("field-error-message");
            label.setWrapText(true);
            label.setVisible(false);
            label.setManaged(false);
            int index = parent.getChildren().indexOf(control);
            parent.getChildren().add(Math.min(index + 1, parent.getChildren().size()), label);
            return label;
        }
    }
}
