FROM openjdk:8-jdk-alpine
EXPOSE 9092
RUN apk add --no-cache curl && apk add --no-cache bash
RUN curl https://apache.mediamirrors.org/kafka/2.7.0/kafka_2.13-2.7.0.tgz --output kafka.tgz && tar -xzf kafka.tgz
WORKDIR kafka
RUN nohup bash -c "./bin/zookeeper-server-start.sh config/zookeeper.properties" && sleep 3
CMD ["./bin/kafka-server-start.sh", "config/server.properties"]