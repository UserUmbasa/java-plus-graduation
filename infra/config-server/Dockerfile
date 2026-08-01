FROM eclipse-temurin:21-jre-jammy
VOLUME /tmp
ARG JAR_FILE=config-server/target/*.jar
COPY ${JAR_FILE} config-server.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /config-server.jar"]