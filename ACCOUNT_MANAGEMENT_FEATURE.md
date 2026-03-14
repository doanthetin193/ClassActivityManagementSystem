# Tài liệu chức năng: Quản lý tài khoản (Account Management)

> **Ngày thực hiện:** 03/03/2026  
> **Phạm vi:** Backend (Spring Boot) + Frontend (Angular)  
> **Quyền truy cập:** Chỉ tài khoản **SUPERADMIN** mới có quyền sử dụng chức năng này

---

## 1. Vấn đề cần giải quyết

Hệ thống hiện tại không có giao diện quản trị để kiểm soát tài khoản người dùng. Người quản trị (SUPERADMIN) không thể:

- Xem danh sách tất cả tài khoản đang tồn tại trong hệ thống
- Biết mỗi tài khoản đang liên kết với ai (giảng viên, sinh viên, cán bộ hay admin)
- Đặt lại mật khẩu khi người dùng quên hoặc cần reset
- Khóa tài khoản vi phạm mà không cần xóa hoàn toàn
- Mở khóa lại tài khoản đã bị khóa trước đó

Chức năng **Quản lý tài khoản** ra đời để giải quyết toàn bộ các vấn đề trên.

---

## 2. Các chức năng nhỏ được đáp ứng

| # | Chức năng | Mô tả |
|---|-----------|-------|
| 1 | **Xem danh sách tài khoản** | Hiển thị toàn bộ tài khoản có phân trang |
| 2 | **Tìm kiếm** | Tìm theo tên đăng nhập (username) |
| 3 | **Lọc theo loại** | Lọc theo type: SUPERADMIN / DEPARTMENT / STUDENT |
| 4 | **Lọc theo trạng thái** | Lọc tài khoản đang hoạt động hoặc đã bị khóa |
| 5 | **Xem thông tin liên kết** | Biết tài khoản liên kết với Giảng viên / Sinh viên / Cán bộ / Admin |
| 6 | **Đặt lại mật khẩu** | Reset mật khẩu về giá trị tùy chọn; nếu để trống → mặc định bằng username |
| 7 | **Khóa tài khoản** | Đánh dấu `isDeleted = true`, tài khoản không thể đăng nhập |
| 8 | **Mở khóa tài khoản** | Đánh dấu `isDeleted = false`, khôi phục quyền đăng nhập |
| 9 | **Bảo vệ tự khóa** | Admin không thể tự khóa chính tài khoản của mình (cả BE lẫn FE) |
| 10 | **Đa ngôn ngữ** | Toàn bộ giao diện hỗ trợ Tiếng Việt / English / 中文 / ລາວ |

---

## 3. Backend — Spring Boot

### 3.1 Kiến trúc tổng quan

Tuân theo pattern chuẩn của project:

```
AccountController  →  AccountService  →  AccountRepository  →  Database
```

Tất cả endpoint yêu cầu vai trò **SUPERADMIN** (kiểm tra qua `SecurityUtils.getCurrentUserType()`).

---

### 3.2 Các file mới tạo

#### `dto/response/AccountResponse.java`
DTO trả về thông tin tài khoản cho client.

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AccountResponse {
    private int id;
    private String username;
    private String type;          // SUPERADMIN / DEPARTMENT / STUDENT
    private String linkedName;    // Tên thực của người dùng liên kết
    private String linkedRole;    // Giảng viên / Sinh viên / Cán bộ / Admin
    private boolean isDeleted;    // true = đã khóa
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

#### `dto/request/ResetPasswordRequest.java`
DTO nhận yêu cầu đặt lại mật khẩu.

```java
public class ResetPasswordRequest {
    private int accountId;
    private String newPassword;  // nullable → nếu null thì reset về username
}
```

---

#### `dto/request/FilterAccount.java`
DTO chứa bộ lọc khi tìm kiếm tài khoản, kế thừa `AbstractFilter` (có sẵn `keyWord`).

```java
public class FilterAccount extends AbstractFilter {
    private String type;          // null = lấy tất cả loại
    private Boolean isDeleted;    // null = lấy tất cả trạng thái
}
```

---

#### `controller/AccountController.java`
Controller định nghĩa 5 endpoint REST.

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `POST` | `/accounts/search` | Tìm kiếm + phân trang tài khoản |
| `GET` | `/accounts/{id}` | Lấy chi tiết 1 tài khoản |
| `PUT` | `/accounts/reset-password` | Đặt lại mật khẩu |
| `PUT` | `/accounts/{id}/lock` | Khóa tài khoản |
| `PUT` | `/accounts/{id}/unlock` | Mở khóa tài khoản |

**Ví dụ:**
```java
@PostMapping("/search")
public ResponseEntity<JsonResponse<PagedResponse<AccountResponse>>> getAccounts(
        @RequestBody CustomPageRequest<FilterAccount> request) {
    checkSuperAdmin();  // guard: chỉ SUPERADMIN
    return ResponseEntity.ok(JsonResponse.ok(accountService.getAccounts(request)));
}
```

---

### 3.3 Các file đã chỉnh sửa

#### `repository/AccountRepository.java`
Thêm phương thức tìm kiếm có lọc và phân trang:

```java
@Query("SELECT a FROM Account a " +
       "WHERE (:keyword IS NULL OR a.username LIKE %:keyword%) " +
       "AND (:type IS NULL OR a.type.name = :type) " +
       "AND (:isDeleted IS NULL OR a.isDeleted = :isDeleted) " +
       "ORDER BY a.createdAt DESC")
Page<Account> searchAccounts(
    @Param("keyword") String keyword,
    @Param("type") String type,
    @Param("isDeleted") Boolean isDeleted,
    Pageable pageable
);
```

> Tất cả tham số đều nullable → khi truyền `null` thì bỏ qua điều kiện lọc đó.

---

#### `service/AccountService.java`
Thêm các phương thức xử lý nghiệp vụ:

| Phương thức | Mô tả |
|-------------|-------|
| `getAccounts(request)` | Gọi repository tìm kiếm, map sang `AccountResponse`, bọc trong `PagedResponse` |
| `getAccountResponse(id)` | Tìm theo id, ném `BadException` nếu không tồn tại |
| `resetPassword(accountId, newPassword)` | Hash mật khẩu bằng BCrypt; nếu `newPassword` null → dùng `username` |
| `lockAccount(accountId)` | Set `isDeleted = true`; chặn không cho tự khóa chính mình |
| `unlockAccount(accountId)` | Set `isDeleted = false`; tìm bằng `findByIdAndIsDeleted(id, true)` |
| `toAccountResponse(account)` *(private)* | Chuyển entity `Account` → DTO `AccountResponse` |
| `resolveLinkedName(username)` *(private)* | Truy vấn Lecturer/Staff/Student để lấy tên thật |
| `resolveLinkedRole(username)` *(private)* | Xác định vai trò liên kết: Giảng viên / Cán bộ / Sinh viên / Admin |

**Logic đặt lại mật khẩu:**
```java
public void resetPassword(int accountId, String newPassword) {
    Account account = accountRepository.findById(accountId)...;
    String raw = (newPassword != null && !newPassword.isBlank())
                 ? newPassword
                 : account.getUsername();  // mặc định = username
    account.setPassword(passwordEncoder.encode(raw));
    accountRepository.save(account);
}
```

**Logic khóa tài khoản (có bảo vệ tự khóa):**
```java
public void lockAccount(int accountId) {
    String currentUsername = SecurityUtils.getCurrentUsername();
    Account account = accountRepository.findById(accountId)...;
    if (account.getUsername().equals(currentUsername)) {
        throw new BadException(ErrorCode.ACCESS_DENIED);  // không cho tự khóa
    }
    account.setDeleted(true);
    accountRepository.save(account);
}
```

---

## 4. Frontend — Angular

### 4.1 Kiến trúc

```
AccountListComponent
    ↕ (HTTP calls)
AccountManagementService
    ↕ (REST API)
Backend /accounts/*
```

---

### 4.2 Các file mới tạo

#### `dto/response/account-response.ts`
Interface TypeScript ánh xạ với `AccountResponse` từ backend:

```typescript
export interface AccountResponse {
    id: number;
    username: string;
    type: string;
    linkedName: string;
    linkedRole: string;
    isDeleted: boolean;
    createdAt: string;
    updatedAt: string;
}
```

---

#### `service/account-management.service.ts`
Service gọi các API từ backend:

```typescript
@Injectable({ providedIn: 'root' })
export class AccountManagementService {
    private baseUrl = environment.apiURL + 'accounts';

    searchAccounts(request: any)                    // POST /accounts/search
    getAccountById(id: number)                      // GET  /accounts/{id}
    resetPassword(accountId, newPassword?)          // PUT  /accounts/reset-password
    lockAccount(id: number)                         // PUT  /accounts/{id}/lock
    unlockAccount(id: number)                       // PUT  /accounts/{id}/unlock
}
```

---

#### `components/page/account/account-list/account-list.component.ts`
Component chính xử lý logic giao diện:

- **Biến trạng thái:** `accounts[]`, `totalRecords`, `page`, `rows`, `searchKeyword`, `selectedType`, `selectedStatus`, `currentUsername`
- **Khởi tạo:** Subscribe `authService.userName$` để lấy username đang đăng nhập (dùng ẩn nút khóa)
- **`loadAccounts()`:** Gọi service với filter + phân trang
- **`openResetModal()`:** Mở modal nhập mật khẩu mới
- **`confirmResetPassword()`:** Gọi API reset, hiển thị toast thành công/lỗi
- **`openConfirmModal()`:** Mở modal xác nhận lock/unlock
- **`confirmAction()`:** Gọi API lock hoặc unlock tương ứng, reload danh sách

---

#### `components/page/account/account-list/account-list.component.html`
Giao diện bao gồm:

1. **Thanh tìm kiếm + bộ lọc:** Input tìm theo username, dropdown lọc loại tài khoản, dropdown lọc trạng thái, nút xóa bộ lọc
2. **Bảng dữ liệu:** Hiển thị ID, username, loại (badge màu), tên liên kết, vai trò, trạng thái (badge), ngày tạo, thao tác
3. **Nút thao tác trên mỗi hàng:**
   - 🔑 Reset mật khẩu (luôn hiển thị)
   - 🔒 Khóa (chỉ hiển thị khi `!acc.isDeleted && acc.username !== currentUsername`)
   - 🔓 Mở khóa (chỉ hiển thị khi `acc.isDeleted`)
4. **Modal reset mật khẩu:** Nhập mật khẩu mới, để trống → reset về username
5. **Modal xác nhận lock/unlock:** Hiển thị tên tài khoản cần thao tác

---

#### `components/page/account/account-list/account-list.component.css`
CSS tối giản: cố định header bảng khi cuộn (`sticky-top`).

---

### 4.3 Các file đã chỉnh sửa

#### `app.module.ts`
Đăng ký `AccountListComponent` vào mảng `declarations` để Angular nhận diện component.

```typescript
import { AccountListComponent } from './components/page/account/account-list/account-list.component';

declarations: [
    // ... các component khác ...
    AccountListComponent,
]
```

---

#### `app-routing.module.ts`
Thêm route mới:

```typescript
import { AccountListComponent } from './components/page/account/account-list/account-list.component';

{ path: 'account-management', component: AccountListComponent }
```

---

#### `components/layout/layout-sidebar/layout-sidebar.component.html`
Thêm mục menu trong sidebar, **chỉ hiển thị khi `isAdminRole()` = SUPERADMIN**:

```html
<li class="nav-item hover-effect" *ngIf="isAdminRole()">
    <a class="nav-link" [routerLink]="'/account-management'" ...>
        <i class="me-2 bi bi-shield-lock-fill"></i>
        {{'ACCOUNT_MANAGEMENT' | translate}}
    </a>
</li>
```

---

#### `assets/i18n/vi.json` + `en.json` + `cn.json` + `lao.json`
Thêm **22 translation key** mới cho 4 ngôn ngữ:

| Key | Tiếng Việt | English | 中文 | ລາວ |
|-----|-----------|---------|------|-----|
| `ACCOUNT_MANAGEMENT` | Quản lý tài khoản | Account Management | 账户管理 | ການຈັດການບັນຊີ |
| `USERNAME` | Tên đăng nhập | Username | 用户名 | ຊື່ຜູ້ໃຊ້ |
| `LINKED_NAME` | Tên liên kết | Linked Name | 关联姓名 | ຊື່ເຊື່ອມໂຍງ |
| `LINKED_ROLE` | Vai trò | Role | 角色 | ບົດບາດ |
| `RESET_PASSWORD` | Đặt lại mật khẩu | Reset Password | 重置密码 | ຕັ້ງລະຫັດຜ່ານໃໝ່ |
| `LOCK_ACCOUNT` | Khóa tài khoản | Lock Account | 锁定账户 | ລັອກບັນຊີ |
| `UNLOCK_ACCOUNT` | Mở khóa tài khoản | Unlock Account | 解锁账户 | ປິດລັອກບັນຊີ |
| `ACCOUNT_ACTIVE` | Hoạt động | Active | 正常 | ໃຊ້ງານໄດ້ |
| `ACCOUNT_LOCKED` | Đã khóa | Locked | 已锁定 | ຖືກລັອກ |
| `CANCEL` | Hủy | Cancel | 取消 | ຍົກເລີກ |
| `CONFIRM` | Xác nhận | Confirm | 确认 | ຢືນຢັນ |
| `CLEAR_FILTER` | Xóa lọc | Clear Filter | 清除筛选 | ລ້າງການກັ່ນຕອງ |
| `NO_DATA` | Không có dữ liệu | No data found | 暂无数据 | ບໍ່ມີຂໍ້ມູນ |
| `ALL` | Tất cả | All | 全部 | ທັງໝົດ |
| *(+ 8 key khác)* | ... | ... | ... | ... |

---

## 5. Bảo mật

| Lớp | Cơ chế | Chi tiết |
|-----|--------|----------|
| **Backend** | Type-based guard | `SecurityUtils.getCurrentUserType()` phải là `"SUPERADMIN"` |
| **Backend** | Tự khóa bản thân | So sánh `currentUsername` với `account.getUsername()`, ném `ACCESS_DENIED` nếu trùng |
| **Frontend** | Route ẩn | Mục sidebar chỉ render khi `isAdminRole()` trả về `true` |
| **Frontend** | Nút ẩn | Nút khóa ẩn khi `acc.username === currentUsername` |

> **Lưu ý:** Lớp bảo vệ backend là quan trọng nhất — dù ai đó cố gọi API trực tiếp cũng bị từ chối.

---

## 6. Luồng hoạt động tổng thể

```
[SUPERADMIN đăng nhập]
        ↓
[Sidebar hiển thị menu "Quản lý tài khoản"]
        ↓
[Truy cập /account-management]
        ↓
[AccountListComponent gọi POST /accounts/search]
        ↓
[Backend trả về danh sách AccountResponse có phân trang]
        ↓
[Hiển thị bảng tài khoản]
        ↓
    ┌───────────────────────────────────────┐
    │  Thao tác tuỳ chọn:                  │
    │  🔑 Reset → Modal → PUT /reset-pass  │
    │  🔒 Lock  → Modal → PUT /{id}/lock   │
    │  🔓 Unlock→ Modal → PUT /{id}/unlock │
    └───────────────────────────────────────┘
        ↓
[Hiển thị toast thành công / lỗi]
        ↓
[Reload danh sách]
```

---

## 7. Tổng kết file thay đổi

### Backend (7 file)
| File | Loại | Mô tả |
|------|------|-------|
| `dto/response/AccountResponse.java` | **Mới** | DTO response |
| `dto/request/ResetPasswordRequest.java` | **Mới** | DTO request reset password |
| `dto/request/FilterAccount.java` | **Mới** | DTO filter tìm kiếm |
| `controller/AccountController.java` | **Mới** | 5 REST endpoints |
| `repository/AccountRepository.java` | **Sửa** | Thêm `searchAccounts()` query |
| `service/AccountService.java` | **Sửa** | Thêm 5 methods + 2 private helpers |

### Frontend (10 file)
| File | Loại | Mô tả |
|------|------|-------|
| `dto/response/account-response.ts` | **Mới** | Interface TypeScript |
| `service/account-management.service.ts` | **Mới** | HTTP service 5 methods |
| `page/account/account-list/*.component.ts` | **Mới** | Component logic |
| `page/account/account-list/*.component.html` | **Mới** | Template giao diện |
| `page/account/account-list/*.component.css` | **Mới** | Style |
| `app.module.ts` | **Sửa** | Đăng ký component |
| `app-routing.module.ts` | **Sửa** | Thêm route `/account-management` |
| `layout-sidebar.component.html` | **Sửa** | Thêm mục menu |
| `assets/i18n/vi.json` | **Sửa** | Thêm 22 key Tiếng Việt |
| `assets/i18n/en.json` | **Sửa** | Thêm 22 key English |
| `assets/i18n/cn.json` | **Sửa** | Thêm 22 key Tiếng Trung |
| `assets/i18n/lao.json` | **Sửa** | Thêm 22 key Tiếng Lào |
