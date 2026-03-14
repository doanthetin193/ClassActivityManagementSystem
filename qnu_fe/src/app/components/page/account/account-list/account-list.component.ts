import { Component, OnInit } from '@angular/core';
import { AccountManagementService } from '../../../../service/account-management.service';
import { AccountResponse } from '../../../../dto/response/account-response';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../../../../service/auth.service';

declare var bootstrap: any;

@Component({
  selector: 'app-account-list',
  templateUrl: './account-list.component.html',
  styleUrl: './account-list.component.css'
})
export class AccountListComponent implements OnInit {
  accounts: AccountResponse[] = [];
  totalRecords: number = 0;
  rows: number = 10;
  first: number = 0;
  page: number = 0;

  // filter
  searchKeyword: string = '';
  selectedType: string = '';
  selectedStatus: string = '';

  typeOptions = [
    { label: 'ALL', value: '' },
    { label: 'SUPERADMIN', value: 'SUPERADMIN' },
    { label: 'DEPARTMENT', value: 'DEPARTMENT' },
    { label: 'STUDENT', value: 'STUDENT' },
  ];

  statusOptions = [
    { label: 'ALL', value: '' },
    { label: 'ACCOUNT_ACTIVE', value: 'active' },
    { label: 'ACCOUNT_LOCKED', value: 'locked' },
  ];

  currentUsername: string = '';

  // reset password modal
  selectedAccount: AccountResponse | null = null;
  newPassword: string = '';
  private resetModalInstance: any;

  // lock/unlock confirm
  actionTarget: AccountResponse | null = null;
  actionType: 'lock' | 'unlock' = 'lock';
  private confirmModalInstance: any;

  constructor(
    private accountService: AccountManagementService,
    private toastr: ToastrService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.authService.userName$.subscribe(name => this.currentUsername = name);
    this.loadAccounts();
  }

  loadAccounts(event: any = { page: 0, rows: this.rows }): void {
    const isDeletedVal: boolean | null =
      this.selectedStatus === 'locked' ? true :
      this.selectedStatus === 'active' ? false : null;

    this.page = event.page;

    this.accountService.searchAccounts({
      page: event.page,
      size: event.rows,
      filter: {
        keyWord: this.searchKeyword || null,
        type: this.selectedType || null,
        isDeleted: isDeletedVal
      }
    }).subscribe(res => {
      if (res.code === 200) {
        this.accounts = res.result.content;
        this.totalRecords = res.result.totalItems;
      }
    });
  }

  onPageChange(event: any): void {
    this.first = event.first;
    this.rows = event.rows;
    this.loadAccounts({ page: event.page, rows: event.rows });
  }

  onSearch(value: string): void {
    this.searchKeyword = value;
    this.loadAccounts({ page: 0, rows: this.rows });
  }

  applyFilter(): void {
    this.loadAccounts({ page: 0, rows: this.rows });
  }

  clearFilter(): void {
    this.selectedType = '';
    this.selectedStatus = '';
    this.searchKeyword = '';
    this.loadAccounts({ page: 0, rows: this.rows });
  }

  // ─── Reset password ───────────────────────────────────────────
  openResetModal(account: AccountResponse): void {
    this.selectedAccount = account;
    this.newPassword = '';
    const el = document.getElementById('resetPasswordModal');
    this.resetModalInstance = new bootstrap.Modal(el!);
    this.resetModalInstance.show();
  }

  confirmResetPassword(): void {
    if (!this.selectedAccount) return;
    const pwd = this.newPassword.trim() || undefined;
    this.accountService.resetPassword(this.selectedAccount.id, pwd).subscribe(res => {
      if (res.code === 200) {
        this.toastr.success('Đặt lại mật khẩu thành công');
        this.resetModalInstance?.hide();
      } else {
        this.toastr.error(res.message || 'Lỗi đặt lại mật khẩu');
      }
    });
  }

  // ─── Lock / Unlock ────────────────────────────────────────────
  openConfirmModal(account: AccountResponse, type: 'lock' | 'unlock'): void {
    this.actionTarget = account;
    this.actionType = type;
    const el = document.getElementById('confirmActionModal');
    this.confirmModalInstance = new bootstrap.Modal(el!);
    this.confirmModalInstance.show();
  }

  confirmAction(): void {
    if (!this.actionTarget) return;
    const obs = this.actionType === 'lock'
      ? this.accountService.lockAccount(this.actionTarget.id)
      : this.accountService.unlockAccount(this.actionTarget.id);

    obs.subscribe(res => {
      if (res.code === 200) {
        const msg = this.actionType === 'lock' ? 'Khóa tài khoản thành công' : 'Mở khóa tài khoản thành công';
        this.toastr.success(msg);
        this.confirmModalInstance?.hide();
        this.loadAccounts({ page: this.page, rows: this.rows });
      } else {
        this.toastr.error(res.message || 'Có lỗi xảy ra');
      }
    });
  }

}
