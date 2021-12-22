FROM adoptopenjdk/openjdk8:latest

WORKDIR .
RUN mkdir -p /application
ADD /target/scala-2.13/Easymacher-assembly-0.1.jar /application/Easymacher.jar

CMD java -Dserver.port=$PORT $JAVA_OPTS -jar /application/Easymacher.jar