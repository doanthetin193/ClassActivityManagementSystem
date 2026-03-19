# Hướng Dẫn Demo Với 5 Tài Khoản Cố Định

## Kết luận nhanh

Bộ 5 tài khoản hiện tại là **đủ để demo gần như toàn bộ website**.

Cụ thể, bạn có thể demo được:
- Đăng nhập và phân quyền nhiều cấp
- Bảo mật đăng nhập nâng cao (rate limit + khóa tạm thời)
- Quản trị hệ thống bằng SuperAdmin
- Quản lý trong phạm vi khoa bằng DepartmentAdmin
- Vai trò cố vấn học tập
- Vai trò lớp trưởng
- Vai trò sinh viên thường
- Quản lý tài khoản, reset mật khẩu, khóa/mở khóa
- Quản lý khoa, khóa, lớp, sinh viên, giảng viên, cán bộ, cố vấn
- Vai trò và phân quyền
- Sinh hoạt lớp, thông báo, xem lớp, xem tài liệu, điểm danh, biên bản
- Đa ngôn ngữ (VI/EN/CN/LAO), dark mode, và giao diện logo mới

## Cập nhật mới đã triển khai (03/2026)

### 1. Bảo mật đăng nhập

- Đã triển khai cơ chế hybrid:
	- Giới hạn tần suất request đăng nhập (burst rate limit)
	- Khóa tạm thời theo số lần nhập sai liên tiếp
- Khi đăng nhập đúng, bộ đếm sai sẽ được reset
- Frontend có hiển thị trạng thái khóa tạm thời và thời gian thử lại
- Có nút reset lock cho môi trường dev để test nhanh trước giờ demo

### 2. Luồng quên mật khẩu cho mô hình nội bộ

- Không có self-register công khai (phù hợp hệ thống nội bộ)
- Nút Quên mật khẩu hiển thị hướng dẫn liên hệ nội bộ
- Bấm lần nữa sẽ đóng phần hướng dẫn

### 3. Thông báo hoàn chỉnh để demo

- Chuông thông báo realtime (WebSocket)
- Popup thông báo có:
	- Đánh dấu đã đọc từng mục
	- Đánh dấu đã đọc tất cả
	- Xem tất cả
- Trang thông báo riêng có phân trang, lọc All/Unread, thao tác read one/read all

### 4. Đồng bộ giao diện và i18n

- Đã đồng bộ đa ngôn ngữ cho login/header/sidebar
- Đã sửa các text cứng còn sót (title, aria-label, menu label)
- Đã cập nhật brand text theo ngôn ngữ, gồm cả VI và LAO

### 5. Cập nhật nhận diện

- Đã thay logo bằng ảnh chính thức Quy Nhơn University tại login và header
- Logo favicon cũng dùng cùng bộ nhận diện

## Lưu ý quan trọng

Bộ 5 tài khoản này **đủ demo web** theo nghĩa trình bày chức năng và trải nghiệm người dùng.

Tuy nhiên, nếu bạn muốn một luồng nghiệp vụ hoàn toàn khép kín theo đúng một khoa và một lớp duy nhất từ đầu đến cuối, thì dữ liệu demo hiện tại có thể chưa đồng bộ tuyệt đối giữa tất cả actor.

Điều đó **không cản trở việc demo**. Cách thực tế nhất là:
- Dùng `admin` để demo toàn quyền
- Dùng `vietxuan@gmail.com` để demo DepartmentAdmin
- Dùng `xuanquynh@gmail.com` để demo Academic Advisor
- Dùng `4451190072` để demo StudentAdmin
- Dùng `4451190073` để demo Student thường

## 5 tài khoản demo đã chốt

| STT | Vai trò demo | Username | Password |
|-----|--------------|----------|----------|
| 1 | SuperAdmin | `admin` | `admin123` |
| 2 | DepartmentAdmin | `vietxuan@gmail.com` | `vietxuan@gmail.com` |
| 3 | Academic Advisor | `xuanquynh@gmail.com` | `xuanquynh@gmail.com` |
| 4 | StudentAdmin (Lớp trưởng) | `4451190072` | `4451190072` |
| 5 | Student thường | `4451190073` | `4451190073` |

## Thứ tự đăng nhập khuyến nghị khi demo

1. `admin`
2. `vietxuan@gmail.com`
3. `xuanquynh@gmail.com`
4. `4451190072`
5. `4451190073`

## Mục tiêu demo theo từng tài khoản

### 1. SuperAdmin

Tài khoản:

```text
Username: admin
Password: admin123
```

Chức năng nên demo:
- Quản lý khoa
- Quản lý khóa học
- Quản lý lớp
- Quản lý giảng viên
- Quản lý cán bộ
- Quản lý sinh viên
- Quản lý cố vấn học tập
- Quản lý vai trò
- Quản lý tài khoản
- Quản lý sinh hoạt lớp

Các điểm nên nhấn mạnh:
- Đây là tài khoản toàn quyền
- Nhìn thấy đầy đủ menu quản trị
- Có thể reset password để chuẩn bị các tài khoản demo khác
- Có thể khóa/mở khóa tài khoản

### 2. DepartmentAdmin

Tài khoản:

```text
Username: vietxuan@gmail.com
Password: vietxuan@gmail.com
```

Chức năng nên demo:
- Quản lý giảng viên trong phạm vi khoa
- Quản lý cán bộ trong phạm vi khoa
- Quản lý lớp trong phạm vi khoa
- Quản lý sinh viên trong phạm vi khoa
- Quản lý cố vấn học tập trong phạm vi khoa
- Quản lý role trong phạm vi type DEPARTMENT
- Xem Activity / Sinh hoạt lớp ở góc nhìn khoa

Các điểm nên nhấn mạnh:
- Không còn menu của SuperAdmin
- Chỉ thấy dữ liệu trong phạm vi khoa của mình
- Đây là tài khoản quản lý khoa, tương đương actor DepartmentAdmin trong demo

### 3. Academic Advisor

Tài khoản:

```text
Username: xuanquynh@gmail.com
Password: xuanquynh@gmail.com
```

Chức năng nên demo:
- Xem lớp được cố vấn
- Xem danh sách sinh viên trong lớp
- Xem thông tin sinh hoạt lớp
- Tham gia luồng theo dõi sinh hoạt lớp ở góc nhìn giảng viên cố vấn

Các điểm nên nhấn mạnh:
- Đây là giảng viên có phân công cố vấn học tập
- Có thể dùng để trình bày actor Academic Advisor trong tài liệu

### 4. StudentAdmin (Lớp trưởng)

Tài khoản:

```text
Username: 4451190072
Password: 4451190072
```

Chức năng nên demo:
- Xem lớp của tôi
- Xem sinh hoạt lớp
- Chọn giờ sinh hoạt lớp
- Thao tác điểm danh
- Ghi biên bản sinh hoạt lớp

Các điểm nên nhấn mạnh:
- Đây là actor trung tâm của phần nghiệp vụ sinh hoạt lớp
- Có thể dùng để demo điểm danh và biên bản

### 5. Student thường

Tài khoản:

```text
Username: 4451190073
Password: 4451190073
```

Chức năng nên demo:
- Xem lớp của tôi
- Nhận thông báo
- Xem tài liệu hướng dẫn sinh hoạt lớp
- Điểm danh bằng mã

Các điểm nên nhấn mạnh:
- Đây là actor cuối trong luồng nghiệp vụ
- Phù hợp để demo trải nghiệm người dùng phổ thông

## Kịch bản demo ngắn gọn 20-30 phút

### Mở đầu 2-3 phút (gợi ý)

- Chuyển nhanh ngôn ngữ (VI -> EN -> CN hoặc LAO)
- Bật/tắt dark mode
- Nhấn mạnh logo nhận diện trường đã đồng bộ
- Giới thiệu nhanh cơ chế bảo mật đăng nhập mới

### Phần 1. SuperAdmin

Đăng nhập `admin` và demo:
- Khoa
- Khóa
- Lớp
- Giảng viên
- Cán bộ
- Sinh viên
- Cố vấn học tập
- Vai trò
- Quản lý tài khoản
- Sinh hoạt lớp
- Mở chuông thông báo để demo đọc từng thông báo / đọc tất cả
- Có thể reset password một tài khoản để chuẩn bị login các phần sau

### Phần 2. DepartmentAdmin

Đăng nhập `vietxuan@gmail.com` và demo:
- Các menu quản lý trong phạm vi khoa
- Sự khác biệt so với SuperAdmin
- Quyền hạn thu gọn nhưng vẫn đủ để vận hành ở cấp khoa

### Phần 3. Academic Advisor

Đăng nhập `xuanquynh@gmail.com` và demo:
- Xem lớp được cố vấn
- Theo dõi lớp
- Vai trò giảng viên trong quy trình sinh hoạt lớp

### Phần 4. StudentAdmin

Đăng nhập `4451190072` và demo:
- Xem lớp của tôi
- Sinh hoạt lớp
- Chọn giờ sinh hoạt
- Điểm danh
- Ghi biên bản

### Phần 5. Student thường

Đăng nhập `4451190073` và demo:
- Nhận thông báo
- Xem tài liệu
- Điểm danh
- Xem thông tin lớp

### Chèn nhanh 2 phút bảo mật login (nên có)

- Thử nhập sai nhiều lần để hiển thị trạng thái khóa tạm thời
- Giải thích đây là lớp bảo vệ chống brute-force
- Nếu cần làm lại nhanh trên máy dev: dùng nút reset lock rồi demo lại

## Cách xử lý nếu cần login nhanh trước giờ demo

Nếu quên mật khẩu của một tài khoản demo:
- Đăng nhập `admin`
- Vào `Quản lý tài khoản`
- Tìm tài khoản
- Bấm `Reset password`
- Để trống ô mật khẩu mới
- Hệ thống sẽ tự reset password = username

## Checklist trước giờ demo

1. Kiểm tra backend đang chạy ở cổng `8000`
2. Kiểm tra frontend đang chạy ở cổng `4200`
3. Kiểm tra đăng nhập lại được 5 tài khoản trên
4. Kiểm tra các tài khoản không bị khóa
5. Nếu cần, reset lại password về đúng `username`
6. Chuẩn bị sẵn 5 tab trình duyệt hoặc 1 cửa sổ thường + 1 cửa sổ ẩn danh
7. Kiểm tra chuông thông báo có dữ liệu để demo popup và trang Notifications
8. Kiểm tra đổi ngôn ngữ không còn text tiếng Anh bị hard-code
9. Kiểm tra logo mới hiển thị đúng ở login và header
10. Nếu cần demo lock login: chuẩn bị sẵn kịch bản nhập sai và thao tác reset lock dev

## Chốt lại

Với bộ tài khoản hiện tại, bạn **đủ để demo toàn bộ web ở mức chức năng và phân quyền**.

Nếu sau này muốn demo theo luồng nghiệp vụ khép kín đẹp hơn nữa, bạn chỉ cần tinh chỉnh lại dữ liệu sao cho DepartmentAdmin, Advisor, StudentAdmin và Student thường cùng nằm trong một khoa/lớp mục tiêu. Nhưng cho buổi demo hiện tại, bộ này là dùng được.