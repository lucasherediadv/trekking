#!/usr/bin/env python3
"""Carga aditiva de salidas de trekking en Córdoba para Trekking App (Python 3 stdlib)."""
from __future__ import annotations

import argparse
import datetime as dt
from pathlib import Path
import sqlite3
import subprocess
import sys
import unicodedata
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
APP = ROOT / "trekking-app"
DEFAULT_DB = ROOT / "trekking.db"
MANIFEST = "__seed_owned"

ROUTES = [
    ("ruta-condorito", "Quebrada del Condorito", 2200, "Rocoso", "Media", "Media", 1),
    ("ruta-champaqui", "Cerro Champaquí desde Villa Alpina", 2790, "Mixto", "Alta", "Alta", 1),
    ("ruta-gigantes", "Cerro Los Gigantes", 2374, "Rocoso", "Alta", "Alta", 1),
    ("ruta-uritorco", "Cerro Uritorco", 1979, "Mixto", "Media", "Alta", 1),
    ("ruta-wank", "Cerro Wank y Cascada Almbach (La Cumbrecita)", 1715, "Boscoso", "Baja", "Baja", 1),
    ("ruta-pan-de-azucar", "Cerro Pan de Azúcar (Cosquín)", 1260, "Mixto", "Baja", "Media", 1),
    ("ruta-terrones", "Los Terrones (Capilla del Monte)", 1300, "Rocoso", "Baja", "Baja", 1),
    ("ruta-aspero", "Cerro Áspero (Alpa Corral)", 2200, "Mixto", "Media", "Alta", 1),
    ("ruta-colorado", "Cerro Colorado y pinturas rupestres", 700, "Sendero", "Baja", "Baja", 0),
]
# Coordenadas y altitudes aproximadas, tomadas como referencia de cada lugar.
POINTS = {
    "ruta-condorito": [("Portada del Parque Nacional", "08:00", -31.6667, -64.6444, 2100, "Centro de visitantes y control de ingreso al Parque Nacional Quebrada del Condorito."), ("Balcón Norte", "10:30", -31.6389, -64.6556, 2120, "Mirador sobre el borde norte de la quebrada."), ("Mirador del Cóndor", "13:00", -31.6444, -64.6361, 1900, "Vista a la garganta del Condorito y a la zona de nidificación de cóndores.")],
    "ruta-champaqui": [("Villa Alpina", "07:00", -31.9667, -64.8000, 1400, "Inicio del sendero, último punto con servicios."), ("Campamento base", "12:00", -31.9750, -64.8700, 2300, "Zona de vivac y descanso antes del ascenso final."), ("Cumbre del Champaquí", "16:00", -31.9833, -64.9333, 2790, "Punto más alto de la provincia de Córdoba.")],
    "ruta-gigantes": [("Playa de los Ríos", "08:00", -31.3667, -64.8167, 1900, "Acceso al macizo y punto de encuentro del grupo."), ("El Mogote", "12:00", -31.3833, -64.8333, 2300, "Afloramiento rocoso intermedio con vista a las gemelas."), ("Cumbre Cerro Los Gigantes", "15:00", -31.4000, -64.8500, 2374, "Cumbre del macizo de Los Gigantes.")],
    "ruta-uritorco": [("Ingreso a la Reserva", "08:00", -30.8328, -64.4944, 1070, "Control de ingreso y registro de visitantes en la base del cerro."), ("Paso del Indio", "11:00", -30.8367, -64.4850, 1600, "Tramo intermedio con vista al valle de Capilla del Monte."), ("Cumbre del Uritorco", "14:00", -30.8333, -64.4667, 1979, "Cumbre del cerro, con vista panorámica del Valle de Punilla.")],
    "ruta-wank": [("Centro de La Cumbrecita", "09:00", -31.8997, -64.7767, 1450, "Villa peatonal y punto de partida."), ("Cascada Almbach", "10:30", -31.8944, -64.7722, 1500, "Cascada sobre el río Almbach."), ("Mirador Cerro Wank", "12:30", -31.9000, -64.7500, 1715, "Mirador con vista a las sierras de Calamuchita.")],
    "ruta-pan-de-azucar": [("Plaza de Cosquín", "08:30", -31.2420, -64.4650, 720, "Punto de encuentro en el centro de Cosquín."), ("Base del Pan de Azúcar", "09:30", -31.2250, -64.4350, 900, "Inicio del sendero de ascenso."), ("Cumbre del Pan de Azúcar", "11:30", -31.2200, -64.4200, 1260, "Mirador natural sobre Cosquín y el Valle de Punilla.")],
    "ruta-terrones": [("Ingreso a Los Terrones", "09:00", -30.8900, -64.5500, 1000, "Acceso al paraje de formaciones rocosas rojizas."), ("Mirador Los Terrones", "11:00", -30.8883, -64.5617, 1300, "Vista a las formaciones erosionadas y al Cerro Uritorco.")],
    "ruta-aspero": [("Alpa Corral", "07:30", -32.6667, -64.7833, 800, "Pueblo de partida y último punto con servicios."), ("Paso intermedio", "11:00", -32.7000, -64.7500, 1500, "Tramo de transición entre el valle y la sierra alta."), ("Cumbre del Cerro Áspero", "15:00", -32.7333, -64.7167, 2200, "Cumbre de las sierras de Comechingones.")],
}
GEAR = [
    ("equipo-carpa", "Carpa 3 personas cuatro estaciones", "Campamento", 8, "Disponible", 1),
    ("equipo-dormir", "Bolsa de dormir -5 °C", "Campamento", 16, "Disponible", 1),
    ("equipo-gps", "GPS de mano", "Navegación", 4, "Disponible", 1),
    ("equipo-mapa", "Cartas topográficas IGN 1:50.000", "Navegación", 2, "En Inspección", 1),
    ("equipo-radio", "Radio VHF portátil", "Comunicación", 6, "Disponible", 1),
    ("equipo-casco", "Casco de escalada", "Seguridad", 12, "Disponible", 1),
    ("equipo-cuerda", "Cuerda dinámica 60 m", "Escalada", 4, "En Reparación", 1),
    ("equipo-campera", "Campera impermeable", "Vestimenta", 10, "Disponible", 1),
    ("equipo-filtro", "Filtro de agua portátil", "Hidratación", 3, "Disponible", 1),
    ("equipo-botiquin", "Botiquín de primeros auxilios", "Primeros Auxilios", 5, "Disponible", 1),
]
# Datos personales ficticios: (DNI, nombre, apellido, sexo, fecha de nacimiento).
CLIENTS = [
    ("38120451", "Lucía", "Fernández", "Femenino", "1994-03-12"), ("36745918", "Mateo", "Gómez", "Masculino", "1992-07-25"),
    ("40233176", "Sofía", "Rodríguez", "Femenino", "1997-11-02"), ("35618842", "Tomás", "Pereyra", "Masculino", "1991-01-18"),
    ("41390527", "Valentina", "Sosa", "Femenino", "1999-05-30"), ("33987264", "Julián", "Acosta", "Masculino", "1988-09-14"),
    ("39456013", "Camila", "Romero", "Femenino", "1996-02-08"), ("37281695", "Nicolás", "Díaz", "Masculino", "1993-12-21"),
    ("42105738", "Agustina", "Luna", "Femenino", "2000-06-16"), ("34872509", "Bruno", "Herrera", "Masculino", "1989-10-04"),
    ("40876321", "Florencia", "Medina", "No binario", "1998-04-27"), ("32654187", "Santiago", "Ruiz", "Masculino", "1986-08-09"),
    ("38904672", "Martina", "Castro", "Femenino", "1995-01-23"), ("36127450", "Facundo", "Ortiz", "Masculino", "1992-03-05"),
    ("41562083", "Julieta", "Vega", "Prefiero no decir", "1999-12-11"),
]
TRIPS = [
    ("salida-1", 0, -2, "planificada", [0, 1, 2], [("equipo-gps", 1), ("equipo-radio", 2)]),
    ("salida-2", 1, 5, "planificada", [3, 4, 5, 6], [("equipo-carpa", 2), ("equipo-casco", 4)]),
    ("salida-3", 2, 12, "planificada", [7, 8], [("equipo-gps", 1), ("equipo-botiquin", 1)]),
    ("salida-4", 3, -5, "planificada", [0, 4], [("equipo-radio", 1)]),
    ("salida-5", 0, -8, "realizada", [1, 2, 3, 5], [("equipo-filtro", 1)]),
    ("salida-6", 4, -35, "realizada", [6, 7, 8], [("equipo-carpa", 1)]),
    ("salida-7", 5, -60, "realizada", [], []),
    ("salida-8", 7, 20, "cancelada", [9], []),
    ("salida-9", 6, 30, "cancelada", [], [("equipo-dormir", 2)]),
]


def java_classpath() -> str:
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    pom = ET.parse(APP / "pom.xml").getroot()
    sqlite_version = pom.findtext("m:properties/m:sqlite-jdbc.version", namespaces=ns)
    m2 = Path.home() / ".m2/repository"
    sqlite_jar = m2 / f"org/xerial/sqlite-jdbc/{sqlite_version}/sqlite-jdbc-{sqlite_version}.jar"
    slf4j_jar = m2 / "org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar"
    if not sqlite_jar.is_file():
        raise FileNotFoundError(f"No se encontró el driver SQLite de Maven: {sqlite_jar}")
    jars = [str(sqlite_jar)] + ([str(slf4j_jar)] if slf4j_jar.is_file() else [])
    modules = [str(APP / module / "target/classes") for module in ("trekking-desktop", "trekking-sqlite", "trekking-core")]
    return ":".join([*modules, *jars])


def initialize_schema(db: Path | None = None, *, cwd: Path = ROOT, compile_app: bool = True) -> None:
    """Compile and call the app's schema initializer without starting JavaFX."""
    if compile_app:
        subprocess.run(["mvn", "-q", "-f", str(APP / "pom.xml"), "compile"], cwd=ROOT, check=True)
    command = ["java"]
    if db is not None:
        command.append(f"-Dtrekking.db={db}")
    command.extend(["-cp", java_classpath(), "com.example.desktop.database.ConexionDB"])
    subprocess.run(command, cwd=cwd, check=True)


def ascii_slug(text: str) -> str:
    return unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode().lower().replace(" ", "")


def own(conn: sqlite3.Connection, entity: str, key: str) -> int | None:
    row = conn.execute(f"SELECT id1 FROM {MANIFEST} WHERE entidad=? AND clave=?", (entity, key)).fetchone()
    return row[0] if row else None


def remember(conn: sqlite3.Connection, entity: str, key: str, id1: int, id2: int | None = None) -> None:
    conn.execute(f"INSERT INTO {MANIFEST}(entidad,clave,id1,id2) VALUES(?,?,?,?)", (entity, key, id1, id2))


def upsert_owned_row(conn: sqlite3.Connection, entity: str, key: str, table: str,
                     columns: tuple[str, ...], values: tuple, natural_sql: str | None = None,
                     natural_value: str | tuple[str, ...] | None = None) -> tuple[int | None, str]:
    old = own(conn, entity, key)
    if old is not None:
        assignments = ",".join(f"{c}=?" for c in columns)
        conn.execute(f"UPDATE {table} SET {assignments} WHERE id=?", (*values, old))
        return old, "actualizados"
    natural_values = natural_value if isinstance(natural_value, tuple) else (natural_value,)
    if natural_sql and conn.execute(natural_sql, natural_values).fetchone():
        print(f"Conflicto omitido: {entity} {key} colisiona con {natural_value}", file=sys.stderr)
        return None, "omitidos"
    marks = ",".join("?" for _ in columns)
    cur = conn.execute(f"INSERT INTO {table} ({','.join(columns)}) VALUES ({marks})", values)
    row_id = cur.lastrowid
    remember(conn, entity, key, row_id)
    return row_id, "insertados"


def seed(conn: sqlite3.Connection) -> dict[str, int]:
    conn.execute(f"CREATE TABLE IF NOT EXISTS {MANIFEST} (entidad TEXT NOT NULL, clave TEXT NOT NULL, id1 INTEGER NOT NULL, id2 INTEGER, PRIMARY KEY(entidad, clave))")
    counts = {"insertados": 0, "actualizados": 0, "omitidos": 0}
    def note(result: str) -> None: counts[result] += 1
    today = dt.date.today()
    routes: dict[str, tuple[int, tuple]] = {}
    for key, name, altitude, terrain, tech, physical, active in ROUTES:
        vals = (name, name.casefold(), altitude, terrain, tech, physical, active)
        rid, result = upsert_owned_row(conn, "ruta", key, "rutas", ("nombre", "nombre_clave", "altitud_maxima", "tipo_terreno", "dificultad_tecnica", "dificultad_fisica", "activo"), vals, "SELECT id FROM rutas WHERE nombre=? COLLATE NOCASE OR nombre_clave=?", (name, name.casefold()))
        note(result)
        # The collision query accepts two parameters.
        if rid is not None: routes[key] = (rid, vals)
    sexos = dict(conn.execute("SELECT nombre,id FROM sexos"))
    clients: dict[int, tuple[int, tuple]] = {}
    for i, (dni, name, surname, sex, birth) in enumerate(CLIENTS):
        vals = (dni, name, surname, birth, f"{ascii_slug(name)}.{ascii_slug(surname)}@example.com", f"+54 351 555-{i:04d}", sexos[sex], sex, 1)
        cid, result = upsert_owned_row(conn, "cliente", f"cliente-{dni}", "clientes", ("dni", "nombre", "apellido", "fecha_nacimiento", "email", "telefono", "sexo_id", "sexo", "activo"), vals, "SELECT id FROM clientes WHERE dni=?", dni)
        note(result)
        if cid is not None: clients[i] = (cid, vals)
    gear: dict[str, tuple[int, tuple]] = {}
    for key, name, category, amount, state, active in GEAR:
        vals = (name, category, amount, state, active)
        eid, result = upsert_owned_row(conn, "inventario", key, "equipamiento", ("nombre", "categoria", "cantidad", "estado", "activo"), vals, "SELECT id FROM equipamiento WHERE nombre=? COLLATE NOCASE", name)
        note(result)
        if eid is not None: gear[key] = (eid, vals)
    route_checkpoints: dict[str, list[tuple]] = {}
    for route_key, points in POINTS.items():
        if route_key not in routes: continue
        rid, _ = routes[route_key]
        copied = []
        for order, point in enumerate(points):
            key = f"{route_key}-checkpoint-{order}"
            name, hour, lat, lon, altitude, description = point
            associated = False
            row = conn.execute(f"SELECT id1 FROM {MANIFEST} WHERE entidad='checkpoint' AND clave=?", (key,)).fetchone()
            if row:
                pid = row[0]
                conn.execute("UPDATE checkpoints SET nombre=?,hora=?,latitud=?,longitud=?,altitud=?,descripcion=? WHERE id=?", (*point, pid))
                note("actualizados")
            else:
                cur = conn.execute("INSERT INTO checkpoints(nombre,hora,latitud,longitud,altitud,descripcion) VALUES(?,?,?,?,?,?)", point)
                pid = cur.lastrowid
                remember(conn, "checkpoint", key, pid); note("insertados")
            relation_key = f"{route_key}|checkpoint|{order}"
            relation = conn.execute(f"SELECT id1,id2 FROM {MANIFEST} WHERE entidad='ruta_checkpoint' AND clave=?", (relation_key,)).fetchone()
            if relation:
                try:
                    cursor = conn.execute("UPDATE ruta_checkpoints SET orden=? WHERE ruta_id=? AND checkpoint_id=?", (order, relation[0], relation[1]))
                    if cursor.rowcount:
                        note("actualizados")
                        associated = True
                    else:
                        conn.execute(f"DELETE FROM {MANIFEST} WHERE entidad='ruta_checkpoint' AND clave=?", (relation_key,))
                        relation = None
                except sqlite3.IntegrityError as exc:
                    counts["omitidos"] += 1
                    print(f"Conflicto omitido: Itinerario {relation_key}: {exc}", file=sys.stderr)
            if not relation:
                occupied = conn.execute("SELECT 1 FROM ruta_checkpoints WHERE ruta_id=? AND orden=? OR checkpoint_id=?", (rid, order, pid)).fetchone()
                if occupied:
                    counts["omitidos"] += 1
                    print(f"Conflicto omitido: Itinerario {relation_key} ya tiene una asociación ajena", file=sys.stderr)
                else:
                    conn.execute("INSERT INTO ruta_checkpoints(ruta_id,checkpoint_id,orden) VALUES(?,?,?)", (rid, pid, order))
                    remember(conn, "ruta_checkpoint", relation_key, rid, pid)
                    note("insertados")
                    associated = True
            if associated:
                copied.append(point)
        route_checkpoints[route_key] = copied
    # Each seeded Salida owns its operational snapshot and child rows.
    for index, (key, route_index, offset, state, participant_indices, assignments) in enumerate(TRIPS):
        route_key = ROUTES[route_index][0]
        if route_key not in routes or any(i not in clients for i in participant_indices) or any(k not in gear for k, _ in assignments):
            counts["omitidos"] += 1
            print(f"Dependencias omitidas para Salida {key}", file=sys.stderr)
            continue
        rid, route = routes[route_key]
        trip_values = (rid, (today + dt.timedelta(days=offset)).isoformat(), state, *route[:1], *route[2:6])
        # Route snapshot columns deliberately match the catalog values.
        sid = own(conn, "salida", key)
        owned_children: dict[str, set[str]] = {}
        if sid:
            conn.execute("UPDATE salidas SET ruta_id=?,fecha=?,estado=?,ruta_nombre=?,ruta_altitud_maxima=?,ruta_tipo_terreno=?,ruta_dificultad_tecnica=?,ruta_dificultad_fisica=? WHERE id=?", (*trip_values, sid)); note("actualizados")
            child_tables = {
                "salida_checkpoint": ("salida_checkpoints", "id"),
                "salida_equipamiento": ("salida_equipamiento", "equipamiento_id"),
                "salida_participante": ("salida_participantes", "cliente_id"),
            }
            for entity, (table, relation_id) in child_tables.items():
                rows = conn.execute(f"SELECT clave,id1,id2 FROM {MANIFEST} WHERE entidad=? AND substr(clave,1,?)=?", (entity, len(key) + 1, key + "|")).fetchall()
                owned_children[entity] = {row[0] for row in rows}
                for child_key, id1, id2 in rows:
                    if entity == "salida_checkpoint":
                        conn.execute("DELETE FROM salida_checkpoints WHERE id=?", (id1,))
                    else:
                        conn.execute(f"DELETE FROM {table} WHERE salida_id=? AND {relation_id}=?", (id1, id2))
                    conn.execute(f"DELETE FROM {MANIFEST} WHERE entidad=? AND clave=?", (entity, child_key))
        else:
            cur = conn.execute("INSERT INTO salidas(ruta_id,fecha,estado,ruta_nombre,ruta_altitud_maxima,ruta_tipo_terreno,ruta_dificultad_tecnica,ruta_dificultad_fisica) VALUES(?,?,?,?,?,?,?,?)", trip_values)
            sid = cur.lastrowid; remember(conn, "salida", key, sid); note("insertados")
        for order, point in enumerate(route_checkpoints.get(route_key, [])):
            child_key = f"{key}|checkpoint|{order}"
            try:
                cur = conn.execute("INSERT INTO salida_checkpoints(salida_id,nombre,hora,latitud,longitud,altitud,descripcion,orden) VALUES(?,?,?,?,?,?,?,?)", (sid, *point, order))
                remember(conn, "salida_checkpoint", child_key, cur.lastrowid)
                note("actualizados" if child_key in owned_children.get("salida_checkpoint", set()) else "insertados")
            except sqlite3.IntegrityError as exc:
                counts["omitidos"] += 1
                print(f"Conflicto omitido: copia de Checkpoint {child_key}: {exc}", file=sys.stderr)
        for gkey, amount in assignments:
            eid, item = gear[gkey]
            child_key = f"{key}|equipo|{gkey}"
            try:
                conn.execute("INSERT INTO salida_equipamiento(salida_id,equipamiento_id,nombre_copia,categoria_copia,cantidad) VALUES(?,?,?,?,?)", (sid, eid, item[0], item[1], amount))
                remember(conn, "salida_equipamiento", child_key, sid, eid)
                note("actualizados" if child_key in owned_children.get("salida_equipamiento", set()) else "insertados")
            except sqlite3.IntegrityError as exc:
                counts["omitidos"] += 1
                print(f"Conflicto omitido: Equipamiento de Salida {child_key}: {exc}", file=sys.stderr)
        for client_index in participant_indices:
            cid, client = clients[client_index]
            child_key = f"{key}|cliente|{client[0]}"
            try:
                conn.execute("INSERT INTO salida_participantes(salida_id,cliente_id,dni_copia,nombre_copia,apellido_copia,telefono_copia) VALUES(?,?,?,?,?,?)", (sid, cid, client[0], client[1], client[2], client[5]))
                remember(conn, "salida_participante", child_key, sid, cid)
                note("actualizados" if child_key in owned_children.get("salida_participante", set()) else "insertados")
            except sqlite3.IntegrityError as exc:
                counts["omitidos"] += 1
                print(f"Conflicto omitido: Participante de Salida {child_key}: {exc}", file=sys.stderr)
    return counts


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Carga salidas de trekking en Córdoba en Trekking App.")
    parser.add_argument("--db", type=Path, default=DEFAULT_DB, help="Base SQLite (por defecto trekking.db en la raíz).")
    args = parser.parse_args(argv)
    db = args.db.expanduser().resolve()
    db.parent.mkdir(parents=True, exist_ok=True)
    try:
        initialize_schema(db)
        with sqlite3.connect(db) as conn:
            conn.execute("PRAGMA foreign_keys=ON")
            result = seed(conn)
            conn.commit()
            totals = {table: conn.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0] for table in ("rutas", "clientes", "equipamiento", "salidas")}
        print("Datos cargados. " + ", ".join(f"{k}: {v}" for k, v in result.items()))
        print("Totales de tabla (incluyen registros ajenos): " + ", ".join(f"{k}: {v}" for k, v in totals.items()))
        return 0
    except (OSError, sqlite3.Error, subprocess.CalledProcessError) as exc:
        print(f"No se pudo cargar la base: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
