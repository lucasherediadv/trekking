# 13: Documentación y convivencia con el escritorio

**What to build:** La API queda documentada para quien trabaje en el proyecto y se valida a mano que puede convivir con la app de escritorio sobre el mismo archivo de base de datos, que es el supuesto de esta etapa (ADR 0006).

**Blocked by:** 10, 11, 12

**Status:** ready-for-agent

- [ ] `AGENTS.md` refleja el módulo `trekking-api`: dependencias permitidas, que no contiene reglas de negocio ni SQL, el comando para ejecutar la API y las propiedades de configuración (`trekking.db`, puerto, `trekking.api.origen-web`, tamaño de ficha).
- [ ] Validación manual documentada en un comentario de este ticket: con la API y la app JavaFX abiertas sobre la misma base, una escritura de cada lado se ve del otro, y dos confirmaciones concurrentes de Salida sobre el mismo elemento no sobrevenden stock.
- [ ] Se verifica a mano que la API sin origen CORS configurado no acepta orígenes cruzados y que solo escucha en `127.0.0.1` por defecto.
- [ ] `mvn -f trekking-app/pom.xml clean package` pasa con los cuatro módulos.
- [ ] El `Status` del spec de la API REST pasa a `closed`.
