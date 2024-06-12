FROM maven:3.8.4-openjdk-17 AS build

# копирование исходного кода в образ
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app

# Сборка приложения
RUN mvn -f /usr/src/app/pom.xml clean package

# Образ JDK для запуска приложения
FROM openjdk:17

# Установка рабочей директории в контейнере
WORKDIR /telegramBot

# Копирование собранного jar-файл из стадии сборки
COPY --from=build /usr/src/app/target/telegramBot-0.0.1-SNAPSHOT.jar /telegramBot/

# Порт, который будет прослушивать приложение
EXPOSE 8080

# Запуск приложения при старте контейнера
CMD ["java", "-jar", "telegramBot-0.0.1-SNAPSHOT.jar"]