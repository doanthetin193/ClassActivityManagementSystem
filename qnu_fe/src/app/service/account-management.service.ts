import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { JsonResponse } from '../dto/response/json-response';
import { PagedResponse } from '../dto/response/paged-response';
import { AccountResponse } from '../dto/response/account-response';

@Injectable({
  providedIn: 'root'
})
export class AccountManagementService {
  private baseUrl = environment.apiURL + 'accounts';

  constructor(private http: HttpClient) {}

  searchAccounts(request: any): Observable<JsonResponse<PagedResponse<AccountResponse>>> {
    return this.http.post<JsonResponse<PagedResponse<AccountResponse>>>(`${this.baseUrl}/search`, request);
  }

  getAccountById(id: number): Observable<JsonResponse<AccountResponse>> {
    return this.http.get<JsonResponse<AccountResponse>>(`${this.baseUrl}/${id}`);
  }

  resetPassword(accountId: number, newPassword?: string): Observable<JsonResponse<string>> {
    return this.http.put<JsonResponse<string>>(`${this.baseUrl}/reset-password`, {
      accountId,
      newPassword: newPassword || null
    });
  }

  lockAccount(id: number): Observable<JsonResponse<string>> {
    return this.http.put<JsonResponse<string>>(`${this.baseUrl}/${id}/lock`, {});
  }

  unlockAccount(id: number): Observable<JsonResponse<string>> {
    return this.http.put<JsonResponse<string>>(`${this.baseUrl}/${id}/unlock`, {});
  }
}
