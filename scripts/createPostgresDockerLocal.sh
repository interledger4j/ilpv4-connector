#!/bin/bash

docker run -d -p 5432:5432 -e POSTGRES_DB=connector --name connector-postgres postgres