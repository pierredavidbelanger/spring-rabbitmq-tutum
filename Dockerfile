FROM dockerfile/java:oracle-java8

RUN mkdir -p /usr/local/rabbitmq-client

ADD target/rabbitmq-client-*.jar /usr/local/rabbitmq-client/rabbitmq-client.jar

ENV JAVA_OPTS="-Xss512k -XX:+UseCompressedOops -Djava.security.egd=file:/dev/./urandom -Djava.rmi.server.useCodebaseOnly=true -Dfile.encoding=UTF-8 -Duser.timezone=GMT -Duser.language=en" \
    SPRING_RABBITMQ_ADDRESSES="amqp://rabbitmq:5672" \
    JAVA_XMX="-Xmx256m"

EXPOSE 8080

WORKDIR /usr/local/rabbitmq-client

CMD java $JAVA_OPTS $JAVA_XMX -jar rabbitmq-client.jar
