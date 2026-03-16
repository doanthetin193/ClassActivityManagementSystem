import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SidebarService {
  private _open = new BehaviorSubject<boolean>(true);
  open$ = this._open.asObservable();

  toggle(): void {
    this._open.next(!this._open.value);
  }

  setOpen(value: boolean): void {
    this._open.next(value);
  }

  get isOpen(): boolean {
    return this._open.value;
  }
}
