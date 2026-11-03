package com.example.desktop.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;

import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

/** Formato de fecha compartido por formularios y listados. */
public final class FechaFormato {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter
            .ofPattern("dd/MM/uuuu", Locale.forLanguageTag("es-AR"))
            .withResolverStyle(ResolverStyle.STRICT);

    private FechaFormato() { }

    public static String formatear(LocalDate fecha) {
        return fecha == null ? "" : FORMATO.format(fecha);
    }

    public static void configurar(DatePicker selector) {
        selector.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate fecha) {
                return formatear(fecha);
            }

            @Override
            public LocalDate fromString(String texto) {
                if (texto == null || texto.isBlank()) return null;
                return LocalDate.parse(texto.trim(), FORMATO);
            }
        });
        selector.setPromptText("dd/mm/aaaa");
    }
}
