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

`JWT_SECRET` debe ser una clave Base64 de al menos 32 bytes. Puede generarse en PowerShell con:

```powershell
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 32
$rng.GetBytes($bytes)
$rng.Dispose()
[Convert]::ToBase64String($bytes)
```

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