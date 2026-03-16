import { Component, OnInit, HostListener, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../service/auth.service';
import { SidebarService } from '../../../service/sidebar.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-layout-sidebar',
  templateUrl: './layout-sidebar.component.html',
  styleUrls: ['./layout-sidebar.component.css'],
})
export class LayoutSidebarComponent implements OnInit, OnDestroy {
  username: string = '';
  isSidebarVisible = true;
  isMobile = false;
  private sub!: Subscription;

  constructor(
    private router: Router,
    public authService: AuthService,
    private sidebarService: SidebarService
  ) {}

  ngOnInit(): void {
    this.authService.userName$.subscribe((name) => {
      this.username = name;
    });
    this.checkMobile();

    // Đồng bộ trạng thái từ SidebarService (được toggle từ header)
    this.sub = this.sidebarService.open$.subscribe((open) => {
      this.isSidebarVisible = open;
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  @HostListener('window:resize')
  checkMobile(): void {
    this.isMobile = window.innerWidth < 768;
    if (this.isMobile) {
      this.sidebarService.setOpen(false);
    } else {
      this.sidebarService.setOpen(true);
    }
  }

  onNavClick(): void {
    if (this.isMobile) {
      this.sidebarService.setOpen(false);
    }
  }

  closeSidebar(): void {
    this.sidebarService.setOpen(false);
  }

  navigate(link: string): void {
    this.router.navigate([link]);
    if (this.isMobile) this.sidebarService.setOpen(false);
  }

  isActive(url: string): boolean {
    return this.router.url.startsWith(url);
  }

  isAdminRole(): boolean {
    return this.authService.getRole() === 'SUPERADMIN';
  }

  isRoleStudent(): boolean {
    return this.authService.getRole() === 'STUDENT';
  }
}
