import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { JsonResponse } from '../dto/response/json-response';
import { PagedResponse } from '../dto/response/paged-response';
import { NotificationResponse } from '../dto/response/notification-response';
import { environment } from '../../environments/environment';


@Injectable({
  providedIn: 'root'
})
export class NotiService {
  private baseUrl = environment.apiURL + 'notifications';

  constructor(private http: HttpClient) { }

    public getNotification(request: any): Observable<JsonResponse<PagedResponse<NotificationResponse>>> {
        return this.http.post<JsonResponse<PagedResponse<NotificationResponse>>>(`${this.baseUrl}/get-notifications`,request);
    }
    
    
  public setReadAll(): Observable<JsonResponse<string>>{
    return this.http.put<JsonResponse<string>>(`${this.baseUrl}/read-all`,null);
  }

  public setReadOne(recipientId: number): Observable<JsonResponse<string>> {
    return this.http.put<JsonResponse<string>>(`${this.baseUrl}/read/${recipientId}`, null);
  }
}
