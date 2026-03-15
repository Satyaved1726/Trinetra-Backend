# TRINETRA Backend

Production-ready Spring Boot backend for the TRINETRA Complaint Management SaaS.

## Stack

- Spring Boot 3.3
- Java 21
- Spring Security with JWT
- Spring Data JPA and Hibernate
- PostgreSQL (Supabase-compatible)
- Multipart file uploads (images, videos, PDFs, documents)
- Maven

## Environment Variables

- `SPRING_DATASOURCE_URL`
- `DATABASE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_PROFILES_ACTIVE`
- `SERVER_PORT` optional for local runs (Render provides `PORT`)
- `JWT_SECRET`
- `JWT_EXPIRATION`
- `STORAGE_URL` optional external file base URL (Supabase/S3 compatible)
- `APP_CORS_ALLOWED_ORIGIN_PATTERNS` optional, default allows localhost and `https://*.vercel.app`
- `APP_UPLOAD_DIR` optional, default `uploads`
- `APP_BOOTSTRAP_ADMIN_EMAIL` optional
- `APP_BOOTSTRAP_ADMIN_PASSWORD` optional
- `APP_BOOTSTRAP_ADMIN_NAME` optional

## Deployment Required Variables

- `SPRING_DATASOURCE_URL` or `DATABASE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_PROFILES_ACTIVE`
- `JWT_SECRET`
- `JWT_EXPIRATION`

Supabase pooler production JDBC format:

`jdbc:postgresql://aws-1-ap-southeast-2.pooler.supabase.com:6543/postgres?pgbouncer=true`

Supabase direct JDBC format:

`jdbc:postgresql://db.hbnjxekpkomrezeiogxm.supabase.co:5432/postgres`

## Run Locally

```bash
./mvnw clean package -DskipTests
java -jar target/trinetra-backend-0.0.1-SNAPSHOT.jar
```

On Windows:

```powershell
.\mvnw.cmd clean package -DskipTests
java -jar target\trinetra-backend-0.0.1-SNAPSHOT.jar
```

## API Summary

- Public/Auth:
  - `POST /api/auth/admin/login`
  - `POST /api/auth/employee/login`
  - `POST /api/auth/register`
  - `POST /api/complaints`
  - `POST /api/complaints/upload`
  - `GET /api/complaints/track/{trackingId}`
- Employee:
  - `POST /api/employee/complaints`
  - `GET /api/employee/my-complaints`
- Admin:
  - `GET /api/admin/complaints`
  - `GET /api/admin/complaints/{id}`
  - `PUT /api/admin/complaints/status`
  - `GET /api/admin/users`
  - `GET /api/admin/analytics`
  - `GET /api/admin/stats`
- Health:
  - `GET /api/health`
  - `GET /actuator/health`

## Roles And Security

- `ADMIN` routes: `/api/admin/**`
- `EMPLOYEE` routes: `/api/employee/**`
- Public routes: `/api/auth/**`, `POST /api/complaints`, `GET /api/complaints/track/**`, `/uploads/**`
- JWT required for protected routes via `Authorization: Bearer <token>`

## Complaint Lifecycle

- `PENDING`
- `UNDER_REVIEW`
- `INVESTIGATING`
- `RESOLVED`
- `REJECTED`

## Multipart Report Submission

Use a JSON part named `request` and optional file parts named `files`.

Example JSON payload:

```json
{
  "title": "Manager harassment complaint",
  "description": "Detailed issue description...",
  "category": "Harassment",
  "anonymous": true
}
```

## Render Deployment

The repository includes [render.yaml](render.yaml). Set the required environment variables in Render and deploy using the generated Maven wrapper.