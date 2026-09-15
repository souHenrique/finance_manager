import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiUrlService } from '../../../core/http/api-url.service';
import { Category, CreateCategoryRequest, UpdateCategoryRequest } from '../models/category.models';

@Injectable({ providedIn: 'root' })
export class CategoryApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(ApiUrlService);

  create(request: CreateCategoryRequest): Observable<Category> {
    return this.http.post<Category>(this.apiUrl.build('categories'), request);
  }

  findAll(): Observable<Category[]> {
    return this.http.get<Category[]>(this.apiUrl.build('categories'));
  }

  findById(id: string): Observable<Category> {
    return this.http.get<Category>(this.apiUrl.build(`categories/${encodeURIComponent(id)}`));
  }

  update(id: string, request: UpdateCategoryRequest): Observable<Category> {
    return this.http.patch<Category>(
      this.apiUrl.build(`categories/${encodeURIComponent(id)}`),
      request,
    );
  }
}
