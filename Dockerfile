# following https://spring.io/blog/2020/01/27/creating-docker-images-with-spring-boot-2-3-0-m1
FROM adoptopenjdk:11.0.9.1_1-jdk-hotspot as builder
COPY ./ ./
RUN ./mvnw clean package

FROM adoptopenjdk:11.0.9.1_1-jre-hotspot
COPY --from=builder target/order-simulation-0.0.1-SNAPSHOT.jar ./ordersim.jar


ENTRYPOINT ["java", "-jar", "./ordersim.jar"]
