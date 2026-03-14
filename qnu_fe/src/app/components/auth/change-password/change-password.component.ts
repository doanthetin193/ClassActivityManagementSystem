import { Component } from '@angular/core';
import { AuthService } from '../../../service/auth.service';
import { ToastrService } from 'ngx-toastr';
import { Router } from '@angular/router';

@Component({
  selector: 'app-change-password',
  templateUrl: './change-password.component.html',
  styleUrl: './change-password.component.css',
})
export class ChangePasswordComponent {
  username: string = '';
  password: string = '';
  newPassword: string = '';
  confirmPassword: string = '';
  errorMessage: string = '';
  passwordsMatch: boolean = true;

  constructor(
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService
  ) {}

  onSubmit(): void {
    this.authService.changePassword({username: this.username, oldPassword: this.password, newPassword: this.newPassword}).subscribe(
      (response) => {
        this.toastr.success('Change password succes', 'Success');
        this.router.navigate(['/']);
      },
      (error) => {
        this.toastr.error('Invalid username or password', 'Error');
      }
    );
  }

  checkPasswordsMatch(): void {
    this.passwordsMatch = this.newPassword === this.confirmPassword;
  }
}
