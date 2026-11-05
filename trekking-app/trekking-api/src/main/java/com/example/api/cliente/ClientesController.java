package com.example.api.cliente;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.api.compartido.EstadoFiltro;
import com.example.api.error.CuerpoError.ErrorCampoDto;
import com.example.core.cliente.CasosDeUsoClientes;
import com.example.core.cliente.Cliente;
import com.example.core.cliente.DatosCliente;

@RestController
@RequestMapping("/api/clientes")
public class ClientesController {

    private final CasosDeUsoClientes clientes;

    public ClientesController(CasosDeUsoClientes clientes) {
        this.clientes = clientes;
    }

    @GetMapping
    public List<ClienteDto> listar(@RequestParam(defaultValue = "activas") EstadoFiltro estado) {
        return clientes.listar().stream()
                .filter(c -> switch (estado) {
                    case activas -> c.activo();
                    case bajas -> !c.activo();
                    case todas -> true;
                })
                .map(this::dto)
                .toList();
    }

    @GetMapping("/{id}")
    public ClienteDto obtener(@PathVariable int id) {
        return dto(clientes.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteDto crear(@RequestBody DatosCliente datos) {
        return dto(clientes.alta(datos));
    }

    @PutMapping("/{id}")
    public ClienteDto editar(@PathVariable int id, @RequestBody DatosCliente datos) {
        return dto(clientes.editar(id, datos));
    }

    /** Valida sin guardar (no consulta DNI duplicado); lista vacía si no hay errores. */
    @PostMapping("/validar")
    public List<ErrorCampoDto> validar(@RequestBody DatosCliente datos) {
        return clientes.validar(datos).stream().map(e -> new ErrorCampoDto(e.campo(), e.mensaje())).toList();
    }

    @PostMapping("/{id}/baja")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void darDeBaja(@PathVariable int id) {
        clientes.darDeBaja(id);
    }

    @PostMapping("/{id}/reactivacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivar(@PathVariable int id) {
        clientes.reactivar(id);
    }

    private ClienteDto dto(Cliente c) {
        return ClienteDto.de(c, clientes.esMenor(c.fechaNacimiento()));
    }
}
