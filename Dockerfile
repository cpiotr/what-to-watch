FROM gradle:8.7.0-jdk21-alpine as build
MAINTAINER c.piotre@gmail.com

VOLUME /tmp

ARG W2W_VERSION
ENV W2W_VERSION=${W2W_VERSION}

COPY ./ /what-to-watch/
WORKDIR /what-to-watch
RUN sh -c 'gradle clean build -Pversion=$W2W_VERSION --stacktrace -x check'

FROM mcr.microsoft.com/playwright/java:v1.43.0-jammy
MAINTAINER c.piotre@gmail.com

VOLUME /tmp

ARG W2W_VERSION
ENV W2W_VERSION=${W2W_VERSION}

ENV W2W_TITLES_PAGES_PER_REQUEST=10
ENV W2W_THREAD_POOL_SIZE=16
ENV W2W_THREAD_POOL_USE_VIRTUAL=false
ENV W22_FILM_SCORE_THRESHOLD=0.55
ENV W2W_LOGGING_LEVEL=INFO
ENV W2W_HTTP_CLIENT_DEFAULT_DELAY_MILLIS=100
ENV W2W_HTTP_CLIENT_DELAY_BY_DOMAIN_MILLIS={}
ENV LOGGING_LEVEL=INFO

ENV REDIS_HOST 172.17.0.2

ENV JMX_OPTS="-Dcom.sun.management.jmxremote \
	-Dcom.sun.management.jmxremote.local.only=false \
	-Dcom.sun.management.jmxremote.authenticate=false \
	-Dcom.sun.management.jmxremote.port=9999 \
	-Dcom.sun.management.jmxremote.ssl=false"

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
EXPOSE 9999

CMD sh -c "java \
		-Djava.security.egd=file:/dev/./urandom \
		-Dw2w.titles.pagesPerRequest=$W2W_TITLES_PAGES_PER_REQUEST \
		-Dw2w.pool.size=$W2W_THREAD_POOL_SIZE \
		-Dw2w.pool.useVirtualPool=$W2W_THREAD_POOL_USE_VIRTUAL \
		-Dw2w.suggestions.filter.scoreThreshold=$W22_FILM_SCORE_THRESHOLD \
		-Dhttp.connection.delayByDomain.default=$W2W_HTTP_CLIENT_DEFAULT_DELAY_MILLIS \
		-Dhttp.connection.delayByDomain.map=$W2W_HTTP_CLIENT_DELAY_BY_DOMAIN_MILLIS \
		-Dredis.host=$REDIS_HOST \
		-Dlogging.level.root=$LOGGING_LEVEL\
		-Dlogging.level.pl.ciruk=$W2W_LOGGING_LEVEL\
		$JMX_OPTS \
		$JVM_OPTS \
		-jar ./what-to-watch-boot/build/libs/what-to-watch-boot-$W2W_VERSION.jar"
