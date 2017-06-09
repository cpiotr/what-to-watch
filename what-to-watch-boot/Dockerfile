FROM frolvlad/alpine-oraclejdk8:slim
MAINTAINER c.piotre@gmail.com

VOLUME /tmp

ENV W2W_VERSION 0.0.1-SNAPSHOT
ENV W2W_TITLES_CRAWLED_PAGES_LIMIT 10
ENV W2W_THREAD_POOL_SIZE 16
ENV REDIS_HOST 172.17.0.2

COPY ./build/libs/what-to-watch-boot-$W2W_VERSION.jar w2w.jar
RUN sh -c 'touch /w2w.jar'

EXPOSE 8080

CMD sh -c "java \
		-Djava.security.egd=file:/dev/./urandom \
		-Dw2w.titles.crawled.pages.limit=$W2W_TITLES_CRAWLED_PAGES_LIMIT \
		-Dw2w.pool.size=$W2W_THREAD_POOL_SIZE \
		-Dredis.host=$REDIS_HOST \
		-jar /w2w.jar"
