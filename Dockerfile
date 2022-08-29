FROM openjdk:jre-alpine

WORKDIR /knowledge-portal-rest

CMD apt-get install -y tzdata

RUN echo "Asia/Kolkata" > /etc/timezone
CMD dpkg-reconfigure -f noninteractive tzdata

COPY ./target/scala-2.13/knolx-backend.jar .

CMD ["java","-jar","knolx-backend.jar"]