import { Component, OnInit } from '@angular/core';
import { NotificationResponse } from '../../../dto/response/notification-response';
import { NotiService } from '../../../service/noti.service';
import { WebSocketService } from '../../../service/websocket.service';

@Component({
  selector: 'app-notification',
  templateUrl: './notification.component.html',
  styleUrl: './notification.component.css'
})
export class NotificationComponent implements OnInit {
  isLoading: boolean = false;
  currentPage: number = 0;
  pageSize: number = 20;
  isLastPage: boolean = false;
  notifications: NotificationResponse[] = [];
  selectedFilter: 'all' | 'unread' = 'all';

  constructor(
    private notiService: NotiService,
    private websocketService: WebSocketService
  ) {}

  ngOnInit(): void {
    this.loadNotifications(true);
    this.websocketService.refreshNotiCount();
  }

  loadNotifications(reset: boolean): void {
    this.isLoading = true;

    if (reset) {
      this.currentPage = 0;
      this.notifications = [];
    }

    const request = {
      page: this.currentPage,
      size: this.pageSize,
      sortBy: 'createdAt',
      direction: 'desc'
    };

    this.notiService.getNotification(request).subscribe({
      next: (response) => {
        if (response.code === 200) {
          const newItems = response.result.content;
          this.notifications = reset ? newItems : [...this.notifications, ...newItems];
          this.isLastPage = response.result.last;

          if (!this.isLastPage) {
            this.currentPage++;
          }
        }
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  loadMore(): void {
    if (this.isLoading || this.isLastPage) {
      return;
    }
    this.loadNotifications(false);
  }

  setFilter(filter: 'all' | 'unread'): void {
    this.selectedFilter = filter;
  }

  get filteredNotifications(): NotificationResponse[] {
    if (this.selectedFilter === 'unread') {
      return this.notifications.filter((item) => !item.read);
    }
    return this.notifications;
  }

  markAllAsRead(): void {
    this.notiService.setReadAll().subscribe({
      next: (response) => {
        if (response.code === 200) {
          this.notifications = this.notifications.map((item) => ({ ...item, read: true }));
          this.websocketService.refreshNotiCount();
        }
      }
    });
  }

  markOneAsRead(notification: NotificationResponse): void {
    if (notification.read) {
      return;
    }

    this.notiService.setReadOne(notification.recipientId).subscribe({
      next: (response) => {
        if (response.code === 200) {
          notification.read = true;
          this.websocketService.refreshNotiCount();
        }
      }
    });
  }

  get isUnreadFilter(): boolean {
    return this.selectedFilter === 'unread';
  }

}
