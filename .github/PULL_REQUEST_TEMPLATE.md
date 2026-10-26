## Descripción del Cambio
<!-- Explica brevemente qué resuelve este PR y qué cambios introduce -->

### Historia(s) de Usuario Relacionada(s)
- Closes # <!-- Ej: Closes HU-01 -->

---

## Tipo de Cambio
- [ ] 🚀 Nueva funcionalidad (feature)
- [ ] 🐛 Corrección de bug (fix)
- [ ] 🔨 Refactorización o mejora de arquitectura
- [ ] 📝 Documentación
- [ ] ⚙️ Configuración / CI/CD

---

## Capa de la Arquitectura Hexagonal Impactada
- [ ] **Dominio:** `domain.model`, `domain.exception`
- [ ] **Puertos:** `domain.port.in`, `domain.port.out`
- [ ] **Aplicación:** `application.service`
- [ ] **Adaptadores de Entrada:** REST Controller, Scheduler, RabbitMQ Listener
- [ ] **Adaptadores de Salida:** Persistencia JPA, Cliente HTTP Vehículos, Publicador RabbitMQ
- [ ] **Infraestructura / Migraciones:** Flyway, Configuración

---

## Checklist de Calidad
- [ ] El código compila localmente sin errores (`./gradlew compileJava`).
- [ ] Se ejecutó el análisis estático de Checkstyle sin violaciones (`./gradlew checkstyleMain checkstyleTest`).
- [ ] Las pruebas unitarias e integración pasan al 100% (`./gradlew test`).
- [ ] La cobertura de pruebas supera el umbral del 80% exigido (`./gradlew jacocoTestCoverageVerification`).
- [ ] No se incluyeron credenciales ni archivos `.env` en los commits.
