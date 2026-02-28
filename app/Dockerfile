FROM amazoncorretto:21

LABEL maintainer="xyzsince2014 <xyzsince2014@gmail.com>"

WORKDIR /app

# Copy the fat JAR produced by the shadowJar task (includes all dependencies)
COPY build/libs/*-all.jar app.jar

EXPOSE 8081

# PORT is supplied at runtime via --env-file; no hardcoded default here
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
