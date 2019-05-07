FROM adoptopenjdk/openjdk12:jdk-12.0.1_12-alpine-slim
MAINTAINER c.piotre@gmail.com

VOLUME /tmp

ARG W2W_VERSION
ENV W2W_VERSION=${W2W_VERSION}

ENV W2W_TITLES_PAGES_PER_REQUEST 10
ENV W2W_THREAD_POOL_SIZE 16
ENV W22_FILM_SCORE_THRESHOLD 0.55
ENV W2W_LOGGING_LEVEL=INFO
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
	-XX:-UseBiasedLocking \
	-XX:+PreserveFramePointer \
	-XX:+UnlockExperimentalVMOptions \
	-XX:+UseZGC \
	-XX:+UseNUMA \
	-Xlog:gc:stdout:time"

COPY ./ /what-to-watch/
WORKDIR /what-to-watch
RUN sh -c './gradlew clean build -Pversion=$W2W_VERSION -i --stacktrace'
RUN sh -c 'ls -al ./what-to-watch-boot/build/libs/'

EXPOSE 8080
EXPOSE 9999

CMD sh -c "java \
		-Djava.security.egd=file:/dev/./urandom \
		-Dw2w.titles.pagesPerRequest=$W2W_TITLES_PAGES_PER_REQUEST \
		-Dw2w.pool.size=$W2W_THREAD_POOL_SIZE \
		-Dw2w.suggestions.filter.scoreThreshold=$W22_FILM_SCORE_THRESHOLD \
		-Dredis.host=$REDIS_HOST \
		-Dlogging.level.root=$LOGGING_LEVEL\
		-Dlogging.level.pl.ciruk=$W2W_LOGGING_LEVEL\
		$JMX_OPTS \
		$JVM_OPTS \
		-jar ./what-to-watch-boot/build/libs/what-to-watch-boot-$W2W_VERSION.jar"
