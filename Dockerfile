FROM eclipse-temurin:21-jdk as build
MAINTAINER c.piotre@gmail.com

VOLUME /tmp

ARG W2W_VERSION
ENV W2W_VERSION=${W2W_VERSION}

COPY ./ /what-to-watch/
WORKDIR /what-to-watch
RUN sh -c './gradlew clean build -Pversion=$W2W_VERSION --stacktrace -x check'

FROM eclipse-temurin:21-jre
MAINTAINER c.piotre@gmail.com

VOLUME /tmp

ARG W2W_VERSION
ENV W2W_VERSION=${W2W_VERSION}

ENV W2W_TITLES_PAGES_PER_REQUEST=10
ENV W2W_THREAD_POOL_SIZE=16
ENV W2W_THREAD_POOL_USE_VIRTUAL=false
ENV W2W_FILM_SCORE_THRESHOLD=0.55
ENV W2W_LOGGING_LEVEL=INFO
ENV W2W_HTTP_REQUESTS_PER_SECOND=2
ENV LOGGING_LEVEL=INFO

ENV REDIS_HOST=cache

ENV JVM_OPTS="-Xmx2G -Xms2G \
	-XX:+TieredCompilation \
	-XX:InitialCodeCacheSize=128m \
	-XX:ReservedCodeCacheSize=128m \
	-XX:+PreserveFramePointer \
	-XX:+UnlockExperimentalVMOptions \
	-XX:+UseZGC \
	-XX:+UseNUMA \
	-Xlog:gc:stdout:time"

ENV TZ "Europe/Warsaw"

COPY --from=build /what-to-watch /what-to-watch
WORKDIR /what-to-watch

EXPOSE 8080

CMD sh -c "java \
		-Djava.security.egd=file:/dev/./urandom \
		-Dw2w.titles.pages-per-request=$W2W_TITLES_PAGES_PER_REQUEST \
		-Dw2w.pool.size=$W2W_THREAD_POOL_SIZE \
		-Dw2w.pool.use-virtual=$W2W_THREAD_POOL_USE_VIRTUAL \
		-Dw2w.suggestions.score-threshold=$W2W_FILM_SCORE_THRESHOLD \
		-Dhttp.requests-per-second=$W2W_HTTP_REQUESTS_PER_SECOND \
		-Dredis.host=$REDIS_HOST \
		-Dlogging.level.root=$LOGGING_LEVEL\
		-Dlogging.level.pl.ciruk=$W2W_LOGGING_LEVEL\
		$JVM_OPTS \
		-jar ./what-to-watch-boot/build/libs/what-to-watch-boot-$W2W_VERSION.jar"
