package com.example.api.cliente;

import java.io.IOException;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.example.api.config.PropiedadesApi;
import com.example.core.cliente.CasosDeUsoFichasMedicas;
import com.example.core.excepcion.EntidadNoEncontradaException;

@RestController
@RequestMapping("/api/clientes/{id}/ficha-medica")
public class FichasMedicasController {

    private final CasosDeUsoFichasMedicas fichas;
    private final PropiedadesApi propiedades;

    public FichasMedicasController(CasosDeUsoFichasMedicas fichas, PropiedadesApi propiedades) {
        this.fichas = fichas;
        this.propiedades = propiedades;
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void adjuntar(@PathVariable int id, @RequestParam("archivo") MultipartFile archivo) throws IOException {
        long maximo = propiedades.maxFichaMedica().toBytes();
        if (archivo.getSize() > maximo) throw new MaxUploadSizeExceededException(maximo);
        fichas.adjuntar(id, archivo.getOriginalFilename(), archivo.getBytes());
    }

    @GetMapping
    public ResponseEntity<byte[]> descargar(@PathVariable int id) {
        var ficha = fichas.consultar(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Ficha médica del cliente", id));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(ficha.nombreArchivo(),
                                java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(ficha.contenido());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitar(@PathVariable int id) {
        fichas.quitar(id);
    }
}
