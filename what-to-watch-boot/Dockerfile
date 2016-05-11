FROM frolvlad/alpine-oraclejdk8:slim
MAINTAINER piotr@ciruk.pl
VOLUME /tmp
ADD ./build/libs/what-to-watch-boot-0.0.1-SNAPSHOT.jar w2w.jar
RUN sh -c 'touch /w2w.jar'
EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/w2w.jar"]
