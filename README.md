# MesaTech Cloud: backend

BFF y microservicios de MesaTech Cloud (EP2, DSY1107). Spring Boot 4, Java 21, PostgreSQL.

| Carpeta | Rol | Puerto |
|---|---|---|
| `bff` | Seguridad (JWT de Entra ID) y coordinacion. No accede a la BD | 8080 |
| `ms-solicitudes` | Solicitudes y sus estados | 8081 |
| `ms-catalogo` | Categorias y prioridades | 8082 |

Este repo nace de separar el backend del repo `mesatech-cloud` (EP1). El frontend vive en
`mesatech-web` y RabbitMQ en `mesatech-rabbit`.

## Levantar en local

```bash
cp .env.example .env
docker compose up -d --build
```

Sin Entra ID, para pruebas locales:

```bash
docker compose -f docker-compose.yml -f docker-compose.sinauth.yml up -d --build
```

Con el perfil `sinauth` el BFF acepta los headers `X-Test-Rol` y `X-Test-Usuario`. Nunca usarlo en EC2.

## Despliegue en EC2 (EP2)

Cada microservicio corre en su propia EC2 y usa su propia RDS. El BFF va en la EC2 web junto al front
(ver `deploy/` en el repo `mesatech-web`).

| EC2 | Carpeta | Base de datos |
|---|---|---|
| `ep2-catalogo` | `deploy/catalogo` | RDS `ep2-rds-catalogo` |
| `ep2-solicitudes` | `deploy/solicitudes` | RDS `ep2-rds-solicitudes` |

1. Crear el usuario de la aplicacion en la RDS con el script de `sql/` (la clave se pasa con `-v clave=...`).
2. `cd deploy/<servicio>`, copiar `.env.example` a `.env` y completar las claves.
3. `docker compose up -d --build`

Las RDS de PostgreSQL 16 exigen SSL, por eso la URL de conexion lleva `?sslmode=require`.
El formato del mensaje de RabbitMQ esta en `docs/evento-categoria.md`.
