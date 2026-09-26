# 📁 GMS File System Service

Java 21 / Spring Boot 3.3 microservice for secure file uploads in the **Global Medical System**.
Handles patient documents, reports, and medical images with JWT-gated access.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow)

## 🚀 Quick Start

```bash
mvn spring-boot:run
# Server starts on http://localhost:8081
```

## 🔧 Tech Stack
- **Java 21**, **Spring Boot 3.3.0**
- **Spring Security** + **JWT** (jjwt 0.12.3)
- **Spring Data JPA** + **MySQL 8**
- **Maven**

## 📂 Endpoints (selected)

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST   | /api/files/upload | JWT | Upload medical file |
| GET    | /api/files/{id}   | JWT | Download file by id |
| DELETE | /api/files/{id}   | JWT | Remove file (admin only) |

## 🔐 Auth
All endpoints require a JWT bearer token issued by `backend-service`. Tokens are validated
locally with the shared `JWT_SECRET`.

## 🐳 Docker

```bash
docker build -t gms-file-system-service .
docker run -p 8081:8081 gms-file-system-service
```

Or run the whole stack from the parent project — see the [main repo](https://github.com/golamrabbanyshikder/global-medical-system) for `docker-compose.yml`.

## 📁 Project Structure

```
file-system-service/
├── src/main/java/com/gms/filesystem/   # Spring Boot code
├── src/main/resources/
│   ├── application.properties         # Default config
│   └── static/                        # Static assets (if any)
├── uploads/                           # Patient files (gitignored — PHI)
├── pom.xml
├── Dockerfile
└── .gitignore
```

## ⚙️ Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/gms_db` | MySQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `gms_user` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `gms_pass` | DB password |
| `JWT_SECRET` | — | HMAC secret for JWT validation |
| `FILE_UPLOAD_DIR` | `./uploads` | Where uploaded files are stored |

## 🧪 Build & Test

```bash
mvn clean package         # Build JAR (skips tests)
mvn test                  # Run unit tests
mvn spring-boot:run       # Run locally
```

---

Part of the GMS ecosystem. See the main project: [golamrabbanyshikder/global-medical-system](https://github.com/golamrabbanyshikder/global-medical-system)
