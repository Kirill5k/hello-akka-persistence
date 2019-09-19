### Additional content:

* a filled-in `build.sbt` with the appropriate library dependencies
* a `docker-compose.yml` with already configured Docker containers for Postgres and Cassandra
* a simple `docker-clean.sh` script to remove Docker containers
* a `/sql` folder with a SQL script that will automatically be run in the Postgres container and create the correct tables for Akka (more on that in the PostgreSQL lecture)
* a helper script `psql.sh` to easily connect to Postgres once started
* a helper script `cqlsh.sh` to easily connect to Cassandra once started

