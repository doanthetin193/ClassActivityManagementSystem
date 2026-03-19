import { Component, OnDestroy, OnInit } from '@angular/core';
import { AuthService } from '../../../service/auth.service';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { HttpErrorResponse } from '@angular/common/http';
import { Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { environment } from '../../../../environments/environment';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit, OnDestroy {
  username: string = '';
  password: string = '';
  error: boolean = false;
  errorMessage: string = '';
  showForgotPasswordGuide: boolean = false;
  isLoginLocked: boolean = false;
  lockRemainingSeconds: number = 0;

  private lockTimer: ReturnType<typeof setInterval> | null = null;
  private readonly loginLockStorageKey = 'qnu-login-lock-until';
  private readonly devResetEndpoint = 'dev/rate-limit/reset';

  constructor(
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService,
    private translate: TranslateService,
    @Inject(PLATFORM_ID) private platformId: Object,
  ) {}

  ngOnInit(): void {
    this.restoreLockState();

    this.authService.isLoggedIn$.subscribe((isLoggedIn) => {
      if (isLoggedIn) {
        this.router.navigate(['/']); // Chuyển hướng nếu đã đăng nhập
      }
    });
  }

  ngOnDestroy(): void {
    this.stopLockTimer();
  }
  
  onSubmit(): void {
    if (this.isLoginLocked) {
      this.error = true;
      return;
    }

    this.authService.login(this.username, this.password).subscribe(
      (response) => {
        this.error = false;
        this.errorMessage = '';
        this.clearLockState();
        this.router.navigate(['/']); // Chuyển hướng sau khi đăng nhập thành công
      },
      (error: HttpErrorResponse) => {
        this.error = true;

        if (error.status === 429) {
          const retryAfter = this.extractRetryAfterSeconds(error);
          this.startLoginLock(retryAfter);
          this.toastr.warning(`Quá nhiều lần đăng nhập. Thử lại sau ${this.formatSeconds(this.lockRemainingSeconds)}.`, 'Tạm khóa đăng nhập');
          return;
        }

        const remainingAttempts = this.extractRemainingAttempts(error);
        if (remainingAttempts !== null) {
          this.errorMessage = `Sai tài khoản hoặc mật khẩu. Còn ${remainingAttempts} lần thử trước khi bị khóa.`;
          this.toastr.warning(this.errorMessage, 'Lỗi đăng nhập');
          return;
        }

        this.toastr.error('Sai tài khoản hoặc mật khẩu', 'Lỗi đăng nhập');
        this.errorMessage = 'Sai tài khoản hoặc mật khẩu';
      }
    );
  }

  onForgotPassword(event: Event): void {
    event.preventDefault();
    this.showForgotPasswordGuide = !this.showForgotPasswordGuide;
  }

  get forgotPasswordGuideTitle(): string {
    const key = 'FORGOT_PASSWORD_NOTICE_TITLE';
    const translated = this.translate.instant(key);
    return translated && translated !== key ? translated : 'Quên mật khẩu?';
  }

  get forgotPasswordGuideMessage(): string {
    const key = 'FORGOT_PASSWORD_NOTICE_MESSAGE';
    const translated = this.translate.instant(key);
    return translated && translated !== key
      ? translated
      : 'Vui lòng liên hệ lớp trưởng, cố vấn học tập hoặc văn phòng khoa để được cấp lại mật khẩu.';
  }

  private extractRetryAfterSeconds(error: HttpErrorResponse): number {
    const retryAfterHeader = error.headers?.get('Retry-After');
    const retryAfter = retryAfterHeader ? Number.parseInt(retryAfterHeader, 10) : NaN;
    if (Number.isFinite(retryAfter) && retryAfter > 0) {
      return retryAfter;
    }
    return 900;
  }

  private extractRemainingAttempts(error: HttpErrorResponse): number | null {
    const remainingHeader = error.headers?.get('X-Login-Attempts-Remaining');
    const remaining = remainingHeader ? Number.parseInt(remainingHeader, 10) : NaN;
    if (Number.isFinite(remaining) && remaining >= 0) {
      return remaining;
    }
    return null;
  }

  private startLoginLock(seconds: number): void {
    this.isLoginLocked = true;
    this.lockRemainingSeconds = Math.max(1, seconds);

    const lockUntil = Date.now() + this.lockRemainingSeconds * 1000;
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(this.loginLockStorageKey, String(lockUntil));
    }

    this.stopLockTimer();
    this.lockTimer = setInterval(() => {
      this.lockRemainingSeconds = Math.max(0, this.lockRemainingSeconds - 1);
      if (this.lockRemainingSeconds === 0) {
        this.clearLockState();
      }
    }, 1000);
  }

  private restoreLockState(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const raw = localStorage.getItem(this.loginLockStorageKey);
    if (!raw) {
      return;
    }

    const lockUntil = Number.parseInt(raw, 10);
    if (!Number.isFinite(lockUntil)) {
      this.clearLockState();
      return;
    }

    const remaining = Math.ceil((lockUntil - Date.now()) / 1000);
    if (remaining <= 0) {
      this.clearLockState();
      return;
    }

    this.startLoginLock(remaining);
  }

  private clearLockState(): void {
    this.isLoginLocked = false;
    this.lockRemainingSeconds = 0;
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(this.loginLockStorageKey);
    }
    this.stopLockTimer();
  }

  private stopLockTimer(): void {
    if (this.lockTimer) {
      clearInterval(this.lockTimer);
      this.lockTimer = null;
    }
  }

  formatSeconds(totalSeconds: number): string {
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    if (minutes <= 0) {
      return `${seconds}s`;
    }
    return `${minutes}m ${seconds.toString().padStart(2, '0')}s`;
  }

  get showDevResetButton(): boolean {
    return environment.apiURL.includes('localhost') || environment.apiURL.includes('127.0.0.1');
  }

  onResetLockDev(): void {
    this.authService.resetRateLimitDev(this.devResetEndpoint).subscribe({
      next: () => {
        this.clearLockState();
        this.error = false;
        this.errorMessage = '';
        this.toastr.success('Đã reset lock đăng nhập (dev).', 'Thành công');
      },
      error: () => {
        this.toastr.error('Không reset được lock. Kiểm tra backend profile dev.', 'Lỗi');
      }
    });
  }

}
