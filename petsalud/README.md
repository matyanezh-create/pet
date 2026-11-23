# PETSALUD · Semana 5 · Forma B

Pruebas unitarias de citas, usuarios y mascotas sobre el backend del caso.

## Ejecutar

Se requiere **JDK 17**. Desde la raíz del proyecto:

```powershell
.\mvnw.cmd clean verify
```

En Linux/macOS, ejecutar `bash ./mvnw clean verify`. El primer uso puede requerir acceso a Maven Central. Para repetir una ejecución con las dependencias ya descargadas, agregar `-o`. No se necesita MySQL para esta suite. En el IDE, abrir `pom.xml` como proyecto Maven, seleccionar JDK 17 y usar el ciclo Maven `verify` para obtener también el reporte de cobertura.

- `target/surefire-reports/` contiene resultados por clase y caso.
- `target/site/jacoco/index.html` contiene el reporte navegable.
- JaCoCo requiere al menos el 81 % de cobertura de líneas en **cada** implementación de Cita, Usuario y Mascota.
- La prueba de arranque `BackendpetsaludApplicationTests` se conserva como prueba de integración para entornos con MySQL. La ejecución habitual de Surefire la excluye. Para seleccionarla explícitamente, configurar primero la base y luego ejecutar `.\mvnw.cmd "-Dtest=BackendpetsaludApplicationTests" test`. Las credenciales se leen desde `PETSALUD_DB_USER` y `PETSALUD_DB_PASSWORD`. La URL está en `application.properties` y puede sobreescribirse con `SPRING_DATASOURCE_URL`.

## Archivos y evidencia

- `CitaTest` cubre disponibilidad, estado inicial, relaciones y gestión.
- `UsuarioServiceTest` cubre registro, datos obligatorios, roles, duplicados y operaciones de usuario. `UsuarioTest` conserva las dos pruebas del caso original.
- `MascotaServiceTest` cubre registro, consulta por propietario, datos obligatorios y actualización.
- `evidencia/` incluye los registros de la línea base y la verificación, los XML de Surefire y el informe de JaCoCo. La línea base se ejecutó con las mismas pruebas y servicios sin corregir. Se agregó la firma de la consulta de disponibilidad para compilar las pruebas.

La ejecución entrega 44 casos aprobados y 100 % de líneas y ramas en los tres servicios medidos. La agenda considera ocupado el mismo instante para el mismo veterinario cuando el estado difiere de Cancelada. Las pruebas unitarias ejercitan esa regla simulando el repositorio. Se requiere una prueba de integración para verificar la consulta de MySQL y una garantía de persistencia para reservas simultáneas.

