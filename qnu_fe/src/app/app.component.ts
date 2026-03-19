import { Component, OnInit } from '@angular/core';
import { AuthService } from './service/auth.service';
import { WebSocketService } from './service/websocket.service';
import { TranslateService } from '@ngx-translate/core';
import { Title } from '@angular/platform-browser';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  private readonly routeTitleMap: Array<{ pattern: RegExp; key: string }> = [
    { pattern: /^\/?$/, key: 'HOME' },
    { pattern: /^\/login$/, key: 'LOGIN' },
    { pattern: /^\/change-password$/, key: 'CHANGE_PASSWORD' },
    { pattern: /^\/department(\/.*)?$/, key: 'DEPARTMENT' },
    { pattern: /^\/batch(\/.*)?$/, key: 'BATCH' },
    { pattern: /^\/class(\/.*)?$/, key: 'CLASS' },
    { pattern: /^\/my-class(\/.*)?$/, key: 'MY_CLASS' },
    { pattern: /^\/staff(\/.*)?$/, key: 'STAFF' },
    { pattern: /^\/lecturer(\/.*)?$/, key: 'LECTURER' },
    { pattern: /^\/students(\/.*)?$/, key: 'STUDENT' },
    { pattern: /^\/advisors(\/.*)?$/, key: 'ADVISOR' },
    { pattern: /^\/department-guide(\/.*)?$/, key: 'GUIDE' },
    { pattern: /^\/activity-guide(\/.*)?$/, key: 'GUIDE' },
    { pattern: /^\/activity-view(\/.*)?$/, key: 'ACTIVITY_VIEW' },
    { pattern: /^\/activity\/class-activity(\/.*)?$/, key: 'CLASS_ACTIVITY' },
    { pattern: /^\/activity\/attendance(\/.*)?$/, key: 'ATTENDANCE' },
    { pattern: /^\/activity\/minutes(\/.*)?$/, key: 'ACTIVITY_MINUTE' },
    { pattern: /^\/activity(\/.*)?$/, key: 'ACTIVITY' },
    { pattern: /^\/class-activity(\/.*)?$/, key: 'CLASS_ACTIVITY' },
    { pattern: /^\/role(\/.*)?$/, key: 'ROLE' },
    { pattern: /^\/account-management$/, key: 'ACCOUNT_MANAGEMENT' },
    { pattern: /^\/notifications$/, key: 'NOTIFICATIONS' }
  ];

  constructor(
    private authService: AuthService,
    private websocketService: WebSocketService,
    private translateService: TranslateService,
    private titleService: Title,
    private router: Router
  ) {
  }

  ngOnInit(): void {
    this.authService.checkToken();
    this.initLanguage();
    this.updateDocumentTitle(this.router.url);

    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => this.updateDocumentTitle(event.urlAfterRedirects));

    this.translateService.onLangChange.subscribe(() => {
      this.updateDocumentTitle(this.router.url);
    });
  }

  private initLanguage(): void {
    const savedLanguage = localStorage.getItem('language') || 'vi';
    this.translateService.setDefaultLang('vi');
    this.translateService.use(savedLanguage);
  }

  private updateDocumentTitle(url: string): void {
    const appTitle = this.translateWithFallback('APP_TITLE', 'QNU Class Activity Management');
    const pageTitle = this.resolvePageTitle(url);
    this.titleService.setTitle(`${pageTitle} | ${appTitle}`);
  }

  private resolvePageTitle(url: string): string {
    const normalizedPath = (url || '/').split('?')[0].split('#')[0] || '/';
    const matched = this.routeTitleMap.find((item) => item.pattern.test(normalizedPath));
    const titleKey = matched?.key || 'HOME';
    return this.translateWithFallback(titleKey, 'Trang chủ');
  }

  private translateWithFallback(key: string, fallback: string): string {
    const translated = this.translateService.instant(key);
    return translated && translated !== key ? translated : fallback;
  }
}
