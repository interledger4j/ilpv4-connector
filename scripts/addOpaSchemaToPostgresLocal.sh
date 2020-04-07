#!/bin/bash

docker exec -it connector-postgres psql -U postgres -c "CREATE DATABASE opa;"
docker exec -it connector-postgres psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE opa TO postgres;"