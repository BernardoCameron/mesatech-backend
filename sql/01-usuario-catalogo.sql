-- Usuario de la aplicacion para la RDS de ms-catalogo.
-- Correr desde la EC2 ep2-catalogo con el usuario maestro:
--   psql "host=<endpoint> dbname=catalogo user=mesatech_admin sslmode=require" -v clave='LA_CLAVE' -f 01-usuario-catalogo.sql
CREATE ROLE ms_catalogo LOGIN PASSWORD :'clave';
GRANT CONNECT ON DATABASE catalogo TO ms_catalogo;
-- En PostgreSQL 16 el esquema public ya no deja crear tablas a todos: ms-catalogo crea las suyas al arrancar
GRANT USAGE, CREATE ON SCHEMA public TO ms_catalogo;
