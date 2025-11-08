# Sudoku Take-Home (Java backend + frontend)

Java 17 backend (no frameworks) + vanilla JS frontend. Dockerized with multi-stage build.

## IntelliJ (Mac) quick start
1. Install Java 17 (Temurin): `brew install --cask temurin`
2. Install Maven: `brew install maven`
3. Open IntelliJ -> Open `backend-java` folder (it will detect Maven project)
4. Run tests or run `App.main` to start server

## Build & run locally
mvn package
java -jar target/sudoku-backend-1.0-SNAPSHOT.jar
open http://localhost:3000

## Docker
docker build -t sudoku-java:latest .
docker run -p 3000:3000 sudoku-java:latest

## Deploy to AWS EC2 (Ubuntu / Debian)
1. (Optional) Push Docker image to Docker Hub:
   docker build -t <user>/sudoku-java:latest .
   docker push <user>/sudoku-java:latest
2. Create EC2 instance (Ubuntu 22.04 or Debian 12), allow ports 22 and 3000.
3. SSH in, install docker: `sudo apt update && sudo apt install -y docker.io`
4. Pull/run image or git clone and build on server.
