FROM mysql:8.0

# Copy initialization SQL to auto-run on first startup
COPY init.sql /docker-entrypoint-initdb.d/init.sql
