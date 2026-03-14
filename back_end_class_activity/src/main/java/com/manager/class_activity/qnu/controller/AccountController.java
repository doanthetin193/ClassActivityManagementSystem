package com.manager.class_activity.qnu.controller;

import com.manager.class_activity.qnu.dto.request.FilterAccount;
import com.manager.class_activity.qnu.dto.request.ResetPasswordRequest;
import com.manager.class_activity.qnu.dto.response.AccountResponse;
import com.manager.class_activity.qnu.dto.response.JsonResponse;
import com.manager.class_activity.qnu.dto.response.PagedResponse;
import com.manager.class_activity.qnu.exception.BadException;
import com.manager.class_activity.qnu.exception.ErrorCode;
import com.manager.class_activity.qnu.helper.CustomPageRequest;
import com.manager.class_activity.qnu.service.AccountService;
import com.manager.class_activity.qnu.until.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountController {

    AccountService accountService;

    /**
     * Lấy danh sách tài khoản (có filter, phân trang)
     * Chỉ SUPERADMIN được dùng
     */
    @PostMapping("/search")
    public JsonResponse<PagedResponse<AccountResponse>> getAccounts(
            @RequestBody CustomPageRequest<FilterAccount> request) {
        checkSuperAdmin();
        return JsonResponse.success(accountService.getAccounts(request));
    }

    /**
     * Xem chi tiết 1 tài khoản theo id
     * Chỉ SUPERADMIN được dùng
     */
    @GetMapping("/{accountId}")
    public JsonResponse<AccountResponse> getAccountById(@PathVariable int accountId) {
        checkSuperAdmin();
        return JsonResponse.success(accountService.getAccountResponse(accountId));
    }

    /**
     * Reset mật khẩu cho tài khoản
     * - Nếu newPassword null/rỗng → reset về mật khẩu mặc định = username
     * - Chỉ SUPERADMIN được dùng
     */
    @PutMapping("/reset-password")
    public JsonResponse<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        checkSuperAdmin();
        accountService.resetPassword(request.getAccountId(), request.getNewPassword());
        return JsonResponse.success("Đặt lại mật khẩu thành công.");
    }

    /**
     * Khoá tài khoản (soft delete = isDeleted = true)
     * Chỉ SUPERADMIN được dùng
     */
    @PutMapping("/{accountId}/lock")
    public JsonResponse<String> lockAccount(@PathVariable int accountId) {
        checkSuperAdmin();
        accountService.lockAccount(accountId);
        return JsonResponse.success("Tài khoản đã bị khoá.");
    }

    /**
     * Mở khoá tài khoản (isDeleted = false)
     * Chỉ SUPERADMIN được dùng
     */
    @PutMapping("/{accountId}/unlock")
    public JsonResponse<String> unlockAccount(@PathVariable int accountId) {
        checkSuperAdmin();
        accountService.unlockAccount(accountId);
        return JsonResponse.success("Tài khoản đã được mở khoá.");
    }

    // Chỉ SUPERADMIN (type = "SUPERADMIN") mới được gọi các API này
    private void checkSuperAdmin() {
        if (!"SUPERADMIN".equals(SecurityUtils.getCurrentUserType())) {
            throw new BadException(ErrorCode.ACCESS_DENIED);
        }
    }
}
