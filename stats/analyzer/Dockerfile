FROM eclipse-temurin:21-jre-jammy
VOLUME /tmp
ARG JAR_FILE=analyzer/target/*.jar
COPY ${JAR_FILE} analyzer.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /analyzer.jar"]