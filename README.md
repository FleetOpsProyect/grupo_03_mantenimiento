# FleetOps — Microservicio de Mantenimiento

Gestión del ciclo de vida de órdenes de trabajo de mantenimiento preventivo y correctivo para la flota vehicular corporativa de FleetCorp, garantizando trazabilidad, disponibilidad operativa y resiliencia en un ecosistema distribuido.

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Status](https://img.shields.io/badge/status-En%20Desarrollo-orange.svg)]()
[![Quality Gate](https://img.shields.io/badge/quality%20gate-passed-brightgreen.svg)]()
[![Coverage Backend](https://img.shields.io/badge/coverage%20backend-80%25-brightgreen.svg)]()
[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-9.8.2-blue.svg)](https://gradle.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-blue.svg)](https://supabase.com/)
[![Flyway](https://img.shields.io/badge/Flyway-Migrations-red.svg)](https://flywaydb.org/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-AMQP%200--9--1-orange.svg)](https://www.rabbitmq.com/)
## Descripción

**FleetCorp / FleetOps** es una plataforma distribuida para la administración integral de flotas vehiculares comerciales y corporativas, compuesta por microservicios independientes que colaboran entre sí mediante APIs REST y eventos asíncronos:

| Módulo | Repositorio / Descripción |
|---|---|
| Mantenimiento | *(este repositorio)* |
| Vehículos | Microservicio de gestión de unidades de flota, estados operativos y telemetría |
| Incidentes | Microservicio de reporte y gestión de colisiones, siniestros y fallas en ruta |
| Asignaciones | Microservicio de programación de rutas, conductores y disponibilidad operativa |
| Notificaciones | Despacho de alertas a técnicos, administradores y conductores |

Este repositorio implementa el **microservicio de Mantenimiento**: encargado de administrar la trazabilidad completa de las órdenes de trabajo tanto preventivas como correctivas. Incorpora un motor de inspección multicriterio (tiempo y kilometraje) ejecutado automáticamente mediante un *Scheduler* diario con compensación, una bitácora técnica cronológica con soporte para registros de avance y novedades, asignación y consulta de técnicos de mantenimiento, y comunicación asíncrona desacoplada con el ecosistema a través del estándar **CloudEvents v1.0** sobre **RabbitMQ**, utilizando el patrón **Transactional Outbox** y tolerancia a fallos con **Resilience4j Circuit Breaker**.

## Arquitectura

```text
[ Clientes / Administrador / Técnicos ]
                 ↓
       [ API Gateway FleetOps ] ←── Punto de entrada centralizado
                 |                  Enrutamiento · Autenticación JWT · Rate Limiting
                 ↓ (HTTP / REST)
┌──────────────────────────────────────────────────────────────────────────┐
│             Microservicio de Mantenimiento (Spring Boot 3.4)             │
│                                                                          │
│  [ Inbound Adapters ]                                                    │
│   - REST Controllers (/maintenance, /technicians)                        │
│   - AMQP Consumer (@RabbitListener: incidente.reportado)                 │
│   - Scheduled Tasks (@Scheduled cron 02:00 AM)                           │
│                              ↓                                           │
│  [ Domain & Application Layer (Java 21 Puro) ]                           │
│   - Casos de Uso (Inbound Ports)                                         │
│   - Agregados de Dominio (MaintenanceOrder, Rules, Logs, Technicians)    │
│   - Contratos de Persistencia e Integración (Outbound Ports)             │
│                              ↓                                           │
│  [ Outbound Adapters ]                                                   │
│   - JPA Persistence (Hibernate + Repositorios Spring Data)               │
│   - HTTP Client a MS Vehículos (Resilience4j Circuit Breaker)           │
│   - Event Publisher (RabbitMQ Outbox Relay con SKIP LOCKED)              │
└───────────────────────┬──────────────────────────┬───────────────────────┘
                        ↓                          ↓
         [ Base de Datos (PostgreSQL) ]   [ Broker (RabbitMQ) ]
          (Supabase Pooler o Local)        (Topic Exchanges CloudEvents)
```

### Decisiones arquitectónicas

| Capa | Patrón | Tecnología |
|---|---|---|
| Dominio | Arquitectura Hexagonal / DDD | Java 21 puro sin dependencias de Spring Boot |
| Casos de Uso | Ports and Adapters (Inbound / Outbound) | Interfaces en `port/in` y `port/out` |
| Entrada Web | REST Controllers + DTOs + Global Exception Handler | Spring Web MVC + Springdoc OpenAPI 2.8 |
| Entrada Asíncrona | Message Consumer (Topic Exchange) | Spring AMQP (`@RabbitListener`) + CloudEvents v1.0 |
| Programación | Tareas programadas con compensación | Spring `@Scheduled` (`0 0 2 * * *`) |
| Persistencia | Repository Pattern + Transactional Outbox | Spring Data JPA + Hibernate 6 + Flyway |
| Integración HTTP | Circuit Breaker con ventana móvil y degradación | Resilience4j + RestTemplate |
| Publicación de Eventos | Outbox Relay con `FOR UPDATE SKIP LOCKED` | Spring AMQP (`RabbitTemplate`) |

## Enlaces a Documentación

Puedes consultar la documentación técnica extendida y los artefactos de diseño en el repositorio:

- **Decisiones Arquitectónicas y Observaciones:** [`solution/decisiones_y_observaciones.md`](solution/decisiones_y_observaciones.md)
- **Guía de Contribución y Flujo de Trabajo:** [`CONTRIBUTING.md`](CONTRIBUTING.md)
- **Documento de Arquitectura de Software (DAS):** `solution/MANTENIMIENTO_DAS_V_0_3.docx`
- **Product Backlog y Matriz de Historias de Usuario:** `solution/Mantenimiento_ProductBacklogV2.0.xlsx`
- **Matriz de Endpoints y Eventos:** `solution/FleetOps_Endpoints_y_Eventos_Modulo_Mantenimiento.xlsx`
- **Definición de Contratos de Eventos:** `solution/Definicion de eventos.docx`

## Stack tecnológico

### Backend
| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 LTS | Lenguaje principal (núcleo de dominio puro y adaptadores) |
| Spring Boot | 3.4.3 | Framework empresarial para inyección de dependencias y configuración |
| Spring Data JPA | 3.4.x | Mapeo objeto-relacional y persistencia |
| Flyway | 10.x | Control de versiones y migraciones automatizadas de esquemas de BD |
| RabbitMQ (AMQP) | 3.12+ | Broker de mensajería para eventos CloudEvents v1.0 |
| Resilience4j | 2.x | Tolerancia a fallos y Circuit Breaker para llamadas a MS Vehículos |
| Springdoc OpenAPI | 2.8.5 | Generación de contratos interactivos Swagger UI / OpenAPI 3.0 |

### Integración y Servicios Satélite
| Servicio / Módulo | Protocolo | Uso |
|---|---|---|
| MS Vehículos | HTTP / REST | Consulta de kilometraje, categoría y datos de vehículos |
| MS Incidentes | AMQP / RabbitMQ | Evento entrante `incidente.reportado` para órdenes correctivas |
| MS Vehículos | AMQP / RabbitMQ | Evento saliente `mantenimiento.finalizado` para reactivar flota |

### Infraestructura y datos
| Tecnología | Versión | Uso |
|---|---|---|
| PostgreSQL | 15+ / 16 | Base de datos relacional (Supabase Session Pooler o PostgreSQL local) |
| RabbitMQ | 3.12+ | Broker de mensajería asíncrona |
| Docker & Docker Compose | v2+ | Contenerización y orquestación local y de producción |
| AWS EC2 | Amazon Linux 2023 | Servidor de despliegue continuo en la nube |
| Docker Hub | — | Registro centralizado de imágenes de producción |
| GitHub Actions | — | Pipelines automatizados de CI y CD |
| SonarCloud & JaCoCo | — | Quality Gate, análisis estático y cobertura de pruebas ($\ge 80\%$) |

## Requisitos previos

Antes de ejecutar el microservicio, asegúrate de contar con:

- **Java Development Kit (JDK)**: OpenJDK 21 LTS (Eclipse Temurin recomendado).
- **Gradle**: 9.x (opcional, ya que se incluye el wrapper `./gradlew` y `.\gradlew.bat`).
- **Docker & Docker Compose**: v2.27+ para ejecución local en contenedores.
- **Base de datos PostgreSQL**: Instancia local accesible o proyecto en la nube (ej. Supabase).
- **RabbitMQ**: Broker AMQP 0-9-1 activo (puerto 5672 para datos y 15672 para consola de administración).

---

## Inicio rápido (Quick Start)

### 1. Clonar el repositorio
```bash
git clone https://github.com/FleetOpsProyect/grupo_03_mantenimiento.git
cd grupo_03_mantenimiento
```

### 2. Configurar la base de datos

Crea una base de datos PostgreSQL dedicada para el microservicio (por ejemplo `fleetops_maintenance`) en tu servidor local o en tu proveedor en la nube (como Supabase).

> Las migraciones de estructura de tablas y datos iniciales se ejecutan automáticamente al arrancar la aplicación a través de **Flyway** (`src/main/resources/db/migration`).

### 3. Configurar variables de entorno

Copia la plantilla base de variables de entorno:
```bash
cp .env.example .env
```

Edita el archivo `.env` y configura tus valores de conexión locales o de nube:

```properties
PORT=8083

# Base de datos PostgreSQL (Supabase Session Pooler o PostgreSQL local)
SPRING_DATASOURCE_URL=jdbc:postgresql://<host-db>:<puerto>/<nombre-db>?sslmode=require
SPRING_DATASOURCE_USERNAME=<tu-usuario-db>
SPRING_DATASOURCE_PASSWORD=<tu-contraseña-db>

# Broker RabbitMQ
RABBITMQ_HOST=<host-rabbitmq>
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=<tu-usuario-rabbitmq>
RABBITMQ_PASSWORD=<tu-contraseña-rabbitmq>
RABBITMQ_VIRTUAL_HOST=/

# Integración con Microservicio de Vehículos
VEHICLES_SERVICE_URL=http://<host-vehiculos>:8081
```

> [!WARNING]
> El archivo `.env` contiene credenciales privadas, se encuentra protegido por `.gitignore` y **nunca** debe comitearse al repositorio.

### 4. Ejecutar el microservicio

#### Opción A: Ejecución local con Gradle
```powershell
# En Windows PowerShell:
.\gradlew.bat bootRun

# En Linux / macOS / Bash:
./gradlew bootRun
```

#### Opción B: Ejecución contenerizada con Docker Compose
```bash
docker compose -f docker-compose.local.yml up --build -d
```

### 5. Verificar que todo está corriendo

Una vez iniciado el microservicio, verifica los endpoints principales:

| Endpoint | Descripción | URL |
|---|---|---|
| **API Base** | Recurso principal de órdenes | http://localhost:8083/maintenance |
| **Health Check** | Estado del servicio y sondas Actuator | http://localhost:8083/actuator/health |
| **Swagger UI** | Documentación interactiva OpenAPI | http://localhost:8083/swagger-ui/index.html |
| **OpenAPI Spec** | Especificación JSON de la API | http://localhost:8083/v3/api-docs |

## Comandos útiles

### Backend (Gradle)

Para Windows PowerShell usa `.\gradlew.bat` (en Linux/macOS usa `./gradlew`):

| Acción | Comando |
|---|---|
| **Compilar código fuente** | `.\gradlew.bat compileJava` |
| **Compilar código y pruebas** | `.\gradlew.bat testClasses` |
| **Ejecutar pruebas unitarias e integración** | `.\gradlew.bat test` |
| **Generar reporte HTML JaCoCo** | `.\gradlew.bat jacocoTestReport` |
| **Validar cobertura mínima ($\ge 80\%$)** | `.\gradlew.bat jacocoTestCoverageVerification` |
| **Análisis de estilo (Checkstyle)** | `.\gradlew.bat checkstyleMain checkstyleTest` |
| **Verificación integral (Build + Test + Quality)** | `.\gradlew.bat check` |
| **Iniciar aplicación localmente** | `.\gradlew.bat bootRun` |
| **Generar archivo JAR ejecutable** | `.\gradlew.bat bootJar -x test` |
| **Limpiar artefactos generados** | `.\gradlew.bat clean` |

## Estructura del repositorio

```text
grupo_03_mantenimiento/
├── .github/
│   └── workflows/                # Pipelines CI/CD (ci.yml, cd.yml, qodo-review.yml)
├── gradle/wrapper/               # Wrapper portable de Gradle
├── solution/                     # Documentación de diseño, DAS, backlog y matrices
│   ├── decisiones_y_observaciones.md
│   ├── MANTENIMIENTO_DAS_V_0_3.docx
│   └── Mantenimiento_ProductBacklogV2.0.xlsx
├── src/
│   ├── main/
│   │   ├── java/co/edu/univalle/fleetops/mantenimiento/
│   │   │   ├── domain/           # Núcleo: entidades, excepciones y puertos (Hexágono)
│   │   │   └── infrastructure/   # Adaptadores web, scheduler, messaging y persistencia
│   │   └── resources/
│   │       ├── db/migration/     # Scripts Flyway (V1__init.sql, V2__schema_fixes.sql)
│   │       └── application.yaml  # Configuración principal Spring Boot
│   └── test/                     # Pruebas unitarias, integración y arquitectura
├── .dockerignore                 # Exclusiones de contexto Docker
├── .env.example                  # Plantilla genérica y segura de variables de entorno
├── .gitignore                    # Exclusiones de control de versiones Git
├── build.gradle                  # Configuración de dependencias, JaCoCo, Sonar y Checkstyle
├── checkstyle.xml                # Reglas de estilo de código Java
├── CONTRIBUTING.md               # Guía de contribución y flujo de trabajo en equipo
├── docker-compose.local.yml      # Despliegue en contenedor para desarrollo local
├── docker-compose.prod.yml       # Orquestación de producción para AWS EC2
├── Dockerfile                    # Construcción multi-stage de imagen Docker
├── LICENSE                       # Licencia MIT del proyecto
└── README.md                     # Documentación principal
```

## Estado del proyecto

| Sprint | Estado | Descripción |
|---|---|---|
| **S0** |  Completado | Arquetipo base, arquitectura hexagonal, CI/CD con SonarCloud, DevOps inicial y DAS |
| **S1** |  Completado | HU-01 (Reglas preventivas), HU-02 (Scheduler diario multicriterio), HU-03 (Órdenes manuales) y HU-04 (Consumo de incidentes) |
| **S2** |  Planificado | HU-05 (Consulta y filtros dinámicos), HU-06 (Detalle y bitácora), HU-07 (Órdenes asignadas al técnico) |
| **S3** |  Planificado | HU-08 (Finalización de orden y reactivación vía Outbox), HU-09 (Bitácora de avances técnicos) |
| **S4** |  Planificado | Pruebas de estrés y carga, observabilidad integral y consolidación de release para producción |

## Flujo de trabajo

Consulta [`CONTRIBUTING.md`](CONTRIBUTING.md) para el detalle normativo completo. Resumen operativo:

1. **Trabajar en rama personal:** cada integrante dispone de su propia rama (`DAR`, `jsfv`, `jss`, `nco`).
2. **Validar localmente (`Gate 1`):** compilar, ejecutar Checkstyle, pruebas unitarias y verificar cobertura JaCoCo $\ge 80\%$.
3. **Push y Pull Request:** publicar cambios y abrir PR hacia la rama `develop`.
4. **Verificación automatizada (`Gate 2`):** el pipeline de GitHub Actions (`ci.yml`) ejecuta pruebas, valida cobertura, corre SonarCloud y el agente Qodo AI (`qodo-review.yml`) audita el PR.
5. **Merge a `develop`:** resolver observaciones y realizar *Squash and Merge*.
6. **Cierre de Sprint a `main`:** PR de `develop` a `main` con tag semántico (`v0.X.0`), lo cual activa el CD (`cd.yml`) para publicar la imagen en Docker Hub y desplegar en AWS EC2.

## Secrets de GitHub Actions

Para la correcta ejecución de los flujos automatizados de CI y CD, se deben configurar los siguientes secretos en el repositorio:

| Secret | Flujo | Uso |
|---|---|---|
| `SONAR_TOKEN` | CI | Token de autenticación para el análisis de calidad con SonarCloud |
| `GEMINI_API_KEY` | CI | Llave de API para el agente de revisión de PRs Qodo AI |
| `DOCKERHUB_USERNAME` | CD | Nombre de usuario en Docker Hub para construir y publicar la imagen |
| `DOCKERHUB_TOKEN` | CD | Token de Docker Hub con permisos de escritura y lectura |
| `AWS_HOST` | CD | Host o dirección IP pública del servidor AWS EC2 de producción |
| `AWS_USER` | CD | Usuario SSH del servidor AWS (`ec2-user`) |
| `AWS_SSH_PRIVATE_KEY` | CD | Llave privada SSH (.pem) para autenticación sin contraseña |
| `AWS_SSH_PORT` | CD | *(Opcional)* Puerto SSH del servidor AWS (por defecto: `22`) |

## Créditos

Desarrollado para el curso **Desarrollo de Software III (Semestre 2026-1 / 2026-2)** — **Universidad del Valle, Escuela de Ingeniería de Sistemas y Computación (Célula 3)**:

- Daniel Andrade (`DAR`)
- Jhoan Fernández (`jsfv`)
- Juan Sebastián Sierra (`jss`)
- Nicolás Córdoba (`nco`)
- Juan AMpudia  (`maj`)
## Licencia

MIT — ver [`LICENSE`](LICENSE).
