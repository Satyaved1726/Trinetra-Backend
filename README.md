# TRINETRA Backend

Production-ready Spring Boot backend for the TRINETRA Anonymous Workplace Reporting System.

## Stack

- Spring Boot 3.3
- Java 17 baseline
- Spring Security with JWT
- Spring Data JPA and Hibernate
- PostgreSQL (Supabase-compatible)
- Maven

## Environment Variables

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_PROFILES_ACTIVE`
- `SERVER_PORT` optional for local runs (Render provides `PORT`)
- `JWT_SECRET`
- `JWT_EXPIRATION`
- `APP_CORS_ALLOWED_ORIGIN_PATTERNS` optional, default allows localhost and `https://*.vercel.app`
- `APP_UPLOAD_DIR` optional, default `uploads`
- `APP_BOOTSTRAP_ADMIN_EMAIL` optional
- `APP_BOOTSTRAP_ADMIN_PASSWORD` optional
- `APP_BOOTSTRAP_ADMIN_NAME` optional

## Deployment Required Variables

- `SPRING_DATASOURCE_URL`
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

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/complaints/submit` employee complaint submission
- `POST /api/complaints/anonymous` anonymous complaint submission
- `GET /api/complaints/track/{trackingId}` anonymous tracking without login
- `GET /api/complaints/my` employee complaints
- `GET /api/complaints/all` admin complaint list
- `PUT /api/complaints/status/{id}` admin status update
- `GET /api/admin/reports`
- `PUT /api/admin/report/{id}/status`
- `POST /api/admin/respond/{reportId}`
- `GET /health`

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