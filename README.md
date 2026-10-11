# Hospital Vida Salud – Plataforma administrativa con microservicios

Proyecto académico (T2). Usa **solo datos ficticios**; no diagnostica ni recomienda tratamientos.

## Arquitectura

```
Cliente ──> API Gateway (8080) ──> Eureka (8761) ──┬─> pacientes-service (8081) ─┐
                                                   ├─> citas-service (8082) ─────┤ SQL + R2DBC
                                                   ├─> atenciones-service (8086) ─> laboratorio-service (8087)
                                                   ├─> pagos-service (8083) ─────┤ (una BD por servicio)
                                                   ├─> notificaciones-service (8084)
                                                   └─> auditoria-service (8085)

citas-service ──(síncrono, WebClient + Eureka)──> pacientes-service
citas / atenciones / laboratorio / pagos ──> RabbitMQ (exchange topic "hospital.eventos") ──> notificaciones / auditoría
```

| Módulo | Responsabilidad |
|---|---|
| `hospital-common` | Contrato del evento, formato de error, manejo de excepciones, publicador de eventos |
| `eureka-server` | Registro y descubrimiento de servicios |
| `api-gateway` | Punto de entrada y enrutamiento `/api/**` por nombre de servicio |
| `pacientes-service` | Registro/consulta/actualización de pacientes, sin duplicar documento |
| `citas-service` | Programación y **reprogramación** de citas, estados y transiciones válidas, evento por cada cambio |
| `atenciones-service` | Atención administrativa ligada a una cita CONFIRMADA (consulta síncrona a citas) |
| `laboratorio-service` | Solicitudes de laboratorio ficticias con estados controlados (valida la atención) |
| `pagos-service` | Pagos simulados (monto > 0, operación única) |
| `notificaciones-service` | Consume eventos `cita.*`, `pago.*` y `laboratorio.*` y registra notificaciones simuladas |
| `auditoria-service` | Consume todos los eventos y guarda estado anterior/nuevo/origen |

## Requisitos
- JDK 21 y Docker. **No hace falta instalar Maven**: el repositorio incluye Maven Wrapper (`./mvnw`; en Windows `mvnw.cmd`).
  La primera ejecución descarga Maven 3.9.9 (requiere internet).
- Si al clonar `./mvnw` no es ejecutable: `chmod +x mvnw` (y, antes de subirlo, `git update-index --chmod=+x mvnw`).

## Cómo ejecutar
```bash
cp .env.example .env            # edite las claves (el archivo .env no se sube a Git)
docker compose up -d            # PostgreSQL + RabbitMQ (panel: http://localhost:15672)

export DB_USER=hospital DB_PASSWORD=<su_clave> RABBIT_USER=hospital RABBIT_PASSWORD=<su_clave>

./mvnw -pl eureka-server spring-boot:run          # 1. Eureka   -> http://localhost:8761
./mvnw -pl pacientes-service spring-boot:run      # 2. servicios (una terminal cada uno)
./mvnw -pl citas-service spring-boot:run
./mvnw -pl atenciones-service spring-boot:run
./mvnw -pl laboratorio-service spring-boot:run
./mvnw -pl pagos-service spring-boot:run
./mvnw -pl notificaciones-service spring-boot:run
./mvnw -pl auditoria-service spring-boot:run
./mvnw -pl api-gateway spring-boot:run            # 3. Gateway  -> http://localhost:8080
```
> Si `-pl` no encuentra los módulos, ejecute primero `./mvnw -DskipTests install` en la raíz.

## Probar el flujo extremo a extremo (siempre por el Gateway)
```bash
curl -X POST localhost:8080/api/pacientes -H 'Content-Type: application/json' -d '{"tipoDocumento":"DNI","numeroDocumento":"10000001","nombres":"Ana","apellidos":"Prueba","fechaNacimiento":"1990-01-01","telefono":"999000111","correo":"ana@prueba.test"}'
# repetir el mismo POST  -> 409 (duplicado)
curl -X POST localhost:8080/api/citas -H 'Content-Type: application/json' -d '{"pacienteId":1,"especialidad":"Medicina General","fechaHora":"2030-01-15T10:00:00","profesionalCodigo":"PROF-001","motivo":"Control administrativo"}'
curl -X PATCH localhost:8080/api/citas/1/estado -H 'Content-Type: application/json' -d '{"estado":"CONFIRMADA"}'   # publica cita.confirmada
curl -X POST localhost:8080/api/atenciones -H 'Content-Type: application/json' -d '{"citaId":1,"area":"Consulta externa"}'
curl -X POST localhost:8080/api/laboratorio -H 'Content-Type: application/json' -d '{"atencionId":1,"tipoExamen":"Examen de demostración"}'
curl -X PATCH localhost:8080/api/citas/1/reprogramar -H 'Content-Type: application/json' -d '{"fechaHora":"2030-01-16T11:00:00"}'   # publica cita.reprogramada (el estado se conserva)
curl -X PATCH "localhost:8080/api/pacientes/1/estado?valor=INACTIVO"   # ACTIVO | INACTIVO
curl localhost:8080/api/notificaciones           # notificaciones generadas por los eventos
curl localhost:8080/api/auditoria/cita/1         # historial PENDIENTE -> CONFIRMADA
curl -X PATCH localhost:8080/api/citas/1/estado -H 'Content-Type: application/json' -d '{"estado":"PENDIENTE"}'   # 422 transición inválida
curl -X POST localhost:8080/api/pagos -H 'Content-Type: application/json' -d '{"pacienteId":1,"concepto":"Consulta de ejemplo","monto":0,"medioPago":"TARJETA","numeroOperacion":"OP-1"}'   # 400 monto inválido
```

## Pruebas automatizadas
```bash
./mvnw verify
```
Incluyen pruebas unitarias de reglas (duplicados, transiciones, monto de pago), pruebas de integración de endpoints
(WebTestClient + R2DBC/H2 en memoria) y pruebas del productor y del consumidor de eventos. **No requieren Docker ni RabbitMQ.**

## Integración continua
`.github/workflows/ci.yml` se ejecuta en cada **push** (cualquier rama) y en cada **pull request hacia `main`**:
checkout → JDK 21 + caché Maven → `./mvnw verify` (compila y prueba los 10 módulos). Si algo falla, el job queda en rojo;
con protección de rama (Settings → Branches → *Require status checks*) el PR no se puede integrar.

## Seguridad y datos
Sin contraseñas ni datos reales en el repositorio: las credenciales vienen de variables de entorno (`.env` está en `.gitignore`).

## Supuestos y limitaciones
- Una sola instancia PostgreSQL con **una base de datos por servicio** (ningún servicio lee tablas de otro; no hay claves foráneas entre servicios).
- Agenda simplificada: no se permite que un profesional ni un paciente tengan dos citas (no canceladas) en la misma fecha y hora.
  Se valida por consulta **y** se garantiza en PostgreSQL con índices únicos parciales (`schema-postgresql.sql`), por lo que dos solicitudes simultáneas no pueden ocupar el mismo horario.
  Las pruebas usan H2, que no soporta índices parciales; allí solo actúa la validación por consulta.
- Reprogramación: solo citas `PENDIENTE` o `CONFIRMADA`; la cita conserva su estado, se revalida la agenda (excluyendo la propia cita) y se publica `cita.reprogramada`.
- Una cita admite una sola atención vigente (índice único parcial en `atenciones-service`).
- Los consumidores de RabbitMQ reintentan 3 veces y, si siguen fallando, descartan el mensaje (no hay reencolado infinito). Mejora: cola de mensajes muertos (DLQ) e idempotencia por evento.
- Si RabbitMQ no está disponible, el cambio de estado se guarda y el evento se pierde (mejora: patrón Outbox).
- No implementados (opcionales según el enunciado): Hospitalización y Farmacia.
- Al registrar una atención, la cita no pasa automáticamente a ATENDIDA: se cambia con `PATCH /api/citas/{id}/estado`.
- Sin bloqueo optimista: dos cambios de estado simultáneos sobre el mismo recurso podrían pisarse (mejora: `@Version`).
- `pagos-service` no valida que el paciente exista (el cargo es solo administrativo); mejora: consulta síncrona a pacientes como hace citas.
- Gateway sin autenticación (mejora documentada). El endpoint de actuator `gateway` **no** se expone.

## Documentación adicional
- [`docs/DOCUMENTO_TECNICO.md`](docs/DOCUMENTO_TECNICO.md): análisis, arquitectura, contratos, eventos, justificación técnica y CI.
- [`docs/GUIA_EVIDENCIAS.md`](docs/GUIA_EVIDENCIAS.md): pasos exactos para generar las capturas y el ciclo de CI (fallo controlado → corrección).
