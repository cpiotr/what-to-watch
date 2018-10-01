FROM openjdk:11-slim
MAINTAINER c.piotre@gmail.com

VOLUME /tmp

ARG W2W_VERSION
ENV W2W_VERSION=${W2W_VERSION}
ENV W2W_TITLES_PAGES_PER_REQUEST 10
ENV W2W_THREAD_POOL_SIZE 16
ENV REDIS_HOST 172.17.0.2

ENV LOGGING_LEVEL=INFO
ENV W2W_LOGGING_LEVEL=INFO

ENV JMX_OPTS="-Dcom.sun.management.jmxremote \
	-Dcom.sun.management.jmxremote.local.only=false \
	-Dcom.sun.management.jmxremote.authenticate=false \
	-Dcom.sun.management.jmxremote.port=9999 \
	-Dcom.sun.management.jmxremote.ssl=false"

ENV JVM_OPTS="-Xmx1G -Xms1G \
	-Xss2M \
	-XX:+TieredCompilation \
	-XX:InitialCodeCacheSize=128m \
	-XX:ReservedCodeCacheSize=128m \
	-XX:-UseBiasedLocking \
	-XX:+AggressiveOpts \
	-XX:+PreserveFramePointer \
	-XX:+IgnoreUnrecognizedVMOptions \
	-XX:+UnlockExperimentalVMOptions \
	-XX:+UseZGC"

COPY ./ /what-to-watch/
WORKDIR /what-to-watch
RUN sh -c './gradlew clean build -Pversion=$W2W_VERSION'
RUN sh -c 'ls -al ./what-to-watch-boot/build/libs/'

EXPOSE 8080
EXPOSE 9999

CMD sh -c "java \
		-Djava.security.egd=file:/dev/./urandom \
		-Dw2w.titles.pagesPerRequest=$W2W_TITLES_PAGES_PER_REQUEST \
		-Dw2w.pool.size=$W2W_THREAD_POOL_SIZE \
		-Dredis.host=$REDIS_HOST \
		-Dlogging.level.root=$LOGGING_LEVEL\
		-Dlogging.level.pl.ciruk=$W2W_LOGGING_LEVEL\
		$JMX_OPTS \
		$JVM_OPTS \
		-jar ./what-to-watch-boot/build/libs/what-to-watch-boot-$W2W_VERSION.jar"
