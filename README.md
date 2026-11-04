# MesaTech Cloud: backend

BFF y microservicios de MesaTech Cloud (EP2, DSY1107). Java 21, Spring Boot 4.1.1, PostgreSQL 16. `ms-catalogo` publica
en RabbitMQ y `ms-solicitudes` consume. Cada microservicio tiene su propia base de datos.

| Módulo | Función | Puerto | Base de datos | RabbitMQ |
|---|---|---|---|---|
| `bff` | Valida el JWT de Microsoft Entra ID y llama a los microservicios | 8080 | no | no |
| `ms-catalogo` | Categorías. Propietario de `catalogo` | 8082 | RDS catálogo | publica en `catalogo.exchange` |
| `ms-solicitudes` | Solicitudes, historial, comentarios y copia local de categorías. Propietario de `solicitudes` | 8081 | RDS solicitudes | consume `solicitudes.catalogo.queue` |

Repos relacionados: `mesatech-web` (front y despliegue del BFF) y `mesatech-rabbit` (broker).

## Requisitos

Docker y Docker Compose v2 (las imágenes compilan con Maven dentro del Dockerfile). Para compilar a mano, Java 21 y
Maven. PostgreSQL 16 y RabbitMQ 4.1. Microsoft Entra ID para el login real.

## Variables de entorno

| Variable | Módulo | Descripción |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | ms-catalogo, ms-solicitudes | Conexión JDBC. En RDS la URL termina en `?sslmode=require` |
| `RABBIT_HOST`, `RABBIT_PORT`, `RABBIT_VHOST` | ms-catalogo, ms-solicitudes | Por defecto puerto 5672 y vhost `mesatech` |
| `RABBIT_USER`, `RABBIT_PASSWORD` | ms-catalogo, ms-solicitudes | `catalogo_publicador` y `solicitudes_consumidor` |
| `AZURE_TENANT_ID`, `AZURE_API_CLIENT_ID` | bff | Issuer y audiencia del JWT |
| `MS_SOLICITUDES_URL`, `MS_CATALOGO_URL` | bff | Direcciones de los microservicios |
| `CORS_ALLOWED_ORIGINS` | bff | Orígenes permitidos del front |

Los `.env.example` no llevan valores reales y los `.env` no se versionan.

## Ejecución local

```bash
cp .env.example .env
docker compose up -d --build
# sin Entra ID, solo en el PC:
docker compose -f docker-compose.yml -f docker-compose.sinauth.yml up -d --build
```

El perfil `sinauth` acepta los encabezados `X-Test-Rol` y `X-Test-Usuario` y no se usa en AWS.

## Despliegue en AWS

Una EC2 por microservicio, cada uno con su RDS exclusiva (PostgreSQL 16, sin acceso público).

| EC2 | Directorio | Base de datos | Script de usuario |
|---|---|---|---|
| `ep2-catalogo` | `deploy/catalogo` | `ep2-rds-catalogo` | `sql/01-usuario-catalogo.sql` |
| `ep2-solicitudes` | `deploy/solicitudes` | `ep2-rds-solicitudes` | `sql/02-usuario-solicitudes.sql` |

```bash
psql "host=<endpoint> dbname=<base> user=<admin> sslmode=require" -v clave='<clave>' -f sql/0X-usuario-<servicio>.sql
cd deploy/<servicio> && cp .env.example .env
docker compose up -d --build
```

El BFF se despliega junto al front, ver `deploy/` en `mesatech-web`.

## Tablas

Las crea cada microservicio al arrancar (Hibernate, `ddl-auto: update`), con el usuario de la aplicación. Los scripts
de `sql/` solo crean ese usuario y sus permisos sobre el esquema `public`.

| Módulo | Tablas |
|---|---|
| ms-catalogo | `catalogo_items` |
| ms-solicitudes | `solicitudes`, `historial_estados`, `comentarios_solicitud`, `categorias_locales` |

## Endpoints del BFF

Requieren el token de Microsoft en `Authorization`. Sin token responden 401 y sin el rol adecuado, 403.

| Ruta | Descripción | Roles |
|---|---|---|
| `POST /v1/categorias` | Crear y publicar una categoría | Administrador |
| `GET /v1/categorias` | Listar categorías | Administrador |
| `POST /v1/categorias/{id}/republicar` | Reenviar una categoría pendiente | Administrador |
| `GET /v1/categorias/disponibles` | Categorías ya recibidas por solicitudes | Todos |
| `POST /v1/solicitudes` | Crear solicitud | Todos |
| `GET /v1/solicitudes/mias`, `GET /v2/solicitudes/mias` | Solicitudes propias | Todos |
| `GET /v1/solicitudes/{id}` | Detalle con historial y comentarios | Todos |
| `POST /v1/solicitudes/{id}/comentarios` | Comentar | Todos |
| `GET /v1/solicitudes` | Todas las solicitudes | Operador, Administrador |
| `GET /v1/solicitudes/{id}/historial` | Cambios de estado | Operador, Administrador |
| `GET /v1/metricas` | Cantidades y tiempos de respuesta y resolución | Operador, Administrador |
| `PATCH /v1/solicitudes/{id}/estado` | Cambiar estado. Resolver exige una solución | Operador, Administrador |

El evento de RabbitMQ está en `docs/evento-categoria.md`.

## Operación y recuperación

Orden de arranque: RDS, RabbitMQ, `ms-catalogo` y `ms-solicitudes`, BFF y front. Detención en orden inverso con
`docker compose stop`.

- RabbitMQ caído: `POST /v1/categorias` responde 502 y la categoría queda pendiente; se reenvía con `republicar`.
- `ms-solicitudes` caído: los mensajes quedan en la cola y se procesan al volver.
- Si se recrea un usuario de RabbitMQ, reiniciar el microservicio correspondiente.

## Versión

Último commit de `main`.
