# Base the container image off of JDK 8
FROM openjdk:8

# Set the working dir env var
ENV WORK_DIR /speak-for-the-trees-backend

# The port that YOU should expose when you run this in a container
# see https://docs.docker.com/engine/reference/builder/#expose
EXPOSE 8081

# Set default working directory
WORKDIR ${WORK_DIR}

# Run necessary tasks
RUN apt-get update -y
RUN apt-get upgrade -y
RUN apt-get install maven -y

# Add some files
ADD api ${WORK_DIR}/api
ADD common ${WORK_DIR}/common
ADD persist ${WORK_DIR}/persist
ADD service ${WORK_DIR}/service
ADD pom.xml ${WORK_DIR}/pom.xml
ADD scripts ${WORK_DIR}

RUN rm -rf ${WORK_DIR}/service/test
RUN rm -rf ${WORK_DIR}/service/src/test

# Set a default command to execute
CMD /speak-for-the-trees-backend/deploy.sh
