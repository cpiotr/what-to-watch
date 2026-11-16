# The OS is named, so it changes only when edited here, while Java 25 still gets its patches with `--pull`
FROM eclipse-temurin:25-jdk-resolute AS build
WORKDIR /what-to-watch
COPY . .
ARG W2W_VERSION
# The cache keeps Gradle and the dependencies between builds, outside the image
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :what-to-watch-boot:bootJar -Pversion=$W2W_VERSION --no-daemon --stacktrace \
 && cp what-to-watch-boot/build/libs/what-to-watch-boot-$W2W_VERSION.jar app.jar

FROM eclipse-temurin:25-jre-resolute
WORKDIR /what-to-watch
ENV TZ=Europe/Warsaw

# IMDb's non-commercial datasets, refreshed daily: rebuild the image to update the scores. Downloads are readable
# only by root by default, and Docker Compose runs the app as me. The mode also applies to the directory it creates,
# so it must be one that can be entered.
ADD --chmod=755 https://datasets.imdbws.com/title.ratings.tsv.gz https://datasets.imdbws.com/title.basics.tsv.gz imdb/

COPY --from=build /what-to-watch/app.jar .

# For GC logs. A new volume mounted here takes the owner from the image, so the app can write to it.
RUN install -d -o ubuntu -g ubuntu logs

EXPOSE 8080
# The image's own user (uid 1000), so the app never runs as root, even without the user Docker Compose sets
USER ubuntu

ENTRYPOINT ["java", "-jar", "app.jar"]
