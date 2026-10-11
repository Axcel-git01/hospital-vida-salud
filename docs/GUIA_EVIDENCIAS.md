# Guía para generar las evidencias (entregable C)

Guarda las capturas en `docs/evidencias/` con los nombres sugeridos. Usa siempre datos ficticios y **no** muestres contraseñas en pantalla.

> **Windows/PowerShell:** en lugar de `export VAR=valor` usa `$env:VAR="valor"`. Si `curl` da problemas con las comillas, usa Postman o Insomnia
> con los mismos cuerpos JSON del README.

## 0. Preparación

```bash
cp .env.example .env                      # edite las claves
docker compose up -d                      # PostgreSQL + RabbitMQ
export DB_USER=hospital DB_PASSWORD=<su_clave> RABBIT_USER=hospital RABBIT_PASSWORD=<su_clave>
./mvnw -DskipTests install                # una sola vez
# luego, una terminal por servicio (orden del README): eureka, pacientes, citas, atenciones, laboratorio, pagos, notificaciones, auditoria, gateway
```

## 1. Evidencias funcionales

| # | Qué demostrar | Cómo | Captura sugerida |
|---|---|---|---|
| 1 | **Eureka** con los servicios registrados | Abrir <http://localhost:8761> | `01-eureka.png` (lista *Instances currently registered*: 8 servicios, incluido `API-GATEWAY`) |
| 2 | **Gateway** enruta | Ejecutar los `curl` del README siempre contra `localhost:8080` | `02-gateway-pacientes.png`, `02-gateway-citas.png` (petición + respuesta) |
| 3 | Paciente duplicado rechazado | Repetir el `POST /api/pacientes` | `03-duplicado-409.png` |
| 4 | Cita + transición inválida | `PATCH …/estado {"estado":"PENDIENTE"}` sobre una cita confirmada → 422 | `04-transicion-422.png` |
| 5 | **Reprogramación** | `PATCH /api/citas/1/reprogramar` y luego `GET /api/citas/1` | `05-reprogramar.png` |
| 6 | **Comunicación síncrona** | Detener `pacientes-service` y crear una cita → **503**; levantarlo y repetir → 201 | `06-sincrona-503.png` |
| 7 | **Persistencia SQL / R2DBC** | `docker compose exec postgres psql -U $DB_USER -d citas_db -c "select id, codigo, estado, fecha_hora from cita;"` | `07-postgres-citas.png` |
| 8 | **RabbitMQ**: evento publicado y consumido | Panel <http://localhost:15672> → *Exchanges* (`hospital.eventos`) y *Queues* (`notificaciones.eventos`, `auditoria.eventos`) mostrando *Message rates* tras confirmar/reprogramar una cita | `08-rabbit-exchange.png`, `08-rabbit-colas.png` |
| 9 | Evento **consumido** | `curl localhost:8080/api/notificaciones` y `curl localhost:8080/api/auditoria/cita/1` (historial PENDIENTE → CONFIRMADA y REPROGRAMADA); además el log de `notificaciones-service` ("Notificación simulada…") | `09-notificaciones.png`, `09-auditoria.png` |
| 10 | Pago inválido | `POST /api/pagos` con `monto: 0` → 400; repetir un `numeroOperacion` → 409 | `10-pago-400.png` |
| 11 | Recurso inexistente | `GET /api/citas/9999` → 404 | `11-404.png` |

## 2. Pruebas automatizadas locales

```bash
./mvnw verify
```

Captura el resumen *Reactor Summary* con `BUILD SUCCESS` y el conteo de pruebas → `12-mvnw-verify.png`.

## 3. Integración continua en GitHub

### 3.1 Primer push (CI verde)

```bash
git init -b main
git add .
git status                                  # confirme que NO aparece .env
git update-index --chmod=+x mvnw            # para que mvnw sea ejecutable en GitHub
git commit -m "feat: plataforma hospitalaria con microservicios"
git remote add origin https://github.com/<usuario>/hospital-vida-salud.git
git push -u origin main
```

En GitHub → pestaña **Actions** → abre la ejecución → captura el workflow en verde con sus pasos desplegados → `13-actions-verde.png`
y el log del paso *Compilar y ejecutar pruebas* → `13-actions-log.png`.

### 3.2 Protección de la rama (para que un PR defectuoso no se integre)

GitHub → *Settings → Branches → Add branch protection rule* → `main` → marcar **Require status checks to pass before merging**
y seleccionar `build-test` → `14-proteccion-rama.png`.
(Si `build-test` no aparece en la lista, haz primero un push o PR para que el workflow se ejecute una vez.)

### 3.3 Fallo controlado y corrección (ciclo completo)

```bash
git checkout -b prueba/ci-fallo-controlado
```

1. Abre `citas-service/src/test/java/pe/edu/hospital/citas/EstadoCitaTest.java` y, en `citaCanceladaNoPuedeConfirmarse`,
   cambia `assertFalse(` por `assertTrue(`. (Una cita cancelada **no** puede confirmarse, así que la prueba ahora afirma lo contrario y falla.)
2. Sube el cambio y abre el PR:

```bash
git commit -am "test: romper a proposito una prueba para demostrar el CI"
git push -u origin prueba/ci-fallo-controlado
```

3. En GitHub abre un **Pull Request hacia `main`**. Espera a que Actions termine en **rojo**.
   Capturas: `15-actions-rojo.png` (lista de ejecuciones), `15-actions-log-fallo.png` (log con `EstadoCitaTest … FAILURE`),
   `15-pr-bloqueado.png` (el PR con la verificación fallida y el botón de *merge* bloqueado, si activaste la protección).
4. Corrige y vuelve a subir:

```bash
git revert --no-edit HEAD                   # o edite el test de nuevo
git push
```

5. Espera a que Actions termine en **verde** → `16-actions-verde-tras-correccion.png`.
   Ya puedes hacer *merge* del PR (o descartarlo; lo importante es mostrar el ciclo rojo → corrección → verde).

## 4. Checklist del entregable C

- [ ] Eureka con servicios registrados
- [ ] Gateway enrutando solicitudes
- [ ] Endpoints y reglas de negocio funcionando (duplicado, transición inválida, reprogramación, pago inválido, 404)
- [ ] Persistencia SQL (consulta en PostgreSQL) y mención de R2DBC
- [ ] RabbitMQ: evento publicado y consumido
- [ ] `./mvnw verify` local en verde
- [ ] GitHub Actions: ejecución exitosa
- [ ] Fallo controlado y su corrección (rojo → verde)
- [ ] Diagramas de arquitectura y flujo (en `DOCUMENTO_TECNICO.md`; GitHub los renderiza)

## 5. Preguntas probables en la sustentación

- **¿Por qué usar `block()` en los listeners de RabbitMQ?** Corren en el hilo del contenedor de mensajería, no en el pipeline WebFlux.
  Esperar el resultado permite confirmar (*ack*) el mensaje solo si se guardó; si falla, se reintenta 3 veces.
- **¿Qué pasa si RabbitMQ cae?** El cambio de estado se guarda y el evento se pierde (limitación documentada); la solución es el patrón Outbox.
- **¿Qué pasa si `pacientes-service` cae?** Crear citas devuelve 503 tras 3 s de timeout; las consultas de citas existentes siguen funcionando.
- **¿Por qué Gateway y no llamar a los puertos?** Un único punto de entrada estable, para seguridad centralizada y para que los servicios
  puedan moverse o escalar sin que el cliente lo note.
- **¿Cómo evitan dos citas en el mismo horario si llegan a la vez?** Además de la validación por consulta, hay índices únicos parciales en PostgreSQL;
  la segunda inserción se rechaza y se traduce a 409.
- **¿Por qué no hay claves foráneas entre servicios?** Cada servicio es dueño de su base; se referencian por identificador y se valida por contrato.
- **¿Qué aporta lo reactivo aquí?** Menos hilos para muchas solicitudes con espera de E/S (BD y llamadas entre servicios) en horas pico.
