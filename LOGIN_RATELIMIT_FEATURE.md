# Tài liệu chức năng: Rate Limit đăng nhập (Login Rate Limiting)

> **Ngày thực hiện:** 19/03/2026  
> **Phạm vi:** Backend (Spring Boot) + Frontend (Angular)  
> **Mục tiêu:** Tăng bảo mật đăng nhập, giảm brute-force, nhưng vẫn giữ UX tốt và thuận tiện cho môi trường dev/deploy

---

## 1. Bài toán cần giải quyết

Trước khi triển khai, luồng đăng nhập có các vấn đề:

- Có thể thử sai mật khẩu nhiều lần liên tục (nguy cơ brute-force)
- Nếu chỉ chặn cứng theo request, người dùng thật dễ bị khó chịu
- Frontend không hiển thị rõ thời gian chờ khi bị khóa
- Khi dev, mỗi lần bị khóa phải đợi rất lâu hoặc restart backend

Chức năng **Login Rate Limiting** được triển khai để xử lý đồng thời bảo mật + trải nghiệm + tiện dev.

---

## 2. Giải pháp tổng thể đã làm

Áp dụng mô hình **lai 2 lớp**:

| Lớp | Cơ chế | Mục tiêu |
|---|---|---|
| 1 | **Rate limit theo request** (filter) | Chặn burst traffic / bot theo IP ở mức nhẹ |
| 2 | **Khóa theo số lần đăng nhập sai** | Chỉ tăng khi sai mật khẩu, đăng nhập đúng thì reset lại |

Kết quả:

- Vẫn chống brute-force tốt
- Trải nghiệm người dùng thật tốt hơn (đúng mật khẩu thì reset counter)
- Có thông báo rõ ràng: khóa tạm, còn bao nhiêu lần thử
- Có nút reset lock riêng cho môi trường dev

---

## 3. Hành vi nghiệp vụ đã triển khai

### 3.1 Luồng đăng nhập

1. User gửi `POST /auth/log-in`
2. Filter kiểm tra burst limit theo IP
3. Service kiểm tra user có đang bị khóa theo số lần sai không
4. Nếu chưa khóa:
   - Sai mật khẩu: tăng counter sai, trả số lần còn lại
   - Đúng mật khẩu: reset counter sai về 0, trả token
5. Nếu vượt ngưỡng sai: khóa tạm và trả `429` kèm `Retry-After`

---

### 3.2 Quy tắc chính

- **Sai mật khẩu mới bị tính fail attempt**
- **Đăng nhập đúng sẽ reset fail attempt**
- **Vượt ngưỡng fail attempt → khóa tạm 15 phút** (mặc định)
- **Frontend đọc `Retry-After` để đếm ngược chính xác**
- **Frontend đọc `X-Login-Attempts-Remaining` để hiển thị “Còn X lần thử…”**

---

## 4. Backend — Spring Boot

### 4.1 Thành phần mới / đã cập nhật

#### `component/RateLimitFilter.java`
- Rate limit request-level cho các endpoint nhạy cảm
- Login giới hạn theo IP
- Roll-call giới hạn theo user đã xác thực (fallback IP)
- Trả `429` + header `Retry-After`
- Có helper reset buckets để phục vụ dev

#### `service/LoginAttemptService.java` (**mới**)
- Quản lý số lần đăng nhập sai theo username
- `ensureNotLocked(username)`: kiểm tra khóa tạm
- `onLoginFailure(username)`: tăng counter, trả số lần còn lại
- `onLoginSuccess(username)`: reset counter
- `resetAll()`: reset toàn bộ lock/counter (dev)

#### `service/AuthenticationService.java`
- Tích hợp check lock trước xác thực
- Khi sai tài khoản/mật khẩu: tăng fail counter và ném exception chứa số lần còn lại
- Khi đúng: reset fail counter

#### `exception/LoginTemporarilyLockedException.java` (**mới**)
- Exception cho trường hợp bị khóa tạm
- Mang thông tin `retryAfterSeconds`

#### `exception/LoginFailedAttemptException.java` (**mới**)
- Exception cho trường hợp đăng nhập sai
- Mang thông tin `remainingAttempts`

#### `exception/GlobalExceptionHandler.java`
- Bắt và trả lỗi chuẩn JSON cho 2 exception mới
- Thêm header:
  - `Retry-After`
  - `X-Login-Attempts-Remaining`

#### `component/CorsFilter.java`
- Expose headers cho frontend đọc được:
  - `Retry-After`
  - `X-Login-Attempts-Remaining`

#### `controller/DevRateLimitController.java` (**mới**)
- Chỉ chạy ở profile `dev`
- Endpoint reset lock nhanh:
  - `POST /dev/rate-limit/reset`
- Reset cả request buckets và login-attempt locks

#### `constant/EndPoint.java`
- Mở public endpoint cho reset dev:
  - `dev/rate-limit/reset`

---

### 4.2 Cấu hình backend

#### `application.yaml`

```yaml
rate-limit:
  enabled: true
  cleanup-threshold: 2000
  trusted-ips: ""
  login:
    enabled: true
    path: /auth/log-in
    max-requests: 30
    window-seconds: 60
  roll-call:
    enabled: true
    path: /attendance/roll-call
    max-requests: 20
    window-seconds: 60

login-attempt:
  enabled: true
  max-failures: 5
  lock-seconds: 900
  reset-window-seconds: 900
```

#### `application-dev.yml`

- Giữ `rate-limit.enabled=true` để test đúng behavior production ngay tại local.

---

## 5. Frontend — Angular

### 5.1 Các cập nhật chính

#### `components/auth/login/login.component.ts`
- Bắt lỗi `429` và đọc `Retry-After`
- Bắt lỗi `401` và đọc `X-Login-Attempts-Remaining`
- Hiển thị message:
  - `Sai tài khoản hoặc mật khẩu. Còn X lần thử trước khi bị khóa.`
- Quản lý lock state bằng `localStorage` (vẫn lock sau khi refresh)
- Nút dev reset lock gọi endpoint backend

#### `components/auth/login/login.component.html`
- Cảnh báo lock tĩnh: `Đăng nhập tạm khóa.`
- Countdown chỉ hiển thị ở nút đăng nhập
- Thêm nút nhỏ `Reset lock (dev)` (chỉ hiện khi local)

#### `components/auth/login/login.component.css`
- Style nút reset dev
- Đồng bộ dark mode login với giao diện dark tổng thể

#### `service/auth.service.ts`
- Thêm method:
  - `resetRateLimitDev(path: string)`

---

## 6. API liên quan

| Method | Endpoint | Mục đích | Ghi chú |
|---|---|---|---|
| POST | `/auth/log-in` | Đăng nhập | Có rate-limit + fail-attempt lock |
| POST | `/dev/rate-limit/reset` | Reset lock/counter | Chỉ profile `dev` |

### Response headers được sử dụng

| Header | Ý nghĩa |
|---|---|
| `Retry-After` | Số giây còn lại trước khi được login lại |
| `X-Login-Attempts-Remaining` | Số lần thử còn lại trước khi bị khóa |

---

## 7. UX hiển thị trên màn đăng nhập

### Trường hợp sai mật khẩu nhưng chưa khóa

- Hiển thị:  
  `Sai tài khoản hoặc mật khẩu. Còn X lần thử trước khi bị khóa.`

### Trường hợp bị khóa tạm

- Alert trên form:  
  `Đăng nhập tạm khóa.`
- Nút đăng nhập:  
  `Đăng nhập lại sau Xm Ys`
- Input bị disable trong thời gian lock

### Trường hợp dev cần test nhanh

- Bấm nút `Reset lock (dev)` để xóa lock ngay

---

## 8. Luồng hoạt động tổng quát

```
[User nhập username/password]
        ↓
[RateLimitFilter kiểm tra burst theo IP]
        ↓
[AuthenticationService gọi LoginAttemptService.ensureNotLocked]
        ↓
   ┌─────────────────────────────────────────────────┐
   │ Nếu đang khóa: throw LoginTemporarilyLocked    │
   │ → GlobalExceptionHandler trả 429 + Retry-After │
   └─────────────────────────────────────────────────┘
        ↓
[Xác thực mật khẩu]
   ├─ Sai → onLoginFailure(username)
   │        ├─ Còn lượt: throw LoginFailedAttemptException(remaining)
   │        └─ Hết lượt: throw LoginTemporarilyLockedException
   └─ Đúng → onLoginSuccess(username) (reset fail counter)
        ↓
[Frontend đọc header, cập nhật thông báo/đếm ngược]
```

---

## 9. Danh sách file đã tạo/sửa

### Backend

| File | Loại | Mô tả |
|---|---|---|
| `back_end_class_activity/src/main/java/com/manager/class_activity/qnu/service/LoginAttemptService.java` | **Mới** | Quản lý fail attempts và lock |
| `back_end_class_activity/src/main/java/com/manager/class_activity/qnu/config/LoginAttemptProperties.java` | **Mới** | Config cho fail-attempt lock |
| `back_end_class_activity/src/main/java/com/manager/class_activity/qnu/exception/LoginTemporarilyLockedException.java` | **Mới** | Exception khóa tạm |
| `back_end_class_activity/src/main/java/com/manager/class_activity/qnu/exception/LoginFailedAttemptException.java` | **Mới** | Exception trả số lần còn lại |
| `back_end_class_activity/src/main/java/com/manager/class_activity/qnu/controller/DevRateLimitController.java` | **Mới** | Reset lock ở môi trường dev |
| `back_end_class_activity/src/main/java/com/manager/class_activity/qnu/service/AuthenticationService.java` | **Sửa** | Tích hợp lock + reset khi login đúng |
| `back_end_class_activity/src/main/java/com/manager/class_activity/qnu/exception/GlobalExceptionHandler.java` | **Sửa** | Trả header Retry-After + Remaining |
| `back_end_class_activity/src/main/java/com/manager/class_activity/qnu/component/RateLimitFilter.java` | **Sửa** | Rate-limit request-level + helpers |
| `back_end_class_activity/src/main/java/com/manager/class_activity/qnu/component/CorsFilter.java` | **Sửa** | Expose headers cho browser |
| `back_end_class_activity/src/main/java/com/manager/class_activity/qnu/constant/EndPoint.java` | **Sửa** | Thêm endpoint reset dev |
| `back_end_class_activity/src/main/resources/application.yaml` | **Sửa** | Cấu hình rate-limit + login-attempt |
| `back_end_class_activity/src/main/resources/application-dev.yml` | **Sửa** | Bật rate-limit ở local dev |

### Frontend

| File | Loại | Mô tả |
|---|---|---|
| `qnu_fe/src/app/components/auth/login/login.component.ts` | **Sửa** | Xử lý 429/401, countdown, remaining attempts |
| `qnu_fe/src/app/components/auth/login/login.component.html` | **Sửa** | Alert lock + countdown button + nút reset dev |
| `qnu_fe/src/app/components/auth/login/login.component.css` | **Sửa** | Style nút reset dev + dark mode login |
| `qnu_fe/src/app/service/auth.service.ts` | **Sửa** | Thêm API gọi reset lock dev |

---

## 10. Cách test nhanh

1. Chạy backend + frontend
2. Sai mật khẩu liên tiếp:
   - Quan sát message còn `X` lần thử
3. Sai tới ngưỡng:
   - Nhận `429` + countdown lock
4. Nhập đúng trước ngưỡng:
   - Counter sai reset
5. Khi đang lock:
   - Bấm `Reset lock (dev)` để mở ngay

---

## 11. Kết luận

Giải pháp rate-limit đã được triển khai theo hướng cân bằng:

- **Bảo mật:** có burst control + khóa theo fail attempts
- **UX:** thông báo rõ số lần còn lại, countdown chính xác
- **Dev-friendly:** có endpoint + nút reset lock để test nhanh
- **Deploy-friendly:** cấu hình qua env vars, không phụ thuộc hạ tầng phức tạp

Tài liệu này có thể dùng trực tiếp cho phần báo cáo chức năng bảo mật đăng nhập.
