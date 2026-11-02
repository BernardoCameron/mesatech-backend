-- Usuario de la aplicacion para la RDS de ms-solicitudes.
-- Correr desde la EC2 ep2-solicitudes con el usuario maestro:
--   psql "host=<endpoint> dbname=solicitudes user=mesatech_admin sslmode=require" -v clave='LA_CLAVE' -f 02-usuario-solicitudes.sql
CREATE ROLE ms_solicitudes LOGIN PASSWORD :'clave';
GRANT CONNECT ON DATABASE solicitudes TO ms_solicitudes;
GRANT USAGE, CREATE ON SCHEMA public TO ms_solicitudes;
