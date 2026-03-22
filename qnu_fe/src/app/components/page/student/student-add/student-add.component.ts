import { Component, OnInit } from '@angular/core';
import { ClassResponse } from '../../../../dto/response/class-response';
import { StudentService } from '../../../../service/student.service';
import { ClassService } from '../../../../service/class.service';
import { ToastrService } from 'ngx-toastr';
import { ActivatedRoute, Router } from '@angular/router';
import { StudentRequest } from '../../../../dto/request/student-request';
import { Location } from '@angular/common';
import { BaseFilterComponent } from '../../../../core/BaseFilterComponent';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-student-add',
  templateUrl: './student-add.component.html',
  styleUrl: './student-add.component.css'
})
export class StudentAddComponent extends BaseFilterComponent implements OnInit {
  private readonly destroy$ = new Subject<void>();
  maxBirthDate: Date = new Date();
  isEditMode = false;
  studentId: number | null = null;

  studentCode: string = '';
  name: string = '';
  birthDate: Date | null = null;
  email: string = '';
  gender: string = '';
  selectedClass: ClassResponse | null = null;
  studentPosition: string = '';
  selectedClassId: number | null = null;

  classes: ClassResponse[] = [];
  filteredClasses: ClassResponse[] = [];
  genders: { label: string; value: string }[] = [];
  positions: { label: string; value: string }[] = [];

  constructor(
    private studentService: StudentService,
    private classService: ClassService,
    private toastr: ToastrService,
    private translateService: TranslateService,
    private route: ActivatedRoute,
    private router: Router,
    private location: Location
  ) {
    super();
  }

  ngOnInit(): void {
    this.maxBirthDate = this.getMaxBirthDate();
    this.setLocalizedOptions();
    this.translateService.onLangChange
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.setLocalizedOptions());

    this.loadClasses();
    this.route.params.subscribe((params) => {
      this.studentId = params['id'] ? Number(params['id']) : null;
      this.isEditMode = !!this.studentId;

      if (this.isEditMode) {
        this.loadStudentDetail(this.studentId!);
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setLocalizedOptions(): void {
    this.genders = [
      { label: this.translateService.instant('GENDER_MALE'), value: 'Male' },
      { label: this.translateService.instant('GENDER_FEMALE'), value: 'Female' },
    ];

    this.positions = [
      { label: this.translateService.instant('POSITION_LEADER'), value: 'ClassLeader' },
      { label: this.translateService.instant('POSITION_VICE_LEADER'), value: 'ViceLeader' },
      { label: this.translateService.instant('POSITION_SECRETARY'), value: 'Secretary' },
      { label: this.translateService.instant('POSITION_MEMBER'), value: 'Member' },
    ];
  }

  private getMaxBirthDate(): Date {
    const date = new Date();
    date.setFullYear(date.getFullYear() - 18);
    return date;
  }

  private isAdult(date: Date | null): boolean {
    if (!date) {
      return false;
    }
    const today = new Date();
    let age = today.getFullYear() - date.getFullYear();
    const monthDiff = today.getMonth() - date.getMonth();
    const dayDiff = today.getDate() - date.getDate();

    if (monthDiff < 0 || (monthDiff === 0 && dayDiff < 0)) {
      age--;
    }

    return age >= 18;
  }

  private getSelectedClassId(): number | null {
    if (!this.selectedClass || typeof this.selectedClass !== 'object') {
      return null;
    }

    return Object.prototype.hasOwnProperty.call(this.selectedClass, 'id')
      ? this.selectedClass.id
      : null;
  }

  private handleStudentSaveError(error: HttpErrorResponse): void {
    const errorCode = error?.error?.code;

    if (errorCode === 1001) {
      this.toastr.error(this.translateService.instant('STUDENT_CODE_ALREADY_EXISTS'));
      return;
    }

    if (errorCode === 1007) {
      this.toastr.error(this.translateService.instant('ACCESS_DENIED'));
      return;
    }

    const fallbackKey = this.isEditMode ? 'STUDENT_UPDATE_FAILED' : 'STUDENT_ADD_FAILED';
    this.toastr.error(this.translateService.instant(fallbackKey));
  }

  loadClasses(): void {
    this.classService.getClassSummary().subscribe((response) => {
      if (response.code === 200) {
        this.classes = response.result;
        if(this.selectedClassId !== null) {
          this.selectedClass = this.classes.find(
            (cls) => cls.id === this.selectedClassId
          ) ?? null;
      }
      }
    });
  }

  loadStudentDetail(studentId: number): void {
    this.studentService.getStudentById(studentId).subscribe((response) => {
      if (response.code === 200) {
        const student = response.result;
        this.studentCode = student.studentCode;
        this.name = student.name;
        this.birthDate = new Date(student.birthDate);
        this.email = student.email;
        this.gender = student.gender;
        this.selectedClassId = student.classId;
        this.selectedClass = this.classes.find(
          (cls) => cls.id === student.classId
        ) ?? null;
        this.studentPosition = student.studentPosition;
      }
    });
  }

  validateInput(): boolean {
    if (
      !this.studentCode ||
      !this.name ||
      !this.birthDate ||
      !this.email ||
      !this.gender ||
      !this.getSelectedClassId() ||
      !this.studentPosition
    ) {
      this.toastr.error(this.translateService.instant('PLEASE_FILL_ALL_FIELDS'));
      return false;
    }

    if (!this.isAdult(this.birthDate)) {
      this.toastr.error(this.translateService.instant('STUDENT_AGE_INVALID'));
      return false;
    }

    if (typeof this.selectedClass !== 'object') {
      this.toastr.error(this.translateService.instant('PLEASE_SELECT_VALID_CLASS'));
      return false;
    }

    return true;
  }

  addStudent(): void {
    if (!this.validateInput()) return;

    const studentRequest: StudentRequest = {
      studentCode: this.studentCode,
      name: this.name,
      birthDate: this.birthDate!,
      email: this.email,
      gender: this.gender,
      classId: this.getSelectedClassId()!.toString(),
      studentPositionEnum: this.studentPosition,
    };

    this.studentService.createStudent(studentRequest).subscribe({
      next: (response) => {
        if (response.code === 200) {
          this.toastr.success(this.translateService.instant('STUDENT_ADDED_SUCCESS'));
          this.goBack();
        } else {
          this.toastr.error(this.translateService.instant('STUDENT_ADD_FAILED'));
        }
      },
      error: (error: HttpErrorResponse) => this.handleStudentSaveError(error),
    });
  }

  updateStudent(): void {
    if (!this.validateInput()) return;

    const studentRequest: StudentRequest = {
      studentCode: this.studentCode,
      name: this.name,
      birthDate: this.birthDate!,
      email: this.email,
      gender: this.gender,
      classId: this.getSelectedClassId()!.toString(),
      studentPositionEnum: this.studentPosition,
    };

    this.studentService
      .updateStudent(this.studentId!, studentRequest)
      .subscribe({
        next: (response) => {
          if (response.code === 200) {
            this.toastr.success(this.translateService.instant('STUDENT_UPDATED_SUCCESS'));
            this.goBack();
          } else {
            this.toastr.error(this.translateService.instant('STUDENT_UPDATE_FAILED'));
          }
        },
        error: (error: HttpErrorResponse) => this.handleStudentSaveError(error),
      });
  }
  

  goBack(): void {
    this.location.back(); // Quay lại trang trước
  }

}
