# PhotosReview

[中文](./README-zh.md) | [English](./README.md)

PhotosReview is a photo review system built with **Spring Boot + SQLite**, providing:

- User login and token validation.
- Admin-side project management (create projects, assign tasks, manage users, upload and delete photos).
- Frontend project browsing and review modes (Project / History).
- Chinese/English language switching and light/dark theme switching.

## Repository Structure

- `src/main/java/com/hodastar/photosreview/`: Backend Java code (Controller / Mapper / Config / Utils / Entity).
- `src/main/resources/static/`: Frontend static pages and scripts (`index.html`, `admin.html`, `proj.html`, etc.).
- `src/main/resources/static/i18n/`: Internationalization resources (`zh.json`, `en.json`).
- `data/`: Runtime data directory (e.g., default icons, project assets).

## Requirements

- JDK 17+
- Maven 3.9+ (or use the bundled `mvnw`)

## Run Locally

```bash
# 1) Clone and enter the project
git clone <your-repo-url>
cd PhotosReview

# 2) Start
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Default URLs after startup:

- Frontend: `http://localhost:8080/`
- Admin: `http://localhost:8080/admin.html`

## Build

```bash
./mvnw clean package
java -jar target/*.jar
```

## Notes

- Frontend language switching relies on `localStorage.lang`, with copy from `static/i18n/*.json`.
- Theme switching relies on `localStorage.darkmode`.
- On first run, required tables and base data are initialized by backend logic if absent.

## Contributing

Issues and Pull Requests are welcome.
