# He thong Quan ly Don hang - Nha May Nhua Viet Duc

## Gioi thieu
Ung dung quan ly don hang cho Nha may nhua Viet Duc, xay dung bang Java Spring Boot + MongoDB + Bootstrap 5.

## Cong nghe su dung
- **Backend**: Java 17, Spring Boot 3.2.0
- **Database**: MongoDB
- **Frontend**: HTML5, Bootstrap 5, Thymeleaf
- **Build Tool**: Maven

## Cau hinh

### 1. MongoDB
Dam bao MongoDB da duoc cai dat va chay tren may:
```bash
# Kiem tra MongoDB
mongod --version
```

### 2. Database Connection
Chinh sua `src/main/resources/application.properties`:
```properties
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=nhamay_nhua_vietduc
```

## Cai dat va Chay

### Buoc 1: Clone/Copy project
```bash
cd D:\LUMI-HOME\qldhjava
```

### Buoc 2: Build project
```bash
mvn clean install
```

### Buoc 3: Chay ung dung
```bash
mvn spring-boot:run
```

### Buoc 4: Truy cap ung dung
Mo trinh duyet va truy cap: **http://localhost:8080**

## Tinh nang chinh

### 1. Dashboard
- Tong quan he thong
- Thong ke don hang theo trang thai
- Hien thi don hang gan day

### 2. Quan ly Don hang
- **Tao don hang moi**: Nhap thong tin khach hang va san pham
- **Danh sach don hang**: Tim kiem, loc theo trang thai
- **Chinh sua don hang**: Cap nhat thong tin
- **Xoa don hang**: Xoa don hang
- **Xem chi tiet**: Xem day du thong tin don hang
- **Cap nhat trang thai**: Chuyen doi trang thai don hang

### 3. Trang thai Don hang
- Cho xac nhan (xam)
- Da xac nhan (xanh duong)
- Dang san xuat (cam)
- Hoan thanh (xanh la)
- Da huy (do)

### 4. Bao cao Thong ke
- Tong quan don hang
- Doanh thu tong
- Phan bo don hang theo trang thai
- Ty le hoan thanh

## Cau truc Project

```
qldhjava/
├── pom.xml
├── src/
│   └── main/
│       ├── java/com/vietduc/plastic/
│       │   ├── PlasticFactoryApplication.java
│       │   ├── model/
│       │   │   ├── Order.java
│       │   │   ├── OrderItem.java
│       │   │   └── OrderStatus.java
│       │   ├── repository/
│       │   │   └── OrderRepository.java
│       │   ├── service/
│       │   │   └── OrderService.java
│       │   └── controller/
│       │       └── OrderController.java
│       └── resources/
│           ├── application.properties
│           └── templates/
│               ├── layout.html
│               ├── dashboard.html
│               ├── orders/
│               │   ├── list.html
│               │   ├── form.html
│               │   └── detail.html
│               └── reports/
│                   └── index.html
```

## API Endpoints

| Method | Endpoint | Mo ta |
|--------|----------|-------|
| GET | `/` | Dashboard |
| GET | `/orders` | Danh sach don hang |
| GET | `/orders/new` | Form tao don hang moi |
| POST | `/orders/save` | Luu don hang moi |
| GET | `/orders/edit/{id}` | Form chinh sua |
| POST | `/orders/update/{id}` | Cap nhat don hang |
| GET | `/orders/delete/{id}` | Xoa don hang |
| GET | `/orders/detail/{id}` | Chi tiet don hang |
| POST | `/orders/{id}/status` | Cap nhat trang thai |
| GET | `/reports` | Trang bao cao |

## Ma don hang tu dong
Dinh dang: `VD-YYYYMMDD-NNN`
- VD: Viet Duc
- YYYYMMDD: Ngay tao
- NNN: So thu tu trong ngay (001, 002, ...)

Vi du: `VD-20261003-001`

## Huong dan su dung

### Tao don hang moi
1. Tu Dashboard, click "Tao Don hang moi"
2. Nhap thong tin khach hang (bat buoc: Ho ten, So dien thoai)
3. Them san pham (ten, ma, so luong, don gia)
4. Chon trang thai va nhap ghi chu (neu co)
5. Click "Tao Don hang"

### Cap nhat trang thai
1. Tu trang Chi tiet don hang
2. Chon trang thai moi tu cac nut badge
3. He thong tu dong cap nhat

### Tim kiem don hang
1. Tu trang Danh sach don hang
2. Nhap tu khoa tim kiem (ten hoac so dien thoai)
3. Hoac loc theo trang thai cu the

## Lien he
Nha May Nhua Viet Duc
- Hotline: 0123.456.789
- Email: info@vietducplastic.vn
