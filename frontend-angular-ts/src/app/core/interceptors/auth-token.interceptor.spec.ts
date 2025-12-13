import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../features/auth/api/auth.service';
import { authTokenInterceptor } from './auth-token.interceptor';

describe('authTokenInterceptor', () => {
  function setup(token: string | null) {
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { token: () => token } as unknown as AuthService },
        provideHttpClient(withInterceptors([authTokenInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    return {
      http: TestBed.inject(HttpClient),
      httpMock: TestBed.inject(HttpTestingController),
    };
  }

  it('attaches Authorization header when token exists', () => {
    const { http, httpMock } = setup('jwt');
    http.get('/api/secure').subscribe();
    const req = httpMock.expectOne('/api/secure');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt');
    req.flush({});
    httpMock.verify();
  });

  it('does not override an existing Authorization header', () => {
    const { http, httpMock } = setup('jwt');
    http.get('/api/secure', { headers: { Authorization: 'Bearer existing' } }).subscribe();
    const req = httpMock.expectOne('/api/secure');
    expect(req.request.headers.get('Authorization')).toBe('Bearer existing');
    req.flush({});
    httpMock.verify();
  });

  it('does not attach Authorization header when token is missing', () => {
    const { http, httpMock } = setup(null);
    http.get('/api/secure').subscribe();
    const req = httpMock.expectOne('/api/secure');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
    httpMock.verify();
  });
});
