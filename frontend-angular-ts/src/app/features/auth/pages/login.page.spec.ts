import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { provideRouter } from '@angular/router';
import { LoginPageComponent } from './login.page';
import { AuthService } from '../api/auth.service';
import { ToastService } from '../../../core/services/toast.service';

/**
 * Unit tests for the LoginPageComponent.
 * These tests verify the component's behavior in response to user interactions
 * and API responses, using spies and mock services to isolate the component.
 */
describe('LoginPageComponent', () => {
  // A spy for the ToastService to verify that toasts are shown as expected.
  const toastSpy = { show: vi.fn(), dismiss: vi.fn() } as unknown as ToastService;

  /**
   * Tests that the component navigates to the dashboard on a successful login.
   */
  it('navigates to /dashboard on successful login', async () => {
    // A spy for the AuthService that returns a successful login response.
    const authSpy = { login: vi.fn(() => of({ token: 't', role: 'ROLE_USER', expiresAt: 'x' })) } as unknown as AuthService;

    await TestBed.configureTestingModule({
      imports: [LoginPageComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authSpy },
        { provide: ToastService, useValue: toastSpy },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(LoginPageComponent);
    const component = fixture.componentInstance;
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    component.form.setValue({ email: 'user@example.com', password: 'Password123!' });
    component.onSubmit();

    expect(authSpy.login).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith('/dashboard');
  });

  /**
   * Tests that an error toast is shown on a failed login.
   */
  it('shows error toast on failed login', async () => {
    // A spy for the AuthService that returns an error response.
    const authSpy = { login: vi.fn(() => throwError(() => ({ status: 401, error: { message: 'Bad creds' } }))) } as unknown as AuthService;

    await TestBed.configureTestingModule({
      imports: [LoginPageComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authSpy },
        { provide: ToastService, useValue: toastSpy },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(LoginPageComponent);
    const component = fixture.componentInstance;

    component.form.setValue({ email: 'user@example.com', password: 'Password123!' });
    component.onSubmit();

    expect(toastSpy.show).toHaveBeenCalled();
  });

  /**
   * Tests that the form submission is blocked if the form is invalid.
   */
  it('blocks submit when invalid', async () => {
    const authSpy = { login: vi.fn(() => of({ token: 't', role: 'ROLE_USER', expiresAt: 'x' })) } as unknown as AuthService;

    await TestBed.configureTestingModule({
      imports: [LoginPageComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authSpy },
        { provide: ToastService, useValue: toastSpy },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(LoginPageComponent);
    const component = fixture.componentInstance;

    component.form.setValue({ email: '', password: '' });
    component.onSubmit();

    expect(authSpy.login).not.toHaveBeenCalled();
  });
});
