# Observaciones, Inconsistencias y Decisiones Pendientes - Módulo de Mantenimiento

Este documento consolida las discrepancias, inconsistencias y decisiones de diseño identificadas al contrastar los requerimientos de la solución:
- **Product Backlog** (`Mantenimiento_ProductBacklogV2.0.xlsx`)
- **Matriz de Endpoints y Eventos** (`FleetOps_Endpoints_y_Eventos_Modulo_Mantenimiento.xlsx`)
- **Definición de Eventos** (`Definicion de eventos.docx`)
- **Documento de Arquitectura de Software** (`MANTENIMIENTO_DAS_V_0_3.docx`)
- **Configuración del Proyecto** (`build.gradle`, `application.yaml`)

---

## 1. Convención de Endpoints REST
* **Decisión Confirmada:** Se adoptará la convención en **inglés** (`/maintenance/...` y `/technicians/...`) según la hoja de convenciones de endpoints (`FleetOps_Endpoints_y_Eventos_Modulo_Mantenimiento.xlsx`).
  * Endpoints principales:
    * `POST /maintenance/rules` (o reglas preventivas de HU-01)
    * `POST /maintenance` (Creación manual de orden)
    * `GET /maintenance` (Listado y filtros de órdenes)
    * `GET /maintenance/my_orders` (Órdenes asignadas al técnico)
    * `GET /maintenance/{id}` (Detalle completo de la orden)
    * `PUT /maintenance/{id}/technician` (Asignación de técnico)
    * `PUT /maintenance/{id}/status` (Actualizar estado a `IN_PROGRESS` o `COMPLETED`)
    * `POST /maintenance/{id}/logs` (Bitácora de revisiones y avances técnicos)
    * `POST /technicians` y `GET /technicians` (Gestión de técnicos)

---

## 2. Formato de Identificadores
* **Decisión Confirmada:** Se utilizarán **UUID v4** estándar para todas las claves primarias y parámetros de ruta de las entidades (órdenes, reglas, avances/logs, técnicos).

---

## 3. Liberación del Vehículo al Finalizar Mantenimiento (HU-08)
* **Decisión Confirmada:** La reactivación del vehículo se realizará exclusivamente a través del evento asíncrono **`MantenimientoFinalizadoEvent`** en RabbitMQ (mediante el patrón Transactional Outbox), desacoplando Mantenimiento del MS Vehículos bajo la arquitectura EDA descrita en el DAS.

---

## 4. Estado Inicial de Órdenes Correctivas
* **Decisión:** Catálogo de estados unificado:
  * `PROGRAMADO_PENDIENTE` (o `SCHEDULED_PENDING`)
  * `EN_PROCESO` (o `IN_PROGRESS`)
  * `COMPLETADO` (o `COMPLETED`)
  * `CANCELADO` (o `CANCELLED` por compensación)

---

## 5. Estándar de Mensajería
* **Decisión:** Eventos bajo especificación **CloudEvents v1.0** transportados vía RabbitMQ en formato JSON.

---

## 6. Paquete Base del Código Fuente
* **Decisión:** Refactorizar el paquete raíz a `co.edu.univalle.fleetops.mantenimiento` según lo definido en `build.gradle` y alineado con la estructura de arquitectura hexagonal del DAS.



```text
src/main/java/co/edu/univalle/fleetops/mantenimiento/
├── MantenimientoApplication.java          # Entrada principal Spring Boot + @EnableScheduling
├── domain/
│   ├── model/
│   │   ├── MaintenanceOrder.java          # Entidad raíz con reglas y transiciones de estado
│   │   ├── MaintenanceRule.java           # Reglas preventivas (GLOBAL, CATEGORIA, VEHICULO)
│   │   ├── MaintenanceLog.java            # Bitácora cronológica y avances técnicos
│   │   ├── Technician.java                # Datos del técnico de mantenimiento
│   │   ├── OutboxMessage.java             # Mensajes para Transactional Outbox Pattern
│   │   └── enums/
│   │       ├── MaintenanceStatus.java     # SCHEDULED_PENDING, IN_PROGRESS, COMPLETED, CANCELLED
│   │       ├── MaintenanceType.java       # PREVENTIVE, CORRECTIVE
│   │       ├── MaintenanceOrigin.java     # SCHEDULER, INCIDENT, MANUAL
│   │       ├── RuleScope.java             # GLOBAL, CATEGORY, VEHICLE
│   │       ├── VehicleCategory.java       # CAMIONETA, AUTOMOVIL, CARGA_PESADA
│   │       ├── LogType.java               # REVISION, PROGRESS, INCIDENT_UPDATE, etc.
│   │       └── OutboxStatus.java          # PENDING, SENT, FAILED
│   ├── exception/
│   │   ├── DomainException.java           # Base de excepciones de dominio
│   │   ├── BusinessRuleException.java     # 422 - Violación de reglas de negocio
│   │   ├── InvalidStateTransitionException.java # 409/422 - Saltos de estado inválidos
│   │   ├── EntityNotFoundException.java   # 404 - Recursos no encontrados
│   │   └── ResourceConflictException.java # 409 - Duplicados y conflictos
│   └── port/
│       ├── in/                            # Casos de Uso (Inbound Ports)
│       │   ├── RegisterRuleUseCase.java               # HU-01
│       │   ├── ExecutePreventiveInspectionUseCase.java# HU-02
│       │   ├── RegisterManualOrderUseCase.java        # HU-03
│       │   ├── ProcessReportedIncidentUseCase.java    # HU-04
│       │   ├── QueryOrdersUseCase.java                # HU-05, HU-07
│       │   ├── GetOrderDetailUseCase.java             # HU-06
│       │   ├── UpdateOrderStatusUseCase.java          # HU-08
│       │   ├── RegisterProgressUseCase.java           # HU-09
│       │   ├── PublishPendingOutboxUseCase.java       # Outbox Relay
│       │   └── ManageTechniciansUseCase.java          # Gestión de técnicos
│       └── out/                           # Puertos de Salida (Outbound Ports)
│           ├── OrderRepositoryPort.java
│           ├── RuleRepositoryPort.java
│           ├── ProgressRepositoryPort.java
│           ├── TechnicianRepositoryPort.java
│           ├── OutboxRepositoryPort.java
│           ├── VehicleServiceClientPort.java
│           └── EventPublisherPort.java
```

---

## 7. Comandos Fundamentales del Proyecto (Gradle)

Para Windows PowerShell se utiliza `.\gradlew.bat` (en Linux/macOS o CI/CD usar `./gradlew`).

### A. Compilación
* **Compilar código principal:**
  ```powershell
  .\gradlew.bat compileJava
  ```
* **Compilar código principal y pruebas:**
  ```powershell
  .\gradlew.bat testClasses
  ```

### B. Pruebas y Cobertura (JaCoCo)
* **Ejecutar pruebas unitarias e integración:**
  ```powershell
  .\gradlew.bat test
  ```
  *(Al finalizar, genera automáticamente el reporte de cobertura JaCoCo).*
* **Generar reporte HTML de cobertura JaCoCo manualmente:**
  ```powershell
  .\gradlew.bat jacocoTestReport
  ```
  *(El reporte queda disponible en: `build/reports/jacocoHtml/index.html`).*
* **Verificar el umbral mínimo de cobertura (80% requerido en build.gradle):**
  ```powershell
  .\gradlew.bat jacocoTestCoverageVerification
  ```

### C. Calidad de Código y Estilo (Checkstyle / Sonar)
* **Ejecutar análisis estático de Checkstyle:**
  ```powershell
  .\gradlew.bat checkstyleMain checkstyleTest
  ```
  *(Los reportes quedan en: `build/reports/checkstyle/main.html` y `test.html`).*
* **Verificación completa de calidad (Tests + Cobertura + Checkstyle):**
  ```powershell
  .\gradlew.bat check
  ```

### D. Ejecución y Despliegue Local
* **Iniciar el microservicio localmente:**
  ```powershell
  .\gradlew.bat bootRun
  ```
* **Construir el archivo JAR ejecutable (omitiendo tests si se requiere rapidez):**
  ```powershell
  .\gradlew.bat bootJar -x test
  ```
  *(El JAR generado se ubica en: `build/libs/maintenance-service-1.0.0-SNAPSHOT.jar`).*
* **Limpiar artefactos previos de compilación:**
  ```powershell
  .\gradlew.bat clean
  ```
* **Ciclo completo de construcción y empaquetado (Clean + Build):**
  ```powershell
  .\gradlew.bat clean build
  ```

---

## 8. Correcciones y Mejoras de Arquitectura y Persistencia (Code Review)
Tras la revisión de código automatizada (Qodo / CI), se adoptaron las siguientes decisiones de diseño para garantizar robustez, confiabilidad y concurrencia:

1. **Relaciones JPA Bidireccionales Estándar:**
   - Se reemplazó el `mappedBy` que apuntaba a columnas escalares UUID (`maintenanceOrderId`, `maintenanceLogId`) por asociaciones bidireccionales `@ManyToOne` (`order` y `maintenanceLog`) con `@OneToMany(mappedBy = "order")` y `@OneToMany(mappedBy = "maintenanceLog")`.
   - Se evita `LazyInitializationException` mediante queries con `LEFT JOIN FETCH` y `@EntityGraph(attributePaths = {"logs"})`, manteniendo `spring.jpa.open-in-view: false`.

2. **Migración Flyway V2 (`V2__schema_fixes.sql`):**
   - Inclusión de `cancel_reason TEXT` y `assigned_workshop VARCHAR(255)` en `maintenance_order`.
   - Inclusión de `routine_description TEXT` en `preventive_rule`.
   - Inclusión de `aggregate_type VARCHAR(100)`, `exchange VARCHAR(255)` y `routing_key VARCHAR(255)` en `outbox_message`.

3. **Validaciones en Dominio y Persistencia:**
   - `MaintenanceOrder.complete`: Requiere obligatoriamente los datos de atribución (`completedByType` y `completedById`), impidiendo que órdenes finalizadas queden anónimas.
   - `RulePersistenceAdapter.save`: Invoca `rule.validate()` antes de persistir, asegurando que ninguna regla con umbrales inválidos (<1,000 km, >50,000 km o días fuera de rango) pueda almacenarse.

4. **Concurrencia en Transactional Outbox:**
   - La consulta `findPendingToProcessForUpdate` incorpora `FOR UPDATE SKIP LOCKED` para permitir que múltiples instancias del microservicio o ejecuciones concurrentes del scheduler procesen mensajes sin duplicación ni bloqueos.
   - Reintento con backoff exponencial real: $10s \times 2^{(\text{attempts}-1)}$ hasta un máximo de 3 intentos antes de marcar `FAILED`.

5. **Armonización de Paginación y Consultas Agregadas:**
   - Estandarización de páginas 1-based en `TechnicianPersistenceAdapter` (`page > 0 ? page - 1 : 0`), alineándose con el contrato de `OrderPersistenceAdapter`.
   - El cálculo de `avg_repair_hours` en reportes operativos toma en cuenta los filtros de vehículo, técnico y tipo de mantenimiento mediante parámetros opcionales.

6. **Endurecimiento de Actuator:**
   - Se restringen los endpoints públicos de Spring Actuator a `health` e `info` con `show-details: when_authorized`, protegiendo métricas operativas y datos de infraestructura.


