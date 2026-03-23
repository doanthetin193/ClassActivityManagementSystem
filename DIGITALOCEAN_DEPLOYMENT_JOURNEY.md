# Hành Trình Triển Khai Hệ Thống Lên DigitalOcean

## 1. Mục tiêu tài liệu
Tài liệu này ghi lại chi tiết toàn bộ quá trình triển khai dự án Class Activity Management System lên DigitalOcean App Platform theo đúng những gì đã thực hiện thực tế, bao gồm:
- Các quyết định hạ tầng.
- Các bước cấu hình backend, frontend, database.
- Các lỗi phát sinh trong từng giai đoạn và cách xử lý cụ thể.
- Cấu hình cuối cùng đã chạy thành công.

Phạm vi triển khai:
- Backend: Spring Boot (Web Service).
- Frontend: Angular (Static Site).
- Database: DigitalOcean Managed MySQL.
- Kết nối tổng thể: frontend -> backend -> database.

---

## 2. Vì sao chọn DigitalOcean

### 2.1 Lý do chính
- Tận dụng ưu đãi dành cho sinh viên: credit miễn phí 200 USD giúp giảm đáng kể chi phí thử nghiệm và triển khai.
- Nền tảng App Platform cho phép triển khai nhanh trực tiếp từ GitHub, không cần tự dựng server thủ công.
- Hỗ trợ tốt mô hình monorepo: backend và frontend cùng nằm trong một repository nhưng có thể tách Source Directory theo từng component.
- Có sẵn Managed Database (MySQL), thuận tiện cho vận hành, backup và phân tách rõ vai trò ứng dụng với dữ liệu.

### 2.2 Điều kiện cần để kích hoạt
- Cần email sinh viên hợp lệ để nhận ưu đãi.
- Cần thẻ thanh toán quốc tế (Visa/MasterCard) để xác minh thanh toán và kích hoạt tài khoản.

---

## 3. Kiến trúc triển khai cuối cùng

Thông tin chính:
- App name: clownfish-app.
- Region: SGP1.
- Domain: https://clownfish-app-nfk4m.ondigitalocean.app.

Thành phần trong app:
- classactivitymanagementsystem-ba: Web Service (backend).
- classactivitymanagementsystem-qn: Static Site (frontend).

Database quản lý:
- Host: qnu-mysql-sgp1-do-user-34642533-0.g.db.ondigitalocean.com.
- Port: 25060.

Quy tắc định tuyến đã chốt:
- /AMQNU/api chuyển vào backend.
- / chuyển vào frontend.

Ý nghĩa của thiết kế này:
- API và UI tách riêng rõ ràng, tránh xung đột route.
- Frontend gọi API theo cùng domain App Platform, giảm rắc rối CORS so với tách domain phức tạp.

---

## 4. Chuẩn bị trước khi triển khai

### 4.1 Cấu trúc monorepo
Repository GitHub: doanthetin193/ClassActivityManagementSystem.

Hai thư mục nguồn quan trọng:
- back_end_class_activity: mã nguồn backend.
- qnu_fe: mã nguồn frontend.

### 4.2 Điều kiện kỹ thuật backend
- Công nghệ: Spring Boot 3.x.
- Java runtime: Java 21.
- Context path API: /AMQNU/api.
- Public HTTP port: 8000.

### 4.3 Điều kiện kỹ thuật frontend
- Công nghệ: Angular 18.
- Build production output: dist/qnu/browser.

### 4.4 Mốc kiểm tra trước khi bấm deploy
- Đảm bảo repository đã push đầy đủ.
- Đảm bảo không thiếu file cấu hình môi trường.
- Đảm bảo package-lock.json tồn tại và được theo dõi trong git để npm ci ổn định.

---

## 5. Triển khai backend lên App Platform

### 5.1 Tạo component backend
Thiết lập component:
- Loại: Web Service.
- Source Directory: back_end_class_activity.
- Public HTTP Port: 8000.
- Auto Deploy: bật.

### 5.2 Cấu hình biến môi trường backend
Đã cấu hình đầy đủ nhóm biến cần thiết (tổng khoảng 25 biến), bao gồm:
- Cấu hình profile chạy: SPRING_PROFILES_ACTIVE.
- Bảo mật JWT: JWT_SIGNER_KEY.
- Email service: MAIL_PASSWORD.
- Cloudinary: CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET.
- Kết nối database: DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD.
- Chống spam đăng nhập: RATE_LIMIT_ENABLED.

### 5.3 Cấu hình readiness check cho backend
Thiết lập chuẩn để App Platform chỉ route traffic khi backend thật sự sẵn sàng:
- Readiness type: HTTP.
- Path: /AMQNU/api/document/api-docs.
- Tần suất kiểm tra: mỗi 10 giây.

Nếu không cấu hình readiness path phù hợp, service có thể bị đánh dấu chưa sẵn sàng, gây fail trong pha deploy hoặc mất ổn định route.

---

## 6. Triển khai Managed MySQL và nhập dữ liệu

### 6.1 Tạo database trên DigitalOcean
- Tạo MySQL managed cluster trong cùng region SGP1.
- Nhận thông tin kết nối host, port, username, password từ DigitalOcean.

### 6.2 Kết nối từ máy local bằng MySQL Workbench
Vấn đề gặp phải lúc đầu:
- Kết nối bằng port mặc định 3306 nên thất bại.
- Chưa thêm IP local vào Trusted Sources.

Cách xử lý:
- Đổi đúng port sang 25060.
- Thêm Trusted Sources:
  - IP máy local (ví dụ 42.119.99.167).
  - Nguồn từ App Platform component backend.
- Thiết lập SSL mode đúng theo yêu cầu cụm managed DB.

### 6.3 Import dữ liệu thực tế
- Import SQL dump từ môi trường local lên database cloud.
- Kết quả import thành công với 26 bảng và dữ liệu mẫu phục vụ demo đăng nhập, phân quyền, danh mục.

---

## 7. Triển khai frontend lên App Platform

### 7.1 Tạo component frontend
Thiết lập component:
- Loại: Static Site.
- Source Directory: qnu_fe.
- Auto Deploy: bật.

### 7.2 Cấu hình build frontend
Thông số đã dùng và chạy được:
- Build Command: npm ci --include=dev && npm run build -- --configuration production.
- Output Directory: dist/qnu/browser.
- Biến môi trường buildpack: BP_NODE_VERSION=20.*.

Giải thích quan trọng:
- Dùng npm ci --include=dev vì Angular CLI nằm trong devDependencies; nếu bỏ dev dependencies thì build sẽ fail ngay trên cloud.

### 7.3 Cấu hình fallback cho SPA
Trong Static Site, cần cấu hình catchall để route client-side Angular hoạt động khi refresh trực tiếp:
- Catchall document: index.html.

Lưu ý đã từng gặp lỗi nhập sai:
- Nhập /index.html gây lỗi validate vì DigitalOcean yêu cầu filename hợp lệ, không kèm dấu gạch chéo đầu.

---

## 8. Dòng thời gian sự cố và cách khắc phục

### Giai đoạn 1: Build frontend fail do budget Angular
Triệu chứng:
- Build production báo bundle vượt budget (main bundle lớn hơn ngưỡng mặc định).

Xử lý:
- Cập nhật angular.json tăng ngưỡng budget phù hợp kích thước thực tế:
  - initial: warning 2MB, error 2.5MB.
  - anyComponentStyle: warning 6kB, error 8kB.

Kết quả:
- Build local qua, không còn fail vì budget.

---

### Giai đoạn 2: Xung đột route backend và frontend
Triệu chứng:
- Route root bị trùng, frontend không nhận đúng request.

Xử lý:
- Chốt lại route rõ ràng:
  - Backend phục vụ dưới /AMQNU/api.
  - Frontend phục vụ dưới /.

Kết quả:
- Luồng API và UI tách riêng, tránh đè route nhau.

---

### Giai đoạn 3: Deploy vẫn fail dù đã sửa routing
Triệu chứng:
- Build/deploy có lần thất bại trong pha kiểm tra service.

Xử lý:
- Thêm readiness check HTTP cho backend vào /AMQNU/api/document/api-docs.
- Thiết lập catchall cho frontend là index.html.

Kết quả:
- Hạ tầng định tuyến và kiểm tra sức khỏe service ổn định hơn.

---

### Giai đoạn 4: Lỗi thiếu file environment.prod.ts
Triệu chứng:
- Build frontend báo không tìm thấy đường dẫn environment.prod.ts trong file replacements.

Nguyên nhân gốc:
- File môi trường production chưa được theo dõi đúng cách trong git do cấu hình gitignore.

Xử lý:
- Tạo/cập nhật src/environments/environment.prod.ts với URL production.
- Sửa gitignore để file cần thiết không bị bỏ qua.
- Commit/push lại và trigger deploy mới.

---

### Giai đoạn 5: Lỗi tiếp theo thiếu environment.ts
Triệu chứng:
- Build báo không resolve được ../../environments/environment ở nhiều service.

Nguyên nhân gốc:
- Angular production vẫn cần file gốc environment.ts để cơ chế file replacement hoạt động.
- File này bị thiếu trong repository do rule ignore trước đó.

Xử lý:
- Bổ sung và track src/environments/environment.ts.
- Chuẩn hóa lại gitignore để không làm mất các file cấu hình môi trường cốt lõi.

Kết quả:
- Frontend build thành công trên DigitalOcean.

---

### Giai đoạn 6: Nhầm lẫn log cũ và log mới trên DigitalOcean
Vấn đề:
- Giao diện Activity dễ khiến hiểu nhầm đang xem log mới nhất, trong khi thực tế có thể là deployment cũ.

Cách kiểm tra đúng:
- Đối chiếu commit hash trong phần Summary.
- Đối chiếu timestamp bắt đầu build.
- Nếu cần, dùng Force rebuild and deploy để bắt buộc lấy source mới nhất.

---

## 9. Cấu hình file môi trường đã chốt

### 9.1 Môi trường development
File: src/environments/environment.ts.

Thông tin chính:
- production = false.
- apiURL = http://localhost:8000/AMQNU/api/.
- socketURL = ws://localhost:8000/AMQNU/api/ws/notifications.

### 9.2 Môi trường production
File: src/environments/environment.prod.ts.

Thông tin chính:
- production = true.
- apiURL = https://clownfish-app-nfk4m.ondigitalocean.app/AMQNU/api/.
- socketURL = wss://clownfish-app-nfk4m.ondigitalocean.app/AMQNU/api/ws/notifications.

---

## 10. Runbook triển khai chuẩn cho các lần sau

### Bước 1: Chuẩn bị source code
1. Đảm bảo nhánh main chứa đủ code backend và frontend.
2. Kiểm tra file môi trường cần thiết đã được theo dõi trong git.
3. Push commit mới lên GitHub.

### Bước 2: Kiểm tra cấu hình backend component
1. Source Directory đúng là back_end_class_activity.
2. Port public là 8000.
3. Biến môi trường đầy đủ.
4. Readiness check HTTP trỏ đúng /AMQNU/api/document/api-docs.

### Bước 3: Kiểm tra cấu hình frontend component
1. Source Directory đúng là qnu_fe.
2. Build Command đúng: npm ci --include=dev && npm run build -- --configuration production.
3. Output Directory đúng: dist/qnu/browser.
4. Catchall document là index.html.

### Bước 4: Kiểm tra networking toàn app
1. Route /AMQNU/api về backend.
2. Route / về frontend.

### Bước 5: Thực hiện deploy
1. Chờ auto deploy chạy theo commit.
2. Nếu cần, bấm Force rebuild and deploy.
3. Chỉ dùng Clear build cache khi nghi ngờ cache cũ gây lỗi.

### Bước 6: Xác nhận sau deploy
1. Đăng nhập bằng tài khoản demo.
2. Kiểm tra các màn danh mục tải dữ liệu thành công.
3. Mở Network tab xác nhận API trả mã 200 ổn định.

---

## 11. Trạng thái sau khi đã triển khai thành công

Đã đạt được:
- Frontend truy cập được qua domain public.
- Backend chạy ổn định và kết nối được database cloud.
- Dữ liệu nghiệp vụ hiển thị đúng (ví dụ danh mục Department có dữ liệu).
- Luồng đăng nhập hoạt động.

Hạng mục còn theo dõi:
- WebSocket thông báo có lúc báo disconnected/error.
- Đây là phần real-time bổ sung, chưa chặn được luồng chính của hệ thống.
- Có thể tối ưu ở giai đoạn hardening sau deploy.

---

## 12. Bài học kinh nghiệm

1. Với monorepo, Source Directory sai một ký tự cũng đủ làm deploy thất bại.
2. Angular production bắt buộc có cả environment.ts và environment.prod.ts cho cơ chế replace.
3. Một rule gitignore không đúng có thể khiến cloud build fail dù local vẫn chạy.
4. Luôn kiểm tra commit hash khi đọc log trên App Platform để tránh phân tích nhầm deployment cũ.
5. Readiness check backend và catchall frontend là hai cấu hình nền tảng để hệ thống ổn định.
6. Sau mỗi lần sửa lỗi, nên push commit nhỏ, rõ ràng, dễ truy vết nguyên nhân.

---

## 13. Các commit mốc trong hành trình

- 9cddf0c: tăng budget Angular production cho App Platform.
- 93cdbeb: cho phép theo dõi file environment.prod.ts trong git.
- 564b9a5: sửa rule gitignore để tracking file môi trường đúng cách.
- 1424851: bổ sung environment.ts và hoàn thiện theo dõi cả hai file môi trường.

---

## 14. Checklist vận hành nhanh cho nhóm

Trước khi deploy:
- [ ] Tài khoản DigitalOcean đã kích hoạt credit sinh viên và xác minh thanh toán.
- [ ] Có App Platform app với đủ 2 component backend và frontend.
- [ ] Có Managed MySQL cùng region.
- [ ] Đã import dữ liệu mẫu hoặc migrate schema.
- [ ] Đã cấu hình đầy đủ biến môi trường backend.
- [ ] Đã xác nhận file môi trường frontend tồn tại và được theo dõi trong git.

Trong lúc deploy:
- [ ] Theo dõi build log từng component riêng.
- [ ] Đối chiếu commit hash đúng với commit mới nhất.
- [ ] Nếu deploy lặp lỗi không rõ nguyên nhân, cân nhắc Force rebuild and deploy.

Sau khi deploy:
- [ ] Truy cập được trang chủ frontend.
- [ ] Đăng nhập thành công.
- [ ] API dữ liệu trả về bình thường.
- [ ] Kiểm tra nhanh các module trọng yếu như Department, Class, Student, Activity.

---

## 15. Đề xuất cải tiến sau triển khai

1. Hoàn thiện xử lý WebSocket production
   - Xem lại endpoint, handshake, timeout và cơ chế reconnect.
2. Bổ sung monitoring thực tế
   - Cấu hình cảnh báo dựa trên error rate và response time.
3. Bổ sung quy trình rollback
   - Chốt quy tắc quay lại commit ổn định khi deploy lỗi.
4. Chuẩn hóa tài liệu runbook nội bộ
   - Gộp checklist vận hành với checklist kiểm thử sau deploy.

Tài liệu này là phiên bản tổng kết theo đúng quá trình triển khai thực tế đã thực hiện thành công.
