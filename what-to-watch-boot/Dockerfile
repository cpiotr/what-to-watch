FROM frolvlad/alpine-oraclejdk8:slim
MAINTAINER c.piotre@gmail.com

VOLUME /tmp

ENV W2W_VERSION 0.0.1-SNAPSHOT
ENV W2W_TITLES_CRAWLED_PAGES_LIMIT 10
ENV W2W_THREAD_POOL_SIZE 16
ENV REDIS_HOST 172.17.0.2

ENV LOGGING_LEVEL=INFO
ENV W2W_LOGGING_LEVEL=INFO

ENV JVM_OPTS="-XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=50 -XX:G1MaxNewSizePercent=75 -Xmx1G -Xms1G"

COPY ./build/libs/what-to-watch-boot-$W2W_VERSION.jar w2w.jar
RUN sh -c 'touch /w2w.jar'

EXPOSE 8080
EXPOSE 9999

CMD sh -c "java \
		-Djava.security.egd=file:/dev/./urandom \
		-Dw2w.titles.crawled.pages.limit=$W2W_TITLES_CRAWLED_PAGES_LIMIT \
		-Dw2w.pool.size=$W2W_THREAD_POOL_SIZE \
		-Dredis.host=$REDIS_HOST \
		-Dlogging.level.root=$LOGGING_LEVEL\
		-Dlogging.level.pl.ciruk=$W2W_LOGGING_LEVEL\
		-Dcom.sun.management.jmxremote \
		-Dcom.sun.management.jmxremote.local.only=false \
		-Dcom.sun.management.jmxremote.authenticate=false \
		-Dcom.sun.management.jmxremote.port=9999 \
		-Dcom.sun.management.jmxremote.ssl=false \
		$JVM_OPTS \
		-jar /w2w.jar"
