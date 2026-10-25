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

