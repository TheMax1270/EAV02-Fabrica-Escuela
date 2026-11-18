# DevConnect

DevConnect es una red social para desarrolladores creada como proyecto de Fábrica Escuela 2026-2. La plataforma permite registrar usuarios y está orientada a perfiles técnicos, proyectos e interacción entre desarrolladores.

## Tecnologías

### Backend
- Java 21
- Spring Boot
- Maven
- Spring Security
- JWT
- Spring Data JPA
- Flyway
- PostgreSQL
- Supabase

### Frontend
- React
- TypeScript
- Vite
- Axios
- React Router

## Ejecución local

### Ejecutar todas las pruebas y verificaciones

Desde la raíz del proyecto, ejecutar:

```powershell
npm.cmd ci --prefix frontend
.\test-all.ps1
```

El script ejecuta el lint, las pruebas con cobertura y el build del frontend, y después
las pruebas y verificaciones del backend. No requiere una base de datos externa para
ejecutar las pruebas automatizadas.

También se pueden ejecutar por separado:

```powershell
Set-Location frontend
npm.cmd ci
npm.cmd run lint
npm.cmd test
npm.cmd run build

Set-Location ..\backend
.\mvnw.cmd -B verify
```

GitHub Actions ejecuta estas verificaciones en cada pull request y en los cambios a
`main` o `develop`; los reportes de cobertura quedan disponibles como artefactos de la
ejecución.

### 1. Configurar variables de entorno

En la raíz del proyecto, copiar `.env.example` como `.env`:

```powershell
Copy-Item .env.example .env
```

Completar el archivo `.env`:

```env
DB_URL=
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=

```

Las credenciales de PostgreSQL corresponden a la base de datos configurada en Supabase.

El archivo `.env` no debe subirse al repositorio.

### 2. Ejecutar el backend

Desde la raíz:

```powershell
.\run-backend.ps1
```

Backend:

```text
http://localhost:8080
```

### 3. Ejecutar el frontend

En otra terminal:

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

Frontend:

```text
http://localhost:5173
```
