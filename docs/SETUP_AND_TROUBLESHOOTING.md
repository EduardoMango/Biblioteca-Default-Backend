# Guía de Instalación, Configuración y Solución de Problemas (Setup & Troubleshooting)

Esta guía proporciona instrucciones paso a paso para configurar el entorno de desarrollo local, inicializar la aplicación **Biblioteca API** y resolver de forma autónoma los problemas e incidencias más comunes.

---

## 1. Requisitos Previos del Sistema

Antes de comenzar, asegúrate de tener instalados los siguientes componentes:

| Herramienta | Versión Mínima Requerida | Comando de Verificación | Notas |
| :--- | :---: | :--- | :--- |
| **Java JDK** | **21 LTS** | `java -version` | Recomendado: Eclipse Temurin, Amazon Corretto o OpenJDK 21. |
| **Git** | 2.30+ | `git --version` | Control de versiones. |
| **Maven** | 3.9+ | `./mvnw -version` | Se incluye el Maven Wrapper (`mvnw`), por lo que no es estrictamente necesario instalar Maven globalmente. |
| **Docker** *(Opcional)* | 24.0+ | `docker --version` | Útil si deseas utilizar PostgreSQL en contenedor en lugar de H2 en memoria. |

---

## 2. Configuración del IDE

El proyecto utiliza **Lombok** para reducir código repetitivo y **MapStruct** para la conversión tipada y de alto rendimiento entre entidades JPA y DTOs. Ambas herramientas requieren habilitar el **Procesamiento de Anotaciones** (*Annotation Processing*) en tu IDE.

### 2.1. IntelliJ IDEA (Recomendado)

1. Abre el proyecto en IntelliJ (`File -> Open -> Selecciona la carpeta Biblioteca`).
2. Ve a **Settings / Preferences** (`Ctrl + Alt + S` en Linux/Windows, `Cmd + ,` en macOS):
   - Navega a: **Build, Execution, Deployment** $\rightarrow$ **Compiler** $\rightarrow$ **Annotation Processors**.
   - Marca la casilla: **Enable annotation processing**.
   - Haz clic en **Apply** y **OK**.
3. Verifica que el plugin de **Lombok** esté habilitado (viene integrado de forma nativa en versiones recientes de IntelliJ).
4. Opcional: Instala el plugin **MapStruct Support** desde el Marketplace de IntelliJ para autocompletado en mappers.
5. Ejecuta un rebuild del proyecto: `Build -> Rebuild Project`.

### 2.2. Visual Studio Code

1. Instala el paquete de extensiones **Extension Pack for Java** (Microsoft).
2. Instala la extensión **Lombok Annotations Support for VS Code**.
3. Abre el archivo de configuración `.vscode/settings.json` y asegúrate de contar con:
   ```json
   {
     "java.configuration.updateBuildConfiguration": "automatic",
     "java.compile.nullAnalysis.mode": "automatic"
   }
   ```
4. Limpia el workspace de Java si es necesario: Presiona `Ctrl + Shift + P` $\rightarrow$ `Java: Clean Java Language Server Workspace`.

---

## 3. Configuración de Variables de Entorno (`.env`)

El proyecto sigue la metodología *Twelve-Factor App*, desacoplando la configuración sensible del código fuente mediante variables de entorno gestionadas por `spring-dotenv`.

### 3.1. Crear archivo `.env`

En la raíz del proyecto, copia el archivo de plantilla `.env.example`:

```bash
cp .env.example .env
```

### 3.2. Explicación de Variables

Abre tu archivo `.env` y configura los valores requeridos:

```dotenv
# ==============================================================================
# PUERTO DEL SERVIDOR
# ==============================================================================
# El puerto por defecto es 8081 para evitar colisiones con el puerto estándar 8080
SERVER_PORT=8081

# ==============================================================================
# BASE DE DATOS (H2 en memoria por defecto / PostgreSQL para producción)
# ==============================================================================
SPRING_DATASOURCE_URL=jdbc:h2:mem:biblioteca;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
SPRING_DATASOURCE_USERNAME=sa
SPRING_DATASOURCE_PASSWORD=
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect
SPRING_JPA_HIBERNATE_DDL_AUTO=update

# ==============================================================================
# SEGURIDAD Y JWT (JSON Web Tokens)
# ==============================================================================
# Llave secreta simétrica HMAC-SHA256 (Mínimo 256 bits / 32 caracteres)
JWT_SECRET=tu_secreto_super_seguro_y_largo_de_al_menos_32_caracteres_123456
# Tiempo de expiración del Access Token (86400000 ms = 24 horas)
JWT_EXPIRATION=86400000
# Tiempo de expiración del Refresh Token (604800000 ms = 7 días)
JWT_REFRESH_EXPIRATION=604800000

# ==============================================================================
# SERVICIO DE CORREO ELECTRÓNICO (Mailtrap para Dev/Testing)
# ==============================================================================
SPRING_MAIL_HOST=sandbox.smtp.mailtrap.io
SPRING_MAIL_PORT=2525
SPRING_MAIL_USERNAME=tu_usuario_mailtrap
SPRING_MAIL_PASSWORD=tu_password_mailtrap

# ==============================================================================
# SERVICIOS EXTERNOS (Google Books API)
# ==============================================================================
# Clave opcional para mayor cuota en importación de libros por ISBN
GOOGLE_BOOKS_API_KEY=tu_google_books_api_key_opcional
```

> [!TIP]
> **Generar un secreto JWT seguro en la terminal:**
> En Linux/macOS puedes ejecutar:
> ```bash
> openssl rand -base64 32
> ```
> Y pegar el resultado en `JWT_SECRET`.

> [!NOTE]
> **Configuración de Mailtrap (Sandbox de Emails gratuito):**
> Para probar la verificación de cuentas y recuperación de contraseñas sin enviar correos a destinatarios reales:
> 1. Crea una cuenta gratuita en [Mailtrap.io](https://mailtrap.io).
> 2. Ve a **Email Testing** $\rightarrow$ **Inboxes** $\rightarrow$ **My Inbox**.
> 3. En **Integrations**, selecciona **Java / Spring Boot** y copia tu `username` y `password` en tu archivo `.env`.

---

## 4. Compilación y Ejecución

### 4.1. Ejecución con Maven Wrapper

En entornos Unix (Linux/macOS):
```bash
# Otorgar permisos de ejecución si es la primera vez
chmod +x mvnw

# Compilar y descargar dependencias
./mvnw clean install -DskipTests

# Iniciar la aplicación
./mvnw spring-boot:run
```

En entornos Windows (PowerShell / CMD):
```powershell
.\mvnw.cmd clean install -DskipTests
.\mvnw.cmd spring-boot:run
```

### 4.2. Ejecutar la Suite de Pruebas Automatizadas

El proyecto cuenta con más de 150 pruebas unitarias y de integración:
```bash
./mvnw test
```

---

## 5. Puntos de Acceso del Sistema

Una vez que el mensaje `Started BibliotecaApplication in X.XXX seconds` aparezca en consola, puedes acceder a los siguientes recursos:

| Servicio | URL Local | Credenciales por Defecto |
| :--- | :--- | :--- |
| **API Base** | `http://localhost:8081/api` | Requiere Bearer JWT (excepto auth y libros públicos). |
| **Swagger UI (OpenAPI 3)** | [`http://localhost:8081/swagger-ui.html`](http://localhost:8081/swagger-ui.html) | Acceso público en navegador. |
| **OpenAPI Docs (JSON)** | [`http://localhost:8081/v3/api-docs`](http://localhost:8081/v3/api-docs) | Especificación JSON Swagger. |
| **Consola H2 (Base de Datos)**| [`http://localhost:8081/h2-console`](http://localhost:8081/h2-console) | **JDBC URL:** `jdbc:h2:mem:biblioteca`<br>**User:** `sa`<br>**Password:** *(vacía)* |

---

## 6. Guía de Solución de Problemas (Troubleshooting)

### 6.1. Conflicto de Puertos (`Port 8080` / `Port 8081 was already in use`)

#### Síntoma:
Al iniciar la aplicación, la consola muestra:
```text
APPLICATION FAILED TO START
Description:
Web server failed to start. Port 8081 was already in use.
```

#### Causa:
1. Otro servicio local (un contenedor de Docker, Jenkins, Tomcat u otra aplicación Spring) está ocupando el puerto.
2. Nota: La aplicación usa por defecto el puerto **8081** específicamente para evitar el puerto común **8080**. Sin embargo, si 8081 también está ocupado por otra instancia previa huérfana de Java, ocurrirá este error.

#### Solución:
- **Opción A (Liberar el puerto en Linux):**
  ```bash
  # Identificar el proceso ocupando el puerto 8081
  lsof -i :8081
  
  # Terminar el proceso
  fuser -k 8081/tcp
  ```
- **Opción B (Liberar el puerto en Windows):**
  ```powershell
  netstat -ano | findstr :8081
  taskkill /PID <PID_NUMERO> /F
  ```
- **Opción C (Cambiar el puerto de la aplicación):**
  Edita tu archivo `.env` y define un puerto libre:
  ```dotenv
  SERVER_PORT=8082
  ```

---

### 6.2. Errores de Compilación de Lombok o MapStruct (`cannot find symbol`)

#### Síntoma:
Al ejecutar `./mvnw compile` o desde el IDE, aparecen errores como:
- `cannot find symbol method builder()`
- `cannot find symbol method getId()`
- `BookMapperImpl is not an abstract and does not override abstract method...`

#### Solución:
1. En tu IDE, confirma que **Annotation Processing** está activo (ver Sección 2).
2. Limpia el directorio de clases compiladas y regenera los fuentes de MapStruct:
   ```bash
   ./mvnw clean compile
   ```
3. En IntelliJ IDEA: `File -> Invalidate Caches... -> Invalidate and Restart`.

---

### 6.3. Swagger UI arroja `401 Unauthorized` o pantalla de login

#### Síntoma:
Al abrir en el navegador `http://localhost:8081/swagger-ui.html`, se redirige a un login de Spring Security o responde con error HTTP `401`.

#### Causa:
1. Estás abriendo el puerto equivocado (por ejemplo `http://localhost:8080` donde corre otro contenedor ajeno a este proyecto).
2. O la versión en ejecución no tiene cargados los filtros de seguridad con exclusión de endpoints públicos.

#### Solución:
1. Confirma en los logs de arranque qué puerto está utilizando la aplicación:
   ```text
   Tomcat initialized with port 8081 (http)
   ```
2. Accede directamente a la URL canónica:
   [`http://localhost:8081/swagger-ui/index.html`](http://localhost:8081/swagger-ui/index.html)
3. En caso de usar un cliente API (Postman / cURL), recuerda que los endpoints de negocio protegidos requieren la cabecera:
   `Authorization: Bearer <tu_token_jwt>`

---

### 6.4. Fallo al Enviar Correos (`MailAuthenticationException`)

#### Síntoma:
Al registrar un usuario o solicitar recuperación de contraseña, la aplicación responde con error `500 Internal Server Error` o en el log aparece:
```text
org.springframework.mail.MailAuthenticationException: 535 5.7.8 Authentication credentials invalid
```

#### Solución:
1. Abre tu archivo `.env`.
2. Verifica que `SPRING_MAIL_USERNAME` y `SPRING_MAIL_PASSWORD` no tengan comillas innecesarias o espacios en blanco al final.
3. Asegúrate de que el puerto sea `2525` o `587` (Mailtrap no suele utilizar el puerto 25 estándar en redes residenciales).

---

### 6.5. Los Datos de la Base de Datos Desaparecen al Reiniciar

#### Síntoma:
Al detener y volver a ejecutar la aplicación, los usuarios registrados o libros creados ya no existen.

#### Causa:
La configuración por defecto utiliza **H2 In-Memory** (`jdbc:h2:mem:biblioteca`), la cual reside exclusivamente en la memoria RAM del proceso Java.

#### Solución:
Este es el comportamiento esperado para entornos de pruebas ágiles. Si deseas que los datos persistan:
- **Opción A (H2 en archivo local):**
  Cambia la URL en tu `.env` a:
  ```dotenv
  SPRING_DATASOURCE_URL=jdbc:h2:file:./data/biblioteca_db;DB_CLOSE_DELAY=-1
  ```
- **Opción B (PostgreSQL con Docker):**
  Inicia una base de datos PostgreSQL local:
  ```bash
  docker run -d --name postgres-biblioteca -e POSTGRES_DB=biblioteca_db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:16-alpine
  ```
  Y actualiza tu `.env`:
  ```dotenv
  SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/biblioteca_db
  SPRING_DATASOURCE_USERNAME=postgres
  SPRING_DATASOURCE_PASSWORD=postgres
  SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
  SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
  ```

---

## 7. Comandos Útiles de Mantenimiento

```bash
# Formatear y verificar linter / estilo
./mvnw clean test-compile

# Generar empaquetado JAR ejecutable de producción
./mvnw clean package -DskipTests

# Ejecutar el archivo JAR generado
java -jar target/Biblioteca-0.0.1-SNAPSHOT.jar
```

