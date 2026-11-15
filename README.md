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

## My ratings
Ratings given in the app go to `what-to-watch-neural/src/main/resources/rated-films.csv`, the file the app learns
my taste from: Docker Compose mounts its directory into the container, and `./gradlew bootRun` writes to it
directly. Commit the file to keep them. The app reads it on every start, so ratings changed in git count from then
on. Elsewhere the app writes to `w2w.ratings.file`, which starts as a copy of the bundled ratings, and
`/resources/ratings` downloads it.
