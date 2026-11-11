FROM eclipse-temurin:25-jdk AS build
WORKDIR /what-to-watch
COPY . .
ARG W2W_VERSION
# The cache keeps Gradle and the dependencies between builds, outside the image
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :what-to-watch-boot:bootJar -Pversion=$W2W_VERSION --no-daemon --stacktrace \
 && cp what-to-watch-boot/build/libs/what-to-watch-boot-$W2W_VERSION.jar app.jar

FROM eclipse-temurin:25-jre
WORKDIR /what-to-watch
ENV TZ=Europe/Warsaw

# IMDb's non-commercial datasets, refreshed daily: rebuild the image to update the scores
ADD https://datasets.imdbws.com/title.ratings.tsv.gz https://datasets.imdbws.com/title.basics.tsv.gz imdb/

COPY --from=build /what-to-watch/app.jar .

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
