# ILP Connector Persistence

This implementation supports a variety of databases, including Postgres, Oracle, MSSQL, and more.

## Postgresql

This section details how to use Postgresql as the underlying Connector datastore.

### Generate DDL

To generate the DDL for the Connector database, execute the following commands:

```bash
>  mvn -DskipTests clean package -P liquibase-pg-sql liquibase:updateSQL
```

This will emit a file `/target/liquibase/migrate.sql` that can be used to populate your database. Alternatively, executing this command will actully connect to the database and run this DDL for you:

```bash
> mvn -DskipTests clean package -P liquibase-pg-sql liquibase:update
```

### Reinitialization

If you want to reinitialize the database, for example during development, the following commands can be used:

```bash
> mvn -DskipTests clean package -P liquibase-pg-sql liquibase:dropAll
> mvn -DskipTests clean package -P liquibase-pg-sql liquibase:update
```

