import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

/**
 * Unit tests for the AuthService.
 * These tests use the HttpTestingController to mock HTTP requests and verify
 * the service's behavior without making actual API calls.
 */
describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    // Clear local storage before each test to ensure a clean state.
    window.localStorage.clear();

    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Verify that there are no outstanding HTTP requests.
    httpMock.verify();
  });

  /**
   * Tests that the service correctly stores the authentication token in state
   * and local storage after a successful login.
   */
  it('stores token after login', () => {
    expect(service.isLoggedIn()).toBe(false);

    service.login({ email: 'user@example.com', password: 'Password123!' }).subscribe();

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush({ token: 'jwt', role: 'ROLE_USER', expiresAt: '2030-01-01T00:00:00Z' });

    expect(service.isLoggedIn()).toBe(true);
    expect(service.token()).toBe('jwt');

    const stored = window.localStorage.getItem('kaban.auth');
    expect(stored).toContain('jwt');
  });

  /**
   * Tests that the service clears the authentication state and local storage
   * on logout.
   */
  it('clears state on logout', () => {
    service.login({ email: 'user@example.com', password: 'Password123!' }).subscribe();
    httpMock
      .expectOne('/api/auth/login')
      .flush({ token: 'jwt', role: 'ROLE_USER', expiresAt: '2030-01-01T00:00:00Z' });

    service.logout();

    expect(service.isLoggedIn()).toBe(false);
    expect(service.token()).toBeNull();
    expect(window.localStorage.getItem('kaban.auth')).toBeNull();
  });
});
