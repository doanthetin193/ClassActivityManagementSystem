package com.manager.class_activity.qnu.service;

import com.manager.class_activity.qnu.dto.request.FilterAccount;
import com.manager.class_activity.qnu.dto.response.AccountResponse;
import com.manager.class_activity.qnu.dto.response.PagedResponse;
import com.manager.class_activity.qnu.entity.*;
import com.manager.class_activity.qnu.entity.Class;
import com.manager.class_activity.qnu.exception.BadException;
import com.manager.class_activity.qnu.exception.ErrorCode;
import com.manager.class_activity.qnu.helper.CustomPageRequest;
import com.manager.class_activity.qnu.repository.AcademicAdvisorRepository;
import com.manager.class_activity.qnu.repository.AccountRepository;
import com.manager.class_activity.qnu.until.AcademicYearUtil;
import com.manager.class_activity.qnu.until.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountService {
    AccountRepository accountRepository;
    AcademicAdvisorRepository academicAdvisorRepository;

    public void saveAccount(Account account) {
        if(accountRepository.existsByUsernameAndIsDeleted(account.getUsername(), false)) {
            throw new BadException(ErrorCode.USER_EXISTED);
        }
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        account.setPassword(passwordEncoder.encode(account.getPassword()));
        accountRepository.save(account);
    }

    public void deleteAccount(int id) {
        Account account = getAccount(id);
        account.setDeleted(true);
        accountRepository.save(account);
    }
    public Account getAccount(int id) {
        return accountRepository.findByIdAndIsDeleted(id,false)
                .orElseThrow(()->new BadException(ErrorCode.USER_NOT_EXISTED));
    }
    public Account getAccount(String username) {
        return accountRepository.findByUsernameAndIsDeleted(username,false).orElseThrow(
                ()->new BadException(ErrorCode.USER_NOT_EXISTED)
        );
    }

    public Class getClassOfAccount(){
        String username = SecurityUtils.getCurrentUsername();
        Student student = accountRepository.getStudentByUsername(username);
        if(ObjectUtils.isNotEmpty(student)){
            return student.getClazz();
        }
        return null;
    }

    public List<Class> getMyClassOfAccountLecturer(){
        String username = SecurityUtils.getCurrentUsername();
        Lecturer lecturer = accountRepository.getLecturerByUsername(username);
        if(ObjectUtils.isNotEmpty(lecturer)){
            List<Class> classes = new ArrayList<>();
            List<AcademicAdvisor> advisors = academicAdvisorRepository.findByAcademicYearAndLecturerAndIsDeletedOrderByUpdatedAt(AcademicYearUtil.getCurrentAcademicYear(),lecturer,false);
            for (AcademicAdvisor item: advisors) {
                classes.add(item.getClazz());
            }
            return classes;
        }
        return null;
    }

    public Department getDepartmentOfAccount() {
        String username = SecurityUtils.getCurrentUsername();
        Lecturer lecturer = accountRepository.getLecturerByUsername(username);
        Staff staff;
        Student student;
        Department department = null;
        if(lecturer == null) {
            staff = accountRepository.getStaffByUsername(username);
            if(staff == null) {
                student = accountRepository.getStudentByUsername(username);
                if(student == null) {
                    return null;
                }
                department = student.getClazz().getDepartment();
                return department;
            }
            department = staff.getDepartment();
            return department;
        }
        department = lecturer.getDepartment();
        return department;
    }


    public String getNameOfAccount(String username) {
        Account account = accountRepository.findByUsernameAndIsDeleted(username,false).orElseThrow(
                ()->new BadException(ErrorCode.USER_NOT_EXISTED)
        );
        if(ObjectUtils.isNotEmpty(accountRepository.getLecturerByUsername(account.getUsername()))){
            return accountRepository.getLecturerByUsername(account.getUsername()).getName();
        }
        if(ObjectUtils.isNotEmpty(accountRepository.getStaffByUsername(account.getUsername()))){
            return accountRepository.getStaffByUsername(account.getUsername()).getName();
        }
        if(ObjectUtils.isNotEmpty(accountRepository.getStudentByUsername(account.getUsername()))){
            return accountRepository.getStudentByUsername(account.getUsername()).getName();
        }
        return "Admin";
    }

    // ===== Account Management =====

    public PagedResponse<AccountResponse> getAccounts(CustomPageRequest<FilterAccount> request) {
        FilterAccount filter = request.getFilter();
        String keyword = filter != null ? filter.getKeyWord() : null;
        String type = filter != null ? filter.getType() : null;
        Boolean isDeleted = filter != null ? filter.getIsDeleted() : null;

        Page<Account> page = accountRepository.searchAccounts(keyword, type, isDeleted, request.toPageable());

        List<AccountResponse> responses = page.getContent().stream()
                .map(this::toAccountResponse)
                .toList();

        return new PagedResponse<>(responses, page.getNumber(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    public AccountResponse getAccountResponse(int id) {
        Account account = getAccount(id);
        return toAccountResponse(account);
    }

    public void resetPassword(int accountId, String newPassword) {
        Account account = getAccount(accountId);
        PasswordEncoder encoder = new BCryptPasswordEncoder(10);
        // Nếu không truyền password mới thì reset về mặc định = username
        String passwordToSet = (newPassword != null && !newPassword.isBlank()) ? newPassword : account.getUsername();
        account.setPassword(encoder.encode(passwordToSet));
        accountRepository.save(account);
    }

    public void lockAccount(int accountId) {
        Account account = getAccount(accountId);
        // Không cho phép khoá chính tài khoản đang đăng nhập
        String currentUsername = SecurityUtils.getCurrentUsername();
        if (account.getUsername().equals(currentUsername)) {
            throw new BadException(ErrorCode.ACCESS_DENIED);
        }
        account.setDeleted(true);
        accountRepository.save(account);
    }

    public void unlockAccount(int accountId) {
        Account account = accountRepository.findByIdAndIsDeleted(accountId, true)
                .orElseThrow(() -> new BadException(ErrorCode.USER_NOT_EXISTED));
        account.setDeleted(false);
        accountRepository.save(account);
    }

    private AccountResponse toAccountResponse(Account account) {
        String linkedName = resolveLinkedName(account.getUsername());
        String linkedRole = resolveLinkedRole(account.getUsername());
        return AccountResponse.builder()
                .id(account.getId())
                .username(account.getUsername())
                .type(account.getType() != null ? account.getType().getName() : null)
                .linkedName(linkedName)
                .linkedRole(linkedRole)
                .isDeleted(account.isDeleted())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private String resolveLinkedName(String username) {
        Lecturer lecturer = accountRepository.getLecturerByUsername(username);
        if (ObjectUtils.isNotEmpty(lecturer)) return lecturer.getName();
        Staff staff = accountRepository.getStaffByUsername(username);
        if (ObjectUtils.isNotEmpty(staff)) return staff.getName();
        Student student = accountRepository.getStudentByUsername(username);
        if (ObjectUtils.isNotEmpty(student)) return student.getName();
        return "Admin";
    }

    private String resolveLinkedRole(String username) {
        if (ObjectUtils.isNotEmpty(accountRepository.getLecturerByUsername(username))) return "Giảng viên";
        if (ObjectUtils.isNotEmpty(accountRepository.getStaffByUsername(username))) return "Nhân viên";
        if (ObjectUtils.isNotEmpty(accountRepository.getStudentByUsername(username))) return "Sinh viên";
        return "Admin";
    }

}
