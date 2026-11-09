package com.example.api.salida;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.core.salida.CasosDeUsoFichaOperativa;
import com.example.core.salida.CasosDeUsoSalidas;
import com.example.core.salida.Salida;

/** Entrega el PDF de la Ficha operativa que genera core; no lo compone ni agrega fichas médicas. */
@RestController
@RequestMapping("/api/salidas/{id}/ficha-operativa")
public class FichaOperativaController {

    private final CasosDeUsoFichaOperativa fichaOperativa;
    private final CasosDeUsoSalidas salidas;

    public FichaOperativaController(CasosDeUsoFichaOperativa fichaOperativa, CasosDeUsoSalidas salidas) {
        this.fichaOperativa = fichaOperativa;
        this.salidas = salidas;
    }

    @GetMapping
    public ResponseEntity<byte[]> descargar(@PathVariable int id) {
        Salida salida = salidas.obtener(id);
        byte[] pdf = fichaOperativa.generar(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombreArchivo(salida), StandardCharsets.UTF_8)
                                .build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private static String nombreArchivo(Salida salida) {
        String ruta = salida.rutaNombre().trim().replaceAll("[^\\p{L}\\p{N}._-]+", "-").replaceAll("^-+|-+$", "");
        if (ruta.isBlank()) ruta = "Ruta";
        return ruta + "-" + salida.fecha() + "-salida-" + salida.id() + ".pdf";
    }
}
