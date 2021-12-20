FROM openjdk:8
ENV SBT_VERSION 1.5.5
RUN curl -L -o sbt-$SBT_VERSION.zip https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.zip
RUN unzip sbt-$SBT_VERSION.zip -d ops
WORKDIR /easymacher
ADD . /easymacher
CMD /ops/sbt/bin/sbt run