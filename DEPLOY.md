# Hướng dẫn Deploy - Spring Boot lên VPS Linux

## 1. Yêu cầu server

| Mục | Tối thiểu | Khuyến nghị |
|-----|-----------|-------------|
| CPU | 1 core | 1-2 core |
| RAM | 512 MB | 1 GB |
| Disk | 1 GB | 5 GB |
| OS | Ubuntu 22.04 LTS | Ubuntu 22.04/24.04 LTS |
| Java | JDK 21 | Temurin 21 LTS |

## 2. Cài JDK 21 trên Ubuntu

```bash
sudo apt update
sudo apt install -y openjdk-21-jre-headless
java -version
```

Không cần JDK, chỉ cần JRE để chạy JAR.

## 3. Build JAR trên máy local

```bash
mvnw.cmd clean package -DskipTests
```

File JAR nằm ở: `target/plastic-factory-manager-1.0.0.jar`

Upload lên server:

```bash
scp target/plastic-factory-manager-1.0.0.jar user@server:/home/appuser/app/
```

## 4. Chuẩn bị thư mục trên server

```bash
sudo mkdir -p /home/appuser/app/logs /home/appuser/app/uploads
sudo chown -R appuser:appuser /home/appuser/app
```

## 5. Tạo file systemd `/etc/systemd/system/nhamay.service`

```ini
[Unit]
Description=Nha May Nhua Viet Duc - Spring Boot
After=network.target

[Service]
Type=simple
User=appuser
WorkingDirectory=/home/appuser/app
Environment="JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64"
Environment="MONGODB_URI_PROD=mongodb+srv://REAL_USER:REAL_PASS@cluster0.bvuezfj.mongodb.net/nhamay_nhua_vietduc?retryWrites=true&w=majority"
Environment="SERVER_PORT=8080"
Environment="LOG_DIR=/home/appuser/app/logs"

ExecStart=/usr/bin/java \
    -Xms200m \
    -Xmx512m \
    -XX:+UseG1GC \
    -XX:MaxRAMPercentage=70 \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -jar /home/appuser/app/plastic-factory-manager-1.0.0.jar \
    --spring.profiles.active=prod

SuccessExitStatus=143
Restart=always
RestartSec=5
StandardOutput=append:/home/appuser/app/logs/stdout.log
StandardError=append:/home/appuser/app/logs/stderr.log

# Bảo vệ tài nguyên
LimitNOFILE=65536
MemoryMax=768M

[Install]
WantedBy=multi-user.target
```

## 6. Kích hoạt & chạy

```bash
sudo cp nhamay.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable nhamay
sudo systemctl start nhamay
sudo systemctl status nhamay
```

## 7. Cài Nginx reverse proxy + HTTPS

```bash
sudo apt install -y nginx certbot python3-certbot-nginx
```

Tạo `/etc/nginx/sites-available/nhamay`:

```nginx
server {
    listen 80;
    server_name yourdomain.com;

    client_max_body_size 50M;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/nhamay /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
sudo certbot --nginx -d yourdomain.com
```

## 8. RAM thực tế sau khi tối ưu

| Cấu hình | RAM ước tính |
|----------|-------------|
| Spring Boot + dev profile | 350-500 MB |
| Spring Boot + prod profile | **180-280 MB** |
| Sau -XX:+UseG1GC + cache=true | 150-200 MB |

Không thua kém Node.js nhiều (~80-150MB), nhưng vẫn giữ được 100% code logic & UI hiện tại.

## 9. Lệnh thường dùng

```bash
# Xem log real-time
sudo journalctl -u nhamay -f

# Hoặc xem log file
tail -f /home/appuser/app/logs/nhamay-prod.log

# Restart
sudo systemctl restart nhamay

# Dừng
sudo systemctl stop nhamay

# Xem trạng thái
sudo systemctl status nhamay

# Xem RAM thực tế
ps aux | grep plastic-factory
```

## 10. Update phiên bản mới

```bash
# Trên máy local
mvnw.cmd clean package -DskipTests

# Upload (giữ folder logs/uploads trên server)
scp target/plastic-factory-manager-1.0.0.jar user@server:/home/appuser/app/

# Trên server
sudo systemctl restart nhamay
```

Hoặc viết script `deploy.sh`:

```bash
#!/bin/bash
set -e
echo "[1/3] Upload JAR..."
scp target/plastic-factory-manager-1.0.0.jar user@server:/home/appuser/app/
echo "[2/3] Restart service..."
ssh user@server "sudo systemctl restart nhamay"
echo "[3/3] Check status..."
sleep 3
ssh user@server "sudo systemctl status nhamay --no-pager"
echo "Deploy done!"
```