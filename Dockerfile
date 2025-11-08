FROM maven:3.9.2-eclipse-temurin-17 as build
WORKDIR /app
COPY backend-java/pom.xml ./
COPY backend-java/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/sudoku-backend-1.0-SNAPSHOT.jar ./sudoku.jar
EXPOSE 3000
ENV PORT=3000
ENTRYPOINT ["java","-jar","/app/sudoku.jar"]
