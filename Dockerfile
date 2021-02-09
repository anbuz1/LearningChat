FROM openjdk:8-jdk-alpine
RUN mkdir -p /usr/src/app/
WORKDIR /usr/src/app/

copy . /usr/src/app/

RUN javac src/*.java

WORKDIR /usr/src/app/src/

RUN jar cvfe LearningChat.jar Main *.class

CMD java -jar LearningChat.jar