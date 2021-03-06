FROM adoptopenjdk/openjdk8:latest

WORKDIR .
RUN mkdir -p /application
COPY /target/scala-2.13/Easymacher-assembly-0.1.jar /application/Easymacher.jar
ENV DATABASE_URL=$DATABASE_URL

CMD java -Dserver.port=$PORT $JAVA_OPTS -jar /application/Easymacher.jar