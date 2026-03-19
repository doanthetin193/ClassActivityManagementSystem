# 🎓 Hướng Dẫn Demo – Hệ Thống Quản Lý Sinh Hoạt Lớp (QNU)

> **Mục tiêu:** Trình bày đầy đủ 14 chức năng của hệ thống cho giảng viên, theo đúng luồng nghiệp vụ thực tế với 5 loại tài khoản được phân quyền rõ ràng.
>
> **Bản cập nhật 03/2026 đã tích hợp:** Logo nhận diện QNU mới, đồng bộ đa ngôn ngữ (VI/EN/CN/LAO), dark mode, thông báo hoàn chỉnh, và bảo mật đăng nhập nâng cao (rate limit + khóa tạm thời).

---

## 📋 Thông Tin Trước Khi Demo

| Thông tin | Chi tiết |
|-----------|----------|
| **URL Frontend** | `http://localhost:4200` |
| **URL Backend** | `http://localhost:8000/AMQNU/api` (Spring Boot) |
| **Database** | MySQL |
| **Ngôn ngữ mặc định** | Tiếng Việt |

---

## 👥 Các Loại Tài Khoản (5 Actor)

Hệ thống có **5 loại actor** với phân quyền khác nhau:

| STT | Vai trò | Username | Mật khẩu | Mô tả |
|-----|---------|----------|-----------|-------|
| 1 | **SuperAdmin** | `admin` | `admin123` | Toàn quyền, quản lý toàn bộ hệ thống |
| 2 | **DepartmentAdmin** (Admin Khoa) | `vietxuan@gmail.com` | `vietxuan@gmail.com` | Quản lý trong phạm vi 1 khoa, nhận thông báo SHL, duyệt |
| 3 | **StudentAdmin** (Lớp trưởng) | `4451190072` | `4451190072` | Chọn giờ SHL, tạo mã điểm danh, ghi biên bản |
| 4 | **Academic Advisor** (Cố vấn HT) | `xuanquynh@gmail.com` | `xuanquynh@gmail.com` | Nhận thông báo SHL, xem biên bản, quản lý chức vụ SV |
| 5 | **Student** (Sinh viên thường) | `4451190073` | `4451190073` | Xem thông tin lớp, xem HDSHL, điểm danh |

> 💡 **Mẹo:** Mở sẵn **5 tab trình duyệt** (hoặc 1 cửa sổ thường + 1 ẩn danh) để chuyển đổi nhanh giữa các vai trò khi demo luồng điểm danh.

> ℹ️ **Lưu ý tài khoản tự động:**
> - Khi thêm **sinh viên** → hệ thống tự tạo account: `username = mã SV`, `password = ngày sinh`
> - Khi thêm **giảng viên/cán bộ** → hệ thống tự tạo account: `username = email`, `password = ngày sinh`

---

## 🗂️ Luồng Nghiệp Vụ Tổng Thể

```
[SuperAdmin]
    → Tạo phiên Sinh Hoạt Lớp + đính kèm file Hướng Dẫn SHL (PDF)
    → Gửi thông báo đến DepartmentAdmin của các khoa
         ↓
[DepartmentAdmin]
    → Nhận thông báo, xem file HDSHL
    → (Tùy chọn) Upload file PDF bổ sung yêu cầu của khoa
    → Duyệt → Hệ thống gửi thông báo đến StudentAdmin (Lớp trưởng)
         ↓
[StudentAdmin - Lớp trưởng]
    → Nhận thông báo, chọn giờ sinh hoạt lớp
    → Hệ thống tự động gửi thông báo đến Student + Academic Advisor
         ↓
[Student & Academic Advisor]
    → Nhận thông báo → Xem HDSHL
         ↓
[StudentAdmin - Lớp trưởng]  ← Bắt đầu buổi sinh hoạt
    → Bật mã điểm danh (hệ thống tạo mã ngẫu nhiên)
    → Trình chiếu mã lên màn hình
         ↓
[Student]
    → Nhập mã điểm danh → Hệ thống ghi nhận
         ↓
[StudentAdmin]
    → Kiểm tra + sửa danh sách điểm danh (nếu cần)
    → Ghi biên bản sinh hoạt lớp
         ↓
[Academic Advisor]
    → Nhận thông báo biên bản → Xem, xác nhận
         ↓
[DepartmentAdmin / SuperAdmin]
    → Xem tổng hợp biên bản tất cả lớp
```

---

## 🔐 PHẦN 1 – ĐĂNG NHẬP VÀ GIỚI THIỆU GIAO DIỆN

### Bước 1.1 – Mở trang đăng nhập

1. Mở trình duyệt, truy cập `http://localhost:4200`
2. Hệ thống tự chuyển sang trang đăng nhập
3. **Giới thiệu giao diện login:**
   - Logo QNU mới đã đồng bộ ở login và header
   - Panel trái: Form đăng nhập (Username + Password)
   - Panel phải: Banner minh họa hệ thống
   - Nút **chuyển ngôn ngữ** ở góc trên phải: 🇻🇳 VI / 🇺🇸 EN / 🇨🇳 中 / 🇱🇦 LAO
   - Có hỗ trợ **dark mode** ở header sau khi đăng nhập

### Bước 1.2 – Demo chuyển ngôn ngữ

1. Click icon cờ → chọn **English** → toàn bộ giao diện chuyển sang tiếng Anh
2. Chọn **中文** hoặc **LAO** để minh họa bản dịch đầy đủ ở login/header/sidebar
3. Chọn lại **Tiếng Việt** trước khi đăng nhập
4. *(Đây là tính năng đa ngôn ngữ hỗ trợ 4 thứ tiếng, đã xử lý các text cứng còn sót ở UI chính)*

### Bước 1.3 – Demo nhanh bảo mật đăng nhập (khuyến nghị 1 phút)

1. Nhập sai mật khẩu liên tiếp để hiển thị cảnh báo khóa tạm thời
2. Giải thích cơ chế:
   - Chặn bắn request dồn dập bằng rate limit
   - Khóa tạm thời khi sai nhiều lần liên tiếp
3. Nếu đang demo trên máy dev và cần làm lại nhanh: dùng nút reset lock dev trên trang login

---

## 👑 PHẦN 2 – DEMO TÀI KHOẢN SUPERADMIN

> **Mục tiêu:** Cho thấy SuperAdmin có quyền quản lý toàn bộ hệ thống.

### Bước 2.1 – Đăng nhập SuperAdmin

```
Username: admin
Password: admin123
→ Nhấn "Đăng nhập"
```

Sau khi đăng nhập:
- **Header:** Logo, nút ☰ thu/mở sidebar, chuông 🔔 thông báo, tên + vai trò người dùng, nút đăng xuất
- **Sidebar:** Menu phân nhóm đầy đủ (Hệ thống / Nhân sự / Hoạt động / Quản trị)

---

### 🏫 Bước 2.2 – Quản Lý Khoa *(Chức năng 1 theo báo cáo)*

**Menu: Hệ thống → Khoa**

1. Click **"Khoa"** → Danh sách các khoa, mỗi khoa có logo/avatar
2. **Demo các thao tác:**
   - 🔍 **Tìm kiếm** theo tên khoa
   - 👁️ **Xem chi tiết** → modal hiển thị thông tin + logo khoa
   - ➕ **Thêm khoa mới:** Click "Thêm khoa" → nhập Tên khoa, Mã khoa, upload Logo
   - 📁 **Import Excel:** Click "Thêm bằng XLSX" → tải file mẫu về → upload hàng loạt
   - ✏️ **Sửa:** Click icon bút chì → chỉnh sửa → "Lưu thay đổi"
   - 🗑️ **Xóa:** Click icon thùng rác → xác nhận → xóa
3. **Lưu ý:** Khoa là đơn vị gốc, lớp học – sinh viên – giảng viên đều thuộc về khoa cụ thể

---

### 📅 Bước 2.3 – Quản Lý Khóa Học *(Chức năng 2 theo báo cáo)*

**Menu: Hệ thống → Khóa**

1. Click **"Khóa"** → Danh sách niên khóa (Khóa 44, Khóa 45...)
2. **Demo:**
   - ➕ **Thêm niên khóa:** Nhập năm bắt đầu + năm kết thúc
   - 📁 Import Excel
   - ✏️ Sửa / 🗑️ Xóa

---

### 🏛️ Bước 2.4 – Quản Lý Lớp Học *(Chức năng 3 theo báo cáo)*

**Menu: Hệ thống → Lớp**

1. Click **"Lớp"** → Toàn bộ lớp của tất cả các khoa
2. **Demo:**
   - 🔍 **Tìm kiếm** theo tên lớp
   - 🔽 **Lọc** theo Khoa + Niên khóa → "Áp dụng"
   - 👁️ **Xem chi tiết lớp** → trang danh sách sinh viên trong lớp đó
   - ➕ **Thêm lớp mới:** Tên lớp, Mã lớp, chọn Khoa, Niên khóa
   - 📁 Import Excel
   - ✏️ Sửa / 🗑️ Xóa
3. **Lưu ý:** Mỗi lớp thuộc về 1 khoa và 1 niên khóa cụ thể

---

### 👩‍🏫 Bước 2.5 – Quản Lý Giảng Viên *(Chức năng 4 theo báo cáo)*

**Menu: Nhân sự → Giảng viên**

1. Click **"Giảng viên"** → Danh sách tất cả giảng viên
2. **Demo:**
   - 🔽 Lọc theo khoa
   - 👁️ **Xem chi tiết** một giảng viên
   - ➕ **Thêm giảng viên mới:** Họ tên, ngày sinh, giới tính, email, số điện thoại, khoa
   - ✅ **Hệ thống tự tạo tài khoản:** `username = email`, `password = ngày sinh`
   - 📁 Import Excel / ✏️ Sửa / 🗑️ Xóa
   - **Lưu ý:** Nếu giảng viên đang là cố vấn học tập → không thể xóa

---

### 🧑‍💼 Bước 2.6 – Quản Lý Cán Bộ *(Chức năng 5 theo báo cáo)*

**Menu: Nhân sự → Cán bộ**

1. Click **"Cán bộ"** → Danh sách cán bộ
2. **Demo:**
   - ➕ **Thêm cán bộ:** Họ tên, mã cán bộ, ngày sinh, email, nhiệm vụ
   - ✅ Hệ thống tự tạo tài khoản cho cán bộ (email + ngày sinh)
   - 📁 Import Excel / ✏️ Sửa / 🗑️ Xóa

---

### 👤 Bước 2.7 – Quản Lý Sinh Viên *(Chức năng 6 theo báo cáo)*

**Menu: Nhân sự → Sinh viên**

1. Click **"Sinh viên"** → Danh sách sinh viên với badge giới tính
2. **Demo:**
   - 🔍 Tìm kiếm theo tên
   - 🔽 Lọc theo Khoa / Niên khóa / Lớp
   - ➕ **Thêm sinh viên mới:** Họ tên, mã SV, ngày sinh, giới tính, lớp học
   - ✅ **Hệ thống tự tạo tài khoản:** `username = mã SV`, `password = ngày sinh`
   - 📁 Import Excel / ✏️ Sửa / 🗑️ Xóa

---

### 🎓 Bước 2.8 – Quản Lý Cố Vấn Học Tập *(Chức năng 7 theo báo cáo)*

**Menu: Nhân sự → Cố vấn học tập**

1. Click **"Cố vấn học tập"** → Danh sách phân công: **Giảng viên ↔ Lớp ↔ Năm học**
2. **Demo:**
   - ➕ **Thêm cố vấn:** Chọn giảng viên + Chọn lớp + Nhập năm học
   - 📁 Import Excel / ✏️ Sửa / 🗑️ Xóa
3. **Giải thích:** Đây là bước phân công giảng viên làm cố vấn học tập (Academic Advisor) cho từng lớp

---

### 🔐 Bước 2.9 – Quản Lý Phân Quyền (Vai Trò) *(Chức năng 8 theo báo cáo)*

**Menu: Quản trị → Vai trò**

1. Click **"Vai trò"** → Danh sách các role có trong hệ thống
2. Mỗi role có:
   - **Type badge:** 🔴 SUPERADMIN / 🟡 DEPARTMENT / 🔵 STUDENT
   - Số người đang giữ vai trò đó
3. **Demo các thao tác:**
   - 📋 **Xem quyền hạn:** Click icon 👁 → xem danh sách permissions của role
   - ➕ **Thêm role mới:** Đặt tên, chọn type (SUPERADMIN/DEPARTMENT/STUDENT), tích chọn permissions
   - ✏️ **Sửa role** (chỉ sửa được role cùng type với bản thân)
   - 👤➕ **Gán role cho tài khoản:** Click icon người+ → nhập username → "Gán vai trò"
   - 🗑️ **Xóa role** → tất cả tài khoản đang giữ role đó bị thu hồi quyền
4. **Giải thích hệ thống phân quyền:**
   - SuperAdmin: toàn quyền, tạo được role type SUPERADMIN/DEPARTMENT/STUDENT
   - DepartmentAdmin: chỉ quản lý trong phạm vi khoa, tạo được role type DEPARTMENT
   - StudentAdmin: lớp trưởng, có quyền đặc biệt trong bảng sinh hoạt lớp của lớp mình

---

### 📋 Bước 2.10 – Tạo Phiên Sinh Hoạt Lớp *(Chức năng 9 theo báo cáo)*

**Menu: Hoạt động → Sinh hoạt lớp**

1. Click **"Sinh hoạt lớp"** → Danh sách phiên SHL đã tạo
2. ➕ **Tạo phiên SHL mới:**
   - Nhập tên phiên sinh hoạt (ví dụ: "Sinh hoạt lớp tháng 3/2025")
   - Kéo thả hoặc Upload **file PDF hướng dẫn sinh hoạt lớp** (bắt buộc)
   - Chọn các lớp tham gia
   - Nhấn "Tạo phiên sinh hoạt"
3. **Kết quả:** Hệ thống tự động gửi **thông báo real-time** đến DepartmentAdmin của tất cả khoa liên quan
4. Quay lại danh sách → thấy phiên SHL mới với trạng thái **"Planned"**

---

### 👁️ Bước 2.11 – Xem Tổng Hợp Biên Bản

*(Từ trang Sinh hoạt lớp → Click icon "Xem chi tiết")*

1. Xem danh sách tất cả lớp đã/chưa nộp biên bản
2. Lọc theo Khoa / Niên khóa / Lớp / Trạng thái
3. Xem tỷ lệ điểm danh của từng lớp

---

### 🔔 Bước 2.12 – Thông Báo Real-time

1. Click icon 🔔 ở header
2. Thấy danh sách thông báo với badge số chưa đọc
3. Demo các thao tác mới:
   - Đánh dấu đã đọc từng thông báo
   - Đánh dấu đã đọc tất cả
   - Bấm **Xem tất cả** để vào trang Notifications
4. Trên trang Notifications: demo lọc **All/Unread** + phân trang
5. **Giới thiệu:** Hệ thống dùng **WebSocket** để gửi thông báo tức thì (không cần F5 trang)

---

### 👋 Bước 2.13 – Đăng Xuất

Click avatar → **"Đăng xuất"** → về trang đăng nhập

---

## 🏢 PHẦN 3 – DEMO TÀI KHOẢN DEPARTMENT ADMIN (ADMIN KHOA)

> **Mục tiêu:** Cho thấy DepartmentAdmin có quyền hạn trong phạm vi khoa + đặc biệt là chức năng "Thêm yêu cầu của khoa" và "Duyệt phiên SHL".

### Bước 3.1 – Đăng nhập DepartmentAdmin

```
Username: vietxuan@gmail.com
Password: vietxuan@gmail.com
```

### Bước 3.2 – So sánh phân quyền với SuperAdmin

| Chức năng | SuperAdmin | DepartmentAdmin |
|-----------|-----------|-----------------|
| Xem sinh viên | Tất cả khoa | Chỉ khoa mình |
| Xem giảng viên | Tất cả khoa | Chỉ khoa mình |
| Lọc theo Khoa | Có | Không cần (mặc định khoa mình) |
| Tạo phiên SHL | Tất cả khoa | Chỉ khoa mình |
| Xem biên bản | Tất cả lớp | Chỉ lớp thuộc khoa mình |
| Quản trị Role | Tất cả type | Chỉ DEPARTMENT type |

### Bước 3.3 – Nhận Thông Báo Phiên SHL

1. Click icon 🔔 → thấy thông báo "SuperAdmin đã tạo phiên sinh hoạt lớp tháng X"
2. Click vào thông báo → chuyển đến phiên SHL liên quan

### Bước 3.4 – Xem Hướng Dẫn Sinh Hoạt Lớp

1. Vào **Sinh hoạt lớp** → thấy phiên SHL mà SuperAdmin vừa tạo
2. Click icon 📄 (Xem file) → mở file PDF hướng dẫn từ SuperAdmin

### Bước 3.5 – Thêm Yêu Cầu Của Khoa *(UseCase đặc trưng – UC 2.3.1)*

> Đây là chức năng **đặc trưng nhất** của DepartmentAdmin trong hệ thống

1. Từ phiên SHL → Click **"Thêm yêu cầu khoa"** hoặc "Upload bổ sung"
2. Upload **file PDF** chứa yêu cầu bổ sung đặc thù của khoa CNTT
3. Nhấn **"Duyệt"** → Hệ thống gửi thông báo đến StudentAdmin (lớp trưởng) của tất cả lớp trong khoa
4. **Trình bày:** "Mỗi khoa có thể thêm yêu cầu riêng vào hướng dẫn SHL chung của trường"

### Bước 3.6 – Xem Biên Bản Sinh Hoạt Lớp *(UC 2.3.2)*

1. Vào trang Sinh hoạt lớp → xem danh sách lớp đã/chưa sinh hoạt
2. Lọc theo tháng/trạng thái
3. Click vào 1 lớp → xem biên bản chi tiết: thời gian, nội dung, tỷ lệ điểm danh

---

## 🎒 PHẦN 4 – DEMO TÀI KHOẢN STUDENT ADMIN (LỚP TRƯỞNG)

> **Mục tiêu:** Cho thấy StudentAdmin (lớp trưởng) là actor trung tâm của quy trình sinh hoạt lớp – chọn giờ, bật điểm danh, ghi biên bản.

### Bước 4.1 – Đăng nhập StudentAdmin (Lớp trưởng)

```
Username: 4451190072
Password: 4451190072
```

> ℹ️ StudentAdmin là sinh viên thuộc type **STUDENT** nhưng được gán role có quyền đặc biệt trong phạm vi lớp mình

### Bước 4.2 – Xem Sidebar của StudentAdmin

Sidebar có các mục:
- **"Lớp của tôi"** – xem thông tin lớp, danh sách SV, cố vấn
- **"Sinh hoạt lớp"** – quản lý phiên SHL của lớp mình

### Bước 4.3 – Nhận Thông Báo và Chọn Giờ Sinh Hoạt *(Chức năng 10 theo báo cáo)*

1. Click 🔔 → thấy thông báo "DepartmentAdmin đã duyệt phiên SHL tháng X"
2. Vào **"Sinh hoạt lớp"** → thấy buổi sinh hoạt đang chờ đặt giờ
3. Click icon ⏰ **(Chọn giờ sinh hoạt):**
   - Chọn ngày + giờ tổ chức (thường thứ 6 cuối tháng)
   - Nhấn **"Xác nhận"**
4. **Kết quả:** Hệ thống tự động gửi thông báo đến:
   - Tất cả sinh viên trong lớp
   - Giảng viên cố vấn học tập của lớp

---

## 🧑‍🏫 PHẦN 5 – DEMO TÀI KHOẢN ACADEMIC ADVISOR (CỐ VẤN HỌC TẬP)

> **Mục tiêu:** Cho thấy giảng viên cố vấn nhận thông báo, xem thông tin lớp và quản lý chức vụ sinh viên.

### Bước 5.1 – Đăng nhập Academic Advisor

```
Username: xuanquynh@gmail.com
Password: xuanquynh@gmail.com
```

### Bước 5.2 – Nhận Thông Báo SHL

1. Click 🔔 → thấy thông báo "Lớp KTPM44A sẽ sinh hoạt lúc 14:00 thứ 6..."
2. Click vào thông báo → xem chi tiết buổi sinh hoạt

### Bước 5.3 – Xem Thông Tin Lớp *(UC 2.3.12)*

1. Vào **"Lớp được cố vấn"** → thấy danh sách lớp mình đang cố vấn
2. Click vào 1 lớp → xem:
   - Danh sách sinh viên trong lớp (tên, mã SV, giới tính, trạng thái học)
   - Thông tin bản thân (cố vấn học tập của lớp)

### Bước 5.4 – Quản Lý Chức Vụ Sinh Viên *(UC 2.3.8)*

1. Trong trang danh sách sinh viên → click vào 1 sinh viên
2. Chọn **chức vụ:** Lớp trưởng / Phó lớp / Thư ký / Thành viên
3. Xác nhận → hệ thống cập nhật và thông báo về việc phân công

> **Giải thích:** Đây là quy trình Academic Advisor phân công StudentAdmin – sau khi gán role "Lớp trưởng", sinh viên đó mới có quyền thực hiện các chức năng ở Phần 4

---

## 👩‍🎓 PHẦN 6 – DEMO TÀI KHOẢN STUDENT (SINH VIÊN THƯỜNG)

> **Mục tiêu:** Cho thấy sinh viên thường nhận thông báo, đọc HDSHL và điểm danh.

### Bước 6.1 – Đăng nhập Student

```
Username: 4451190073
Password: 4451190073
```

### Bước 6.2 – Nhận Thông Báo SHL *(UC 2.3.11)*

1. Click 🔔 → thấy thông báo "Sinh hoạt lớp sẽ diễn ra lúc 14:00 thứ 6 ngày..."
2. Click vào thông báo → xem chi tiết

### Bước 6.3 – Xem Thông Tin Lớp *(UC 2.3.12)*

1. Sidebar chỉ có **"Lớp của tôi"**
2. Click → xem danh sách sinh viên trong lớp
3. Xem thông tin giảng viên cố vấn học tập của lớp

### Bước 6.4 – Xem Hướng Dẫn Sinh Hoạt Lớp *(Chức năng 12 theo báo cáo)*

1. Từ trang "Lớp của tôi" → danh sách phiên SHL → click vào phiên SHL
2. Click icon 📄 **"Xem HDSHL"** → mở file PDF hướng dẫn
3. **Hệ thống ghi nhận:** Sinh viên X đã xem HDSHL (hiển thị trong cột "Đã xem HDSHL")
4. **Trình bày:** "Quản lý có thể theo dõi sinh viên nào đã đọc, sinh viên nào chưa đọc tài liệu"

---

## 🎯 PHẦN 7 – DEMO LUỒNG ĐIỂM DANH HOÀN CHỈNH *(Chức năng 11 theo báo cáo)*

> Đây là **chức năng trọng tâm và đặc sắc nhất** của hệ thống – demo kết hợp StudentAdmin + Student

### Bước 7.1 – StudentAdmin bắt đầu điểm danh

*(Đã login StudentAdmin – lớp trưởng)*

1. Vào **"Sinh hoạt lớp"** → chọn buổi sinh hoạt đang diễn ra (trạng thái **Ongoing**)
2. Click **"Bắt đầu điểm danh"** → Hệ thống tạo **Mã điểm danh ngẫu nhiên**
3. **Trình chiếu mã lên màn hình lớp** để sinh viên thấy
4. Trong thực tế: trước giờ SHL 20 phút, hệ thống đã tự tạo sẵn mã

### Bước 7.2 – Student nhập mã điểm danh

*(Mở tab mới, đăng nhập Student thường)*

1. Vào **"Lớp của tôi"** → thấy buổi sinh hoạt đang **Ongoing** 🟡
2. Click icon ✅ **"Điểm danh"**
3. Nhập **mã điểm danh** từ StudentAdmin → Nhấn Xác nhận
4. Hệ thống ghi nhận: **Có mặt ✅** với timestamp
5. **Nếu nhập sai mã hoặc quá thời gian:** Hệ thống báo lỗi, ghi nhận **Vắng mặt ❌**

### Bước 7.3 – StudentAdmin kiểm tra và chỉnh sửa kết quả điểm danh

*(Quay lại tab StudentAdmin)*

1. Click icon 📊 **"Xem danh sách điểm danh"**
2. Thấy bảng: Tên SV | Trạng thái (Có mặt ✅ / Vắng mặt ❌) | Giờ điểm danh
3. **Sửa thủ công:** Click vào trạng thái → chỉnh sửa *(để tránh gian lận hoặc trường hợp đặc biệt)*
4. Kết thúc buổi sinh hoạt → trạng thái chuyển: **Ongoing → Completed** ✅

### Bước 7.4 – StudentAdmin ghi biên bản sinh hoạt lớp *(Chức năng 11 theo BC)*

1. Click icon ✏️ **"Ghi biên bản"**
2. Điền thông tin:
   - Số sinh viên có mặt / vắng mặt
   - Nhận xét về hoạt động tháng trước
   - Nhận xét chung của lớp
   - Kiến nghị, đề xuất
3. Nhấn nút **"+"** (góc dưới phải) để lưu biên bản
4. **Kết quả:** Hệ thống gửi thông báo đến Academic Advisor

### Bước 7.5 – DepartmentAdmin xem tổng hợp biên bản

*(Quay lại tab DepartmentAdmin)*

1. Vào Sinh hoạt lớp → thấy lớp vừa hoàn thành có trạng thái **Completed**
2. Click → xem biên bản chi tiết: thời gian, nội dung, tỷ lệ điểm danh, ý kiến lớp

---

## 📊 PHẦN 8 – TỔNG KẾT KHI DEMO

### Tóm tắt 14 chức năng theo báo cáo:

| STT | Chức năng | Actor chính |
|-----|-----------|------------|
| 1 | Đăng nhập | Tất cả role |
| 2 | Quản lý khoa | SuperAdmin |
| 3 | Quản lý khóa học | SuperAdmin |
| 4 | Quản lý giảng viên | SuperAdmin |
| 5 | Quản lý cán bộ | SuperAdmin |
| 6 | Quản lý lớp học | SuperAdmin |
| 7 | Quản lý sinh viên | SuperAdmin |
| 8 | Quản lý cố vấn học tập | SuperAdmin / DepartmentAdmin |
| 9 | Quản lý phân quyền (vai trò) | SuperAdmin |
| 10 | Tạo & duyệt phiên sinh hoạt lớp | SuperAdmin → DepartmentAdmin → StudentAdmin |
| 11 | Quản lý điểm danh | StudentAdmin + Student |
| 12 | Chọn giờ sinh hoạt lớp | StudentAdmin |
| 13 | Ghi biên bản sinh hoạt lớp | StudentAdmin |
| 14 | Xem SV đã đọc hướng dẫn SHL | DepartmentAdmin / SuperAdmin |

### Điểm nổi bật cần nhấn mạnh:

| Tính năng | Điểm nổi bật |
|-----------|-------------|
| 🔐 **Phân quyền 5 cấp** | SuperAdmin > DepartmentAdmin > StudentAdmin > Academic Advisor > Student |
| 🛡️ **Bảo mật đăng nhập** | Rate limit + khóa tạm thời + hiển thị thời gian thử lại |
| 🌐 **Đa ngôn ngữ** | Hỗ trợ 4 ngôn ngữ: Việt, Anh, Trung, Lào |
| 🌙 **Dark mode** | Chuyển sáng/tối trực tiếp từ header, đồng bộ các màn hình chính |
| ⚡ **Thông báo Real-time** | WebSocket – không cần F5, nhận ngay lập tức |
| 🔔 **Thông báo hoàn chỉnh** | Read one / Read all / View all + trang Notifications có lọc, phân trang |
| 📊 **Import Excel** | Nhập hàng loạt Khoa / Lớp / Sinh viên / Giảng viên... |
| 📁 **Upload PDF** | Hướng dẫn SHL + Yêu cầu bổ sung của khoa |
| 🎲 **Mã điểm danh** | Tạo tự động, ngẫu nhiên, giới hạn thời gian |
| 📋 **CRUD đầy đủ** | Thêm / Sửa / Xóa / Xem cho tất cả entity |
| 🔍 **Theo dõi xem tài liệu** | Biết SV nào đã/chưa đọc HDSHL |
| 🏷️ **Nhận diện thương hiệu** | Logo QNU mới đồng bộ ở login + header + favicon |

---

## ⚠️ Lưu Ý Khi Demo

1. **Khởi động backend trước:** `http://localhost:8000` phải đang chạy
2. **Chuẩn bị sẵn dữ liệu mẫu:** Ít nhất 1 khoa, 1 niên khóa, 1 lớp, sinh viên, giảng viên
3. **Tài khoản cần có sẵn:** 1 DepartmentAdmin, 1 StudentAdmin (lớp trưởng), 1 Advisor, 1 Student thường
   - `admin / admin123`
   - `vietxuan@gmail.com / vietxuan@gmail.com`
   - `xuanquynh@gmail.com / xuanquynh@gmail.com`
   - `4451190072 / 4451190072`
   - `4451190073 / 4451190073`
4. **Phiên SHL cần có:** Ít nhất 1 buổi ở trạng thái **Ongoing** để demo điểm danh trực tiếp
5. **Không xóa dữ liệu gốc** trong quá trình demo – chỉ thêm hoặc sửa thử nghiệm
6. **Thứ tự đăng nhập khuyến nghị:** SuperAdmin (`admin`) → DepartmentAdmin (`vietxuan@gmail.com`) → StudentAdmin (`4451190072`) → Advisor (`xuanquynh@gmail.com`) → Student (`4451190073`)
7. **Kiểm tra ngôn ngữ trước giờ demo:** đổi thử VI/EN/CN/LAO để chắc chắn không còn text hard-code
8. **Kiểm tra logo mới:** hiển thị đúng ở login và header
9. **Nếu demo bảo mật login:** chuẩn bị sẵn thao tác nhập sai nhiều lần và reset lock dev

---

## 🕐 Thời Gian Demo Gợi Ý

| Phần | Nội dung | Thời gian |
|------|----------|-----------|
| Phần 1 | Trang đăng nhập, đa ngôn ngữ | 2 phút |
| Phần 2 | SuperAdmin: 9 chức năng quản trị | 10 phút |
| Phần 3 | DepartmentAdmin: thêm yêu cầu khoa, duyệt | 4 phút |
| Phần 4 | StudentAdmin: chọn giờ SHL | 2 phút |
| Phần 5 | Academic Advisor: chức vụ, xem lớp | 2 phút |
| Phần 6 | Student: xem lớp, đọc HDSHL | 2 phút |
| Phần 7 | **Luồng điểm danh hoàn chỉnh** | 5 phút |
| Phần 8 | Tổng kết, Q&A | 3 phút |
| **Tổng** | | **~30 phút** |

---

*Báo cáo đồ án: Cao Thanh Vương – KTPM K44 – MSSV: 4451190071 – Khoa CNTT, ĐH Quy Nhơn*  
*Chúc bạn demo thành công! 🎉*
