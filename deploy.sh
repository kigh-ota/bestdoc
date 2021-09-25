(
    cd frontend
    yarn build
)
(
    cd backend
    ./gradlew clean bootJar
    heroku deploy:jar build/libs/bestdoc-1.0-SNAPSHOT.jar
)
