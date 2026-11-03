package com.example.desktop.util;

import java.util.function.Function;

import javafx.scene.control.TableCell;

/** Table cell that shows a value as semantic-color text; unknown values stay uncolored. */
public final class EstadoCell {
    public enum Tono {
        EXITO("estado-exito"), ADVERTENCIA("estado-advertencia"), PELIGRO("estado-peligro"),
        INFORMACION("estado-informacion"), NEUTRO("estado-neutro");

        private final String styleClass;

        Tono(String styleClass) { this.styleClass = styleClass; }
    }

    private EstadoCell() { }

    public static <S> TableCell<S, String> create(Function<String, Tono> tonoPorValor) {
        return create(tonoPorValor, Function.identity());
    }

    public static <S, T> TableCell<S, T> create(Function<T, Tono> tonoPorValor, Function<T, String> texto) {
        return new TableCell<>() {
            @Override protected void updateItem(T valor, boolean empty) {
                super.updateItem(valor, empty);
                for (Tono t : Tono.values()) getStyleClass().remove(t.styleClass);
                setText(null);
                if (empty || valor == null) return;
                setText(texto.apply(valor));
                Tono tono = tonoPorValor.apply(valor);
                if (tono != null) getStyleClass().add(tono.styleClass);
            }
        };
    }
}
