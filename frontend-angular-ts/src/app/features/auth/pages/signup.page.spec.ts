import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { provideRouter } from '@angular/router';
import { SignupPageComponent } from './signup.page';
import { AuthService } from '../api/auth.service';
import { ToastService } from '../../../core/services/toast.service';

/**
 * Unit tests for the SignupPageComponent.
 * These tests verify the component's behavior in response to user interactions
 * and API responses, using spies and mock services to isolate the component.
 */
describe('SignupPageComponent', () => {
  // A spy for the ToastService to verify that toasts are shown as expected.
  const toastSpy = { show: vi.fn(), dismiss: vi.fn() } as unknown as ToastService;

  /**
   * Tests that the component navigates to the login page on a successful signup.
   */
  it('navigates to /auth/login on successful signup', async () => {
    // A spy for the AuthService that returns a successful signup response.
    const authSpy = { signup: vi.fn(() => of({ message: 'ok' })) } as unknown as AuthService;

    await TestBed.configureTestingModule({
      imports: [SignupPageComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authSpy },
        { provide: ToastService, useValue: toastSpy },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(SignupPageComponent);
    const component = fixture.componentInstance;
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    component.form.setValue({
      email: 'user@example.com',
      password: 'Password123!',
      firstName: 'Jane',
      lastName: 'Doe',
    });

    component.onSubmit();

    expect(authSpy.signup).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(
      '/auth/login',
      expect.objectContaining({
        state: expect.objectContaining({ signupSuccess: true, email: 'user@example.com' }),
      }),
    );
  });

  /**
   * Tests that an error toast is shown on a failed signup.
   */
  it('shows error toast on failed signup', async () => {
    // A spy for the AuthService that returns an error response.
    const authSpy = { signup: vi.fn(() => throwError(() => ({ status: 409, error: { message: 'Already exists' } }))) } as unknown as AuthService;

    await TestBed.configureTestingModule({
      imports: [SignupPageComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authSpy },
        { provide: ToastService, useValue: toastSpy },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(SignupPageComponent);
    const component = fixture.componentInstance;

    component.form.setValue({
      email: 'user@example.com',
      password: 'Password123!',
      firstName: 'Jane',
      lastName: 'Doe',
    });

    component.onSubmit();

    expect(toastSpy.show).toHaveBeenCalled();
  });

  /**
   * Tests that the form submission is blocked if the form is invalid.
   */
  it('blocks submit when invalid', async () => {
    const authSpy = { signup: vi.fn(() => of({ message: 'ok' })) } as unknown as AuthService;

    await TestBed.configureTestingModule({
      imports: [SignupPageComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authSpy },
        { provide: ToastService, useValue: toastSpy },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(SignupPageComponent);
    const component = fixture.componentInstance;

    component.form.setValue({
      email: 'bad-email',
      password: 'short',
      firstName: '',
      lastName: '',
    });

    component.onSubmit();

    expect(authSpy.signup).not.toHaveBeenCalled();
  });

  it('blocks submit when password lacks required complexity', async () => {
    const authSpy = { signup: vi.fn(() => of({ message: 'ok' })) } as unknown as AuthService;

    await TestBed.configureTestingModule({
      imports: [SignupPageComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authSpy },
        { provide: ToastService, useValue: toastSpy },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(SignupPageComponent);
    const component = fixture.componentInstance;

    component.form.setValue({
      email: 'user@example.com',
      password: 'Password123', // missing symbol
      firstName: 'Jane',
      lastName: 'Doe',
    });

    component.onSubmit();

    expect(authSpy.signup).not.toHaveBeenCalled();
  });
});
