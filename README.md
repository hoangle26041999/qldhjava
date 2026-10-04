# Nha May Nhua Viet Duc - Plastic Factory Manager

Hệ thống quản lý đơn hàng cho nhà máy nhựa Việt Đức. Spring Boot 3.4 + Java 21 + MongoDB.

## Tech stack
- Java 21, Spring Boot 3.4.0
- Spring Data MongoDB
- Thymeleaf + Thymeleaf Layout Dialect
- Maven build

## Chạy local (development)
Yêu cầu: JDK 21 + Maven 3.9+ + MongoDB local (hoặc Atlas)

```bash
# Cài Maven wrapper nếu chưa có
mvn -v

# Chạy dev (profile mặc định, kết nối MongoDB local)
mvn spring-boot:run

# Hoặc chạy từ IntelliJ IDEA: nút ▶ trên class PlasticFactoryManagerApplication
```

App mở ở: http://localhost:8080

## Build JAR (production-style)
```bash
mvn clean package -DskipTests
java -jar target/plastic-factory-manager-1.0.0.jar --spring.profiles.active=prod
```

## Deploy lên web public
Có 2 cách dễ nhất, miễn phí:

### Cách 1: Railway.app (khuyên dùng - $5 credit/tháng free)
1. Vào https://railway.app
2. Sign in bằng GitHub
3. New Project → Deploy from GitHub repo → chọn `qldhjava`
4. Railway tự detect Java + Maven → build
5. Vào tab **Variables**, thêm:
   - `MONGODB_URI_PROD` = connection string Atlas của bạn
   - `SPRING_PROFILES_ACTIVE` = `prod`
6. Tab **Settings** → **Networking** → **Generate Domain** → copy URL
   - Ví dụ: `qldhjava-production.up.railway.app`

### Cách 2: Render.com (free tier 750h/tháng)
1. Vào https://render.com → Sign in bằng GitHub
2. New → Web Service → chọn repo `qldhjava`
3. Runtime: Java
4. Build command: `mvn clean package -DskipTests`
5. Start command: `java -jar target/plastic-factory-manager-1.0.0.jar --spring.profiles.active=prod`
6. Add env: `MONGODB_URI_PROD` + `SPRING_PROFILES_ACTIVE=prod`
7. Click **Create Web Service** → đợi build → có URL `https://qldhjava.onrender.com`

## CI/CD
- Mỗi lần `git push origin main`, GitHub Actions tự chạy `Java CI with Maven` để build + test.
- Xem tab **Actions** trên GitHub.
