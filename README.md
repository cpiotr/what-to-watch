# what-to-watch
## Building
```
./gradlew clean build
```
The tests start Redis in Docker, so Docker must be running. The tests against the live sites run apart, as CI runs
them on weekdays:
```
./gradlew downloadImdbDatasets integrationTest
```

## Running
With Docker Compose, then open http://localhost:8080/:
```
docker-compose up --build
```
Locally, with Redis from Docker Compose, which publishes it to this machine only. `bootRun` downloads IMDb's
datasets into `imdb/` first.
```
docker-compose up -d cache
./gradlew bootRun
```

### Settings
The settings and their defaults are records in `what-to-watch-boot/src/main/java/pl/ciruk/whattowatch/boot/config`:
`W2wProperties` (`w2w.*`), `HttpProperties` (`http.*`) and `RedisProperties` (`redis.*`). `application.properties`
and the `environment` in `docker-compose.yml` set the same values, to show how each way of running sets them, so
change all three together. Only Redis's host differs. To try a value once:
```
./gradlew bootRun --args='--w2w.suggestions.rating-threshold=6'
```
`w2w.bootstrap.enabled=false` skips the search of the first page that warms up the caches after a start.

### Health and metrics
On port 8081, which Docker Compose publishes to this machine only:
```
curl localhost:8081/actuator/health
curl localhost:8081/actuator/metrics
curl localhost:8081/actuator/metrics/Suggestions.response
curl localhost:8081/actuator/prometheus
```
The last one has all metrics at once, in the format Prometheus scrapes.

### Benchmarks
The benchmark scores titles from pages recorded from the sites, read through Redis in Docker:
```
./gradlew jmh
```
When a site changes its pages, record them again. This asks the live sites, at one request per second.
```
./gradlew recordBenchmarkPages
```

### GC logs
In Docker Compose the JVM logs garbage collection to `logs/gc.log` in the `w2w-logs` volume rather than to the app's
log. Earlier runs' logs, and parts over 10 MB, go to `gc.log.0` to `gc.log.4` in turn, so the newest of those is
the most recently modified.
```
docker-compose exec w2w tail -f logs/gc.log
docker-compose exec w2w ls -lt logs
docker-compose cp w2w:/what-to-watch/logs/gc.log .
```
The volume is writable by uid 1000 only. With a different `W2W_UID`, give it to that user once:
```
docker-compose run --rm --user root --entrypoint chown w2w -R "$(id -u):$(id -g)" logs
```

## My ratings
Ratings given in the app go to `what-to-watch-ratings/src/main/resources/rated-films.csv`, the file the app learns
my taste from: Docker Compose mounts its directory into the container, and `./gradlew bootRun` writes to it
directly. Commit the file to keep them. The app reads it on every start, so ratings changed in git count from then
on. Elsewhere the app writes to `w2w.ratings.file`, which starts as a copy of the bundled ratings, and
`/resources/ratings` downloads it.
