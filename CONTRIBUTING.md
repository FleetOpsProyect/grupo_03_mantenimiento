# Guía de Contribución

Este documento describe el flujo de trabajo y las normas de ingeniería para colaborar en **FleetOps — Microservicio de Mantenimiento**. Toda contribución debe apegarse estrictamente a este proceso para asegurar la calidad del código, el respeto por la Arquitectura Hexagonal y la estabilidad del sistema.

---

## Estrategia de Ramas

El repositorio sigue un modelo de integración basado en GitFlow simplificado con ramas personales:

```
main           ──●──────────────────────●──────────────●──── (releases / producción: tags v0.1.0, v1.0.0)
                  ↑                      ↑              ↑
                  PR                     PR             PR
                  │                      │              │
develop        ──●──●──●──●──●──●──●──●──●──●──●──●──●──●──── (integración continua y pruebas)
                  ↑  ↑  ↑  ↑              ↑  ↑  ↑  ↑
                  PR PR PR PR             PR PR PR PR
                  │  │  │  │              │  │  │  │
DAR            ──●──●──●──────────────────────────────────── (rama personal - Daniel Andrade)
jsfv           ──●──●──●──────────────────────────────────── (rama personal - Jhoan Fernández)
jss            ──●──●──●──●───────────────────────────────── (rama personal - Juan Sebastián Sierra)
nco            ──●──●──●──────────────────────────────────── (rama personal - Nicolás Córdoba)
```

### Ramas Principales

- **`main`**: Rama de producción y entregas oficiales. Solo recibe cambios al cierre de cada sprint validado tras demo funcional y aprobación general. Cada merge a `main` debe llevar un tag de versión semántica (ej. `v0.1.0`, `v1.0.0`). **Protegida** (sin push directo).
- **`develop`**: Rama base de integración continua. Confluye el trabajo completado, testeado y validado del equipo. **Protegida** (requiere Pull Request aprobado y paso obligatorio de CI).
- **Ramas personales (`DAR`, `jsfv`, `jss`, `nco`)**: Cada integrante del equipo dispone de su rama de desarrollo personal asociada para trabajar en sus historias de usuario, adaptadores o tareas asignadas antes de promoverlas a `develop`.

---

### Reglas de los Flujos de Trabajo

#### 1. Trabajo dentro de la rama personal
- Realizar commits frecuentes y atómicos respetando la convención de **Conventional Commits**.
- Validar localmente la compilación, pruebas y estilo antes de cada `push` (ver **Gate 1**).
- Push directo permitido únicamente sobre la rama personal correspondiente.
- Mantener la rama personal sincronizada frecuentemente con `develop`:
  ```bash
  git fetch origin
  git checkout <tu-rama>
  git merge origin/develop # o git rebase origin/develop
  ```

#### 2. Promoción de rama personal a `develop`
- Solo cuando la Historia de Usuario (HU) o tarea del sprint esté 100% desarrollada, probada y estable.
- Apertura de **Pull Request (PR)** desde la rama personal hacia `develop`.
- Completar rigurosamente la plantilla obligatoria [`.github/PULL_REQUEST_TEMPLATE.md`](.github/PULL_REQUEST_TEMPLATE.md).
- El pipeline de GitHub Actions (`ci.yml`) debe pasar todos los controles de calidad en verde (ver **Gate 2**).
- El agente **Qodo AI** (`qodo-review.yml`) revisa automáticamente el PR, emitiendo análisis (`review`) y sugerencias de mejora (`improve`).
- Atender y resolver observaciones señaladas por Qodo antes de solicitar el merge.
- Aplicar **Squash and Merge** (o Rebase Merge según acuerdo del sprint) para mantener un historial limpio en `develop`.

#### 3. Promoción de `develop` a `main`
- Se ejecuta exclusivamente al cierre del sprint tras la demo funcional aceptada por el Product Owner / Docente.
- Apertura de PR desde `develop` hacia `main`.
- Asignación de tag semántico (`v0.X.0` o `v1.X.0`).
- Actualización de documentación: `README.md`, bitácora de decisiones arquitectónicas (`solution/decisiones_y_observaciones.md`) y changelog.

---

## Gates de Calidad

### Gate 1: Validación local previa al push

Antes de subir cambios a tu rama personal remota, ejecuta las tareas de verificación local en tu terminal:

#### En Windows PowerShell:
```powershell
# 1. Compilar código fuente
.\gradlew.bat compileJava

# 2. Análisis de estilo y convenciones (Checkstyle)
.\gradlew.bat checkstyleMain checkstyleTest

# 3. Pruebas unitarias y de integración
.\gradlew.bat test

# 4. Generación de reporte y verificación de cobertura mínima (>= 80%)
.\gradlew.bat jacocoTestReport
.\gradlew.bat jacocoTestCoverageVerification

# Verificación integral completa (ejecuta todo lo anterior)
.\gradlew.bat check
```

#### En Linux / macOS / Bash:
```bash
./gradlew checkstyleMain checkstyleTest
./gradlew test
./gradlew jacocoTestCoverageVerification
# o verificación integral:
./gradlew check
```

> [!WARNING]
> Si cualquiera de estas tareas falla, **no hagas push**. Corrige los errores de estilo, cobertura o lógica primero.

---

### Gate 2: Pipeline de CI Automatizado (`.github/workflows/ci.yml`)

Al abrir o actualizar un PR hacia `develop` o `main`, GitHub Actions ejecuta automáticamente el siguiente flujo:

1. **Checkout & JDK 21**: Clona el repositorio con historial completo (`fetch-depth: 0`) y configura Eclipse Temurin Java 21.
2. **Service Container PostgreSQL 16**: Levanta una base de datos PostgreSQL de prueba para validar migraciones Flyway y persistencia.
3. **Linter Checkstyle**: `./gradlew checkstyleMain checkstyleTest` valida cumplimiento de formato bajo Google Java Style adaptado (`checkstyle.xml`).
4. **Pruebas Automatizadas**: `./gradlew test` ejecuta la suite de tests unitarios y de integración con Testcontainers/Spring Boot Test.
5. **Reporte y Regla de Cobertura JaCoCo**: `./gradlew jacocoTestCoverageVerification` exige un umbral mínimo de **80% de cobertura** sobre el código analizado.
6. **SonarCloud Scan**: `./gradlew sonar` analiza deuda técnica, vulnerabilidades de seguridad, code smells y cobertura en SonarCloud.
7. **Empaquetado**: `./gradlew bootJar -x test` garantiza que el artefacto JAR final sea construible.
8. **Qodo AI Review (`qodo-review.yml`)**: Analiza el diff del PR con el modelo Gemini para retroalimentación automatizada de código, mejores prácticas y seguridad.

> [!IMPORTANT]
> Un PR con checks en rojo no podrá ser mezclado bajo ninguna circunstancia.

---

### Gate 3: Revisión de Código Humana (Peer Review)

Aun cuando el pipeline de CI esté en verde, es indispensable la revisión por parte de al menos un compañero de equipo:

1. **Revisión del diff**: Inspeccionar cada archivo modificado para garantizar cumplimiento de la **Arquitectura Hexagonal**:
   - `domain/`: Debe ser **Java 21 puro**, sin anotaciones de Spring Boot, JPA, Jackson ni RabbitMQ.
   - `port/`: Interfaces claras de entrada (`port/in`) y salida (`port/out`).
   - `adapter/`: Implementación adecuada de controladores, entidades JPA, mappers y listeners.
2. **Revisión de hallazgos de Qodo AI**: Revisar los comentarios de Qodo y verificar que los hallazgos críticos o mayores hayan sido solventados.
3. **Validación de criterios de aceptación**: Verificar que el código satisfaga los requisitos de la Historia de Usuario correspondiente.
4. **Sin secretos en commits**: Comprobar que no se hayan subido credenciales, variables del `.env` o archivos sensibles.
5. **Aprobación explícita**: Emitir la aprobación formal en GitHub antes de proceder al merge.

---

## Convención de Commits

Utilizamos el estándar de **Conventional Commits**:

```
<tipo>(<alcance opcional>): <descripción concisa en imperativo>

[cuerpo explicativo opcional: contexto del problema y solución]

[pie / referencias: Closes HU-XX, Refs #...]
```

### Tipos Permitidos

| Tipo | Propósito | Ejemplo |
| :--- | :--- | :--- |
| `feat` | Nueva funcionalidad (asociada a una HU) | `feat(domain): implementa reglas preventivas jerarquicas` |
| `fix` | Corrección de un defecto o bug | `fix(persistence): corrige mapeo JPA en MaintenanceLogEntity` |
| `docs` | Cambios en documentación | `docs(contributing): actualiza guia de ramas y gates` |
| `refactor` | Refactorización de código sin alterar comportamiento externo | `refactor(adapter): extrae mappers de DTO a entidad` |
| `test` | Incorporación o ajuste de pruebas unitarias/integración | `test(order): agrega pruebas unitarias para UpdateOrderStatusUseCase` |
| `style` | Formato o estilo de código (Checkstyle, indentación, espacios) | `style: corrige sangrias y orden de imports segun checkstyle` |
| `perf` | Optimizaciones de rendimiento | `perf(query): agrega indice en postgres para vehicle_id y status` |
| `ci` | Ajustes en pipelines de GitHub Actions o Gradle scripts | `ci: integra escaneo de SonarCloud y reporte JaCoCo` |
| `chore` | Tareas generales de mantenimiento y dependencias | `chore(deps): actualiza version de springdoc openapi` |

### Ejemplos en el Proyecto

```
feat(rules): implementa HU-01 parametrizacion de reglas preventivas

- Agrega RegisterRuleUseCase y RegisterRuleService
- Define puertos RuleRepositoryPort y adaptador JPA
- Agrega validacion para alcances GLOBAL, CATEGORY y VEHICLE

Closes HU-01
```

```
fix(outbox): asegura despacho atomico de MantenimientoFinalizadoEvent

Corrige excepcion por concurrencia al actualizar el estado de outbox
a SENT tras la publicacion exitosa en RabbitMQ.
```

---

## Plantilla de Pull Request

Al crear un PR en GitHub, se cargará automáticamente la plantilla [`.github/PULL_REQUEST_TEMPLATE.md`](.github/PULL_REQUEST_TEMPLATE.md). Asegúrate de diligenciar:

1. **Descripción del Cambio**: Explicación breve de la solución técnica.
2. **Historia de Usuario Relacionada**: Referenciar el issue (`Closes #HU-XX`).
3. **Tipo de Cambio**: Feature, fix, refactor, docs o CI/CD.
4. **Capa Hexagonal Afectada**:
   - `domain.model` / `domain.exception`
   - `domain.port.in` / `domain.port.out`
   - `application.service`
   - `adapter.in` (Web REST, Scheduler, RabbitMQ Listener)
   - `adapter.out` (Persistencia JPA, RabbitMQ Publisher, Cliente HTTP Vehículos)
   - `infrastructure` / Migraciones Flyway
5. **Checklist de Calidad**:
   - [x] El código compila localmente sin errores (`./gradlew compileJava`).
   - [x] Checkstyle sin violaciones (`./gradlew checkstyleMain checkstyleTest`).
   - [x] Pruebas unitarias e integración al 100% (`./gradlew test`).
   - [x] Cobertura JaCoCo $\ge$ 80% (`./gradlew jacocoTestCoverageVerification`).
   - [x] Sin credenciales ni `.env` en los commits.

---

## Definición de Listo (Definition of Ready - DoR)

Una Historia de Usuario está lista para ser abordada cuando:

- [x] Cuenta con la narrativa estándar (Como [rol], quiero [funcionalidad] para [beneficio]).
- [x] Posee criterios de aceptación claros y verificables en formato Gherkin (*Dado-Cuando-Entonces*).
- [x] Está estimada en Story Points por el equipo.
- [x] Cuenta con prioridad MoSCoW asignada (Must have, Should have, etc.).
- [x] Sus dependencias externas (contratos de eventos CloudEvents, API de Vehículos o esquemas de base de datos) están identificadas.

---

## Definición de Hecho (Definition of Done - DoD)

Una Historia de Usuario se considera finalizada cuando:

- [x] La lógica de negocio reside en el Dominio/Servicios respetando la Arquitectura Hexagonal.
- [x] Todo el código nuevo cuenta con pruebas unitarias e integración.
- [x] Cobertura de pruebas mínima del **80%** verificada con JaCoCo.
- [x] Pasa el linter **Checkstyle** sin excepciones ni advertencias ignoradas.
- [x] Revisión de **Qodo AI** atendida (sin alertas críticas pendientes).
- [x] PR aprobado por al menos un revisor del equipo y mergeado a `develop`.
- [x] Contratos OpenAPI actualizados si hubo cambios o adición de endpoints REST.
- [x] Migraciones de base de datos Flyway probadas y funcionales en PostgreSQL.
- [x] Validación funcional demostrada en la bitácora o sesión de sincronización.

---

## Estructura del Proyecto

Para mantener la coherencia del diseño arquitectónico, ubica tus archivos en el paquete correspondiente:

```text
src/
├── main/
│   ├── java/co/edu/univalle/fleetops/mantenimiento/
│   │   ├── domain/               # Núcleo del negocio en Java 21 puro (sin Spring)
│   │   │   ├── model/            # Entidades, Value Objects y Agregados
│   │   │   ├── exception/        # Excepciones de negocio
│   │   │   ├── port/in/          # Casos de uso (Inbound Ports)
│   │   │   └── port/out/         # Contratos de persistencia/comunicación (Outbound Ports)
│   │   ├── application/service/  # Implementación de casos de uso / orquestación
│   │   └── infrastructure/       # Adaptadores tecnológicos y configuración de framework
│   │       ├── adapter/in/web/   # REST Controllers, DTOs y GlobalExceptionHandler
│   │       ├── adapter/in/scheduler/ # Tareas programadas (@Scheduled)
│   │       ├── adapter/in/messaging/ # Consumidores RabbitMQ (@RabbitListener)
│   │       ├── adapter/out/persistence/ # Entidades JPA, Repositorios Spring Data, Mappers
│   │       ├── adapter/out/client/      # Clientes HTTP externos (Resilience4j)
│   │       ├── adapter/out/messaging/   # Publicadores de eventos RabbitMQ (Outbox)
│   │       └── config/           # Beans de configuración Spring (OpenAPI, RabbitMQ, WebMvc)
│   └── resources/
│       ├── db/migration/         # Scripts de migración Flyway (V1__init.sql, etc.)
│       └── application.yaml      # Configuración de la aplicación
└── test/                         # Suite de pruebas unitarias y de integración
```

---

## Soporte y Dudas

- **Dudas técnicas y bugs**: Abre un Issue en GitHub con el tag correspondiente (`bug`, `question`, `enhancement`).
- **Dudas de arquitectura o diseño**: Consúltalas con el equipo y documenta las decisiones en `solution/decisiones_y_observaciones.md` antes de implementar el código.
