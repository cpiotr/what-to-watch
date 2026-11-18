# what-to-watch
## Building 
```
./gradlew clean build
```

## Running
```
docker-compose build
docker-compose up
xdg-open http://localhost:8080/
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
