FROM registry.access.redhat.com/ubi8/openjdk-11:1.14-12

USER root
ENV JAR_FILE=target/stresstest-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} /home/jboss/app/app.jar
COPY src /home/jboss/app/src 
COPY pom.xml /home/jboss/app/pom.xml
COPY reports /home/jboss/app/reports
EXPOSE 8080
ENTRYPOINT ["java","-jar","/home/jboss/app/app.jar","-Djava.net.preferIPv4Stack=true -Dspring.cloud.kubernetes.enabled=false"]