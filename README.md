# Hệ Thống Quản Lý Sinh Hoạt Lớp QNU

Hệ thống hỗ trợ quản lý sinh hoạt lớp cho Trường Đại học Quy Nhơn, gồm backend Spring Boot và frontend Angular.

README này dành cho:
- Thành viên kỹ thuật cần chạy dự án nhanh ở local.
- Người chuẩn bị demo cần nắm rõ tài khoản, luồng nghiệp vụ và tính năng nổi bật.
- Người chuẩn bị triển khai cần danh sách cấu hình và lưu ý bảo mật.

## 1. Tổng quan dự án

Các nhóm chức năng chính:
- Quản trị dữ liệu nền: khoa, khóa học, lớp, sinh viên, giảng viên, cán bộ, cố vấn.
- Quản lý vai trò và phân quyền nhiều cấp.
- Quản lý phiên sinh hoạt lớp, tài liệu hướng dẫn PDF, biên bản.
- Điểm danh bằng mã cho sinh viên.
- Thông báo thời gian thực qua WebSocket.
- Đa ngôn ngữ: Việt, Anh, Trung, Lào.
- Giao diện sáng/tối (dark mode).
- Bảo mật đăng nhập nâng cao: rate limit + khóa tạm thời.

## 2. Điểm nổi bật

- Phân quyền 5 cấp: SuperAdmin, DepartmentAdmin, Academic Advisor, StudentAdmin, Student.
- Luồng nghiệp vụ khép kín từ tạo phiên sinh hoạt đến điểm danh và ghi biên bản.
- Trang thông báo đầy đủ: đọc từng thông báo, đọc tất cả, lọc All/Unread, phân trang.
- Đa ngôn ngữ đã đồng bộ cho các khu vực chính (login, header, sidebar).
- Nhận diện thương hiệu QNU đã được cập nhật bằng logo chính thức trên login và header.

## 3. Kiến trúc và công nghệ

- Backend:
  - Spring Boot 3.3.x
  - Java 21
  - Spring Security JWT
  - Spring Data JPA
  - Spring WebSocket
  - Flyway
  - Redis
- Frontend:
  - Angular 18
  - PrimeNG
  - ngx-translate
  - Bootstrap
- Cơ sở dữ liệu:
  - MySQL 8+

## 4. Cấu trúc repository

```text
.
|-- back_end_class_activity/      # Backend Spring Boot
|-- qnu_fe/                       # Frontend Angular
|-- demo_guide.md                 # Kịch bản demo tổng thể (20-30 phút)
|-- DEMO_5_ACCOUNTS_GUIDE.md      # Kịch bản demo theo 5 tài khoản cố định
|-- LOGIN_RATELIMIT_FEATURE.md    # Tài liệu bảo mật đăng nhập
|-- ACCOUNT_MANAGEMENT_FEATURE.md # Tài liệu chức năng quản lý tài khoản
```

## 5. Yêu cầu môi trường

- Java 21
- Maven 3.9+
- Node.js 18+
- npm 9+
- MySQL 8+
- Redis 6+ (khuyến nghị để đầy đủ tính năng)

## 6. Cấu hình nhanh trước khi chạy

### 6.1 Backend

File cấu hình chính:
- `back_end_class_activity/src/main/resources/application.yaml`
- `back_end_class_activity/src/main/resources/application-dev.yml`

Thông số mặc định đang dùng:
- Backend port: `8000`
- Context path: `/AMQNU/api/`
- Database URL: `jdbc:mysql://localhost:3306/qnu`
- Redis host/port: `localhost:6379`

Khuyến nghị:
- Dùng biến môi trường để ghi đè cấu hình nhạy cảm trước khi deploy.
- Không để secret thật trong file cấu hình production.

### 6.2 Frontend

File cấu hình môi trường:
- `qnu_fe/src/environments/environment.ts`

Thông số mặc định:
- Frontend URL: `http://localhost:4200`
- API URL: `http://localhost:8000/AMQNU/api/`

## 7. Hướng dẫn chạy local

### 7.1 Chạy backend

```bash
cd back_end_class_activity
./mvnw spring-boot:run
```

Windows:

```bat
cd back_end_class_activity
mvnw.cmd spring-boot:run
```

Sau khi chạy thành công:
- API base: `http://localhost:8000/AMQNU/api`
- Swagger: `http://localhost:8000/AMQNU/api/document/swagger-ui.html`

### 7.2 Chạy frontend

```bash
cd qnu_fe
npm install
npm start
```

Truy cập:
- `http://localhost:4200`

## 8. Tài khoản demo mặc định

| Vai trò | Username | Password |
|---|---|---|
| SuperAdmin | `admin` | `admin123` |
| DepartmentAdmin | `vietxuan@gmail.com` | `vietxuan@gmail.com` |
| Academic Advisor | `xuanquynh@gmail.com` | `xuanquynh@gmail.com` |
| StudentAdmin (Lớp trưởng) | `4451190072` | `4451190072` |
| Student | `4451190073` | `4451190073` |

## 9. Tài liệu demo và nghiệp vụ

- Kịch bản demo tổng: `demo_guide.md`
- Kịch bản demo 5 tài khoản: `DEMO_5_ACCOUNTS_GUIDE.md`
- Tài liệu bảo mật đăng nhập: `LOGIN_RATELIMIT_FEATURE.md`
- Tài liệu quản lý tài khoản: `ACCOUNT_MANAGEMENT_FEATURE.md`

## 10. Checklist trước khi demo

- Backend chạy tại port `8000`.
- Frontend chạy tại port `4200`.
- Đăng nhập lại được đủ 5 tài khoản demo.
- Thông báo có dữ liệu để demo popup và trang Notifications.
- Chuyển ngôn ngữ VI/EN/CN/LAO không còn text hard-code.
- Logo QNU hiển thị đúng ở login và header.
- Nếu demo khóa đăng nhập: chuẩn bị thao tác nhập sai nhiều lần và reset lock dev.

## 11. Lưu ý bảo mật khi đưa lên môi trường thật

- Không dùng cấu hình mặc định local cho production.
- Chuyển toàn bộ thông tin nhạy cảm sang biến môi trường:
  - DB username/password
  - JWT signer key
  - SMTP username/password
  - Cloudinary key/secret
- Giới hạn CORS theo domain thật.
- Bật logging mức phù hợp, tránh lộ dữ liệu nhạy cảm trong log.

## 12. Lỗi thường gặp

- Lỗi không gọi được API từ frontend:
  - Kiểm tra backend đã chạy port `8000`.
  - Kiểm tra `environment.ts` trỏ đúng API URL.
- Lỗi đăng nhập thất bại liên tiếp:
  - Có thể bị khóa tạm thời do cơ chế bảo mật.
  - Chờ hết thời gian khóa hoặc dùng reset lock ở môi trường dev.
- Lỗi không nhận thông báo realtime:
  - Kiểm tra backend WebSocket.
  - Kiểm tra token đăng nhập còn hiệu lực.

## 13. Đóng góp

- Tạo branch riêng cho từng tính năng hoặc bugfix.
- Commit theo nhóm thay đổi rõ ràng.
- Mở Pull Request kèm mô tả thay đổi và cách kiểm thử.
