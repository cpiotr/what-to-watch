FROM frolvlad/alpine-oraclejdk8:slim
MAINTAINER piotr@ciruk.pl
VOLUME /tmp
ENV W2W_VERSION 0.0.1-SNAPSHOT
ADD ./build/libs/what-to-watch-boot-$W2W_VERSION.jar w2w.jar
RUN sh -c 'touch /w2w.jar'
EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/w2w.jar"]
