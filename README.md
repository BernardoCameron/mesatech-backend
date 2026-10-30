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
