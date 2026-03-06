# ==============================
# 1️⃣ BUILD STAGE
# ==============================
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Copy Gradle files trước để cache dependency
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Download dependencies (layer cache)
RUN ./gradlew dependencies --no-daemon || true

# Copy source
COPY src ./src

# Build jar
RUN ./gradlew clean bootJar --no-daemon

# ==============================
# 2️⃣ RUNTIME STAGE
# ==============================
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Khởi tạo múi giờ VN
ENV TZ="Asia/Ho_Chi_Minh"

# Tạo user không có quyền root để bảo mật
RUN groupadd -r spring && useradd -r -g spring springuser

# Copy jar từ build stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Copy secret config
COPY secret.properties ./secret.properties

# Cấp quyền sở hữu file jar cho user vừa tạo
RUN chown springuser:spring app.jar

# Chuyển sang dùng user này từ đây trở đi
USER springuser

# Expose port Gateway (mặc định 8080)
EXPOSE 8080

# JVM tuning cho container
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]