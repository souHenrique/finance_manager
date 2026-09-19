import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/config/api-base-url';
import { Category, CreateCategoryRequest, UpdateCategoryRequest } from '../models/category.models';
import { CategoryApiService } from './category-api.service';

describe('CategoryApiService', () => {
  let service: CategoryApiService;
  let httpMock: HttpTestingController;

  const category: Category = {
    id: 'c487c4cf-d948-4ba8-a85f-e36bb798c928',
    name: 'Alimentação',
    icon: 'FOOD',
    type: 'EXPENSE',
    parentCategoryId: null,
    status: 'ACTIVE',
    createdAt: '2026-09-02T12:00:00Z',
    updatedAt: '2026-09-02T12:30:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CategoryApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: API_BASE_URL,
          useValue: '/api/v1',
        },
      ],
    });

    service = TestBed.inject(CategoryApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve criar uma categoria', () => {
    const payload: CreateCategoryRequest = {
      name: 'Alimentação',
      icon: 'FOOD',
      type: 'EXPENSE',
      parentCategoryId: null,
    };

    service.create(payload).subscribe((response) => {
      expect(response).toEqual(category);
    });

    const request = httpMock.expectOne('/api/v1/categories');

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);

    request.flush(category);
  });

  it('deve listar as categorias', () => {
    service.findAll().subscribe((response) => {
      expect(response).toEqual([category]);
    });

    const request = httpMock.expectOne('/api/v1/categories');

    expect(request.request.method).toBe('GET');

    request.flush([category]);
  });

  it('deve buscar uma categoria pelo id', () => {
    service.findById(category.id).subscribe((response) => {
      expect(response).toEqual(category);
    });

    const request = httpMock.expectOne(`/api/v1/categories/${category.id}`);

    expect(request.request.method).toBe('GET');

    request.flush(category);
  });

  it('deve atualizar uma categoria', () => {
    const payload: UpdateCategoryRequest = {
      name: 'Supermercado',
      icon: 'SHOPPING',
      status: 'INACTIVE',
    };
    const response: Category = {
      ...category,
      ...payload,
    };

    service.update(category.id, payload).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne(`/api/v1/categories/${category.id}`);

    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual(payload);

    request.flush(response);
  });
});
