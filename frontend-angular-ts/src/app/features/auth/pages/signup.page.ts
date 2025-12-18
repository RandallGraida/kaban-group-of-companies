import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HeaderComponent } from '../../../components/common/header/header.component';
import { ToastService } from '../../../core/services/toast.service';
import { AuthService } from '../api/auth.service';

/**
 * A custom validator for password complexity.
 * It checks for minimum length, and the presence of lowercase, uppercase, numbers, and symbols.
 * @param control The form control to validate.
 * @returns A validation error object if the password is not complex enough, otherwise null.
 */
function passwordComplexity(control: AbstractControl<string>): ValidationErrors | null {
  const value = control.value ?? '';
  if (!value) return null;

  const minLength = value.length >= 8;
  const hasLower = /[a-z]/.test(value);
  const hasUpper = /[A-Z]/.test(value);
  const hasNumber = /\d/.test(value);
  const hasSymbol = /[^A-Za-z0-9]/.test(value);

  return minLength && hasLower && hasUpper && hasNumber && hasSymbol
    ? null
    : { passwordComplexity: true };
}

/**
 * A standalone component for the user signup page.
 * It provides a form for new users to register, including fields for name, email, and a complex password.
 * It communicates with the AuthService to perform the registration.
 */
@Component({
  standalone: true,
  selector: 'app-signup-page',
  imports: [ReactiveFormsModule, RouterLink, HeaderComponent],
  template: `
    <app-header />

    <main class="min-h-screen bg-slate-50 text-slate-900">
      <section class="mx-auto max-w-md px-6 py-12">
        <div class="rounded-2xl bg-white p-8 shadow-lg border border-slate-200">
          <h1 class="text-2xl font-bold tracking-tight">Create account</h1>
          <p class="mt-1 text-sm text-slate-600">Sign up for Kaban Banking.</p>

          <form class="mt-6 space-y-4" [formGroup]="form" (ngSubmit)="onSubmit()">
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-sm font-medium text-slate-700">First name</label>
                <input
                  type="text"
                  formControlName="firstName"
                  class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500"
                  placeholder="Jane"
                />
                @if (firstName.touched && firstName.invalid) {
                  <p class="mt-1 text-sm text-rose-600">Required.</p>
                }
              </div>
              <div>
                <label class="block text-sm font-medium text-slate-700">Last name</label>
                <input
                  type="text"
                  formControlName="lastName"
                  class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500"
                  placeholder="Doe"
                />
                @if (lastName.touched && lastName.invalid) {
                  <p class="mt-1 text-sm text-rose-600">Required.</p>
                }
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-slate-700">Email</label>
              <input
                type="email"
                formControlName="email"
                class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500"
                placeholder="user@example.com"
              />
              @if (email.touched && email.invalid) {
                <p class="mt-1 text-sm text-rose-600">Enter a valid email.</p>
              }
            </div>

            <div>
              <label class="block text-sm font-medium text-slate-700">Password</label>
              <input
                type="password"
                formControlName="password"
                class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500"
                placeholder="At least 8 characters"
              />
              @if (password.touched && password.invalid) {
                <p class="mt-1 text-sm text-rose-600">
                  Use 8+ chars with upper/lower/number/symbol.
                </p>
              }
            </div>

            <button
              type="submit"
              [disabled]="isLoading() || form.invalid"
              class="w-full rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500 disabled:opacity-60"
            >
              @if (isLoading()) { Creating account... } @else { Sign up }
            </button>
          </form>

          <p class="mt-5 text-sm text-slate-600">
            Already have an account?
            <a routerLink="/auth/login" class="font-semibold text-blue-600 hover:text-blue-500">Login</a>
          </p>
        </div>
      </section>
    </main>
  `,
})
export class SignupPageComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  // A signal to track the loading state of the signup process.
  readonly isLoading = signal(false);

  // The reactive form group for the signup form.
  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, passwordComplexity]],
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
  });

  // A getter for easy access to the email form control.
  get email() {
    return this.form.controls.email;
  }

  // A getter for easy access to the password form control.
  get password() {
    return this.form.controls.password;
  }

  // A getter for easy access to the first name form control.
  get firstName() {
    return this.form.controls.firstName;
  }

  // A getter for easy access to the last name form control.
  get lastName() {
    return this.form.controls.lastName;
  }

  /**
   * Handles the form submission. It calls the AuthService to register the user
   * and shows a success toast before redirecting to the login page.
   * If an error occurs, it displays an error toast.
   */
  onSubmit(): void {
    if (this.form.invalid || this.isLoading()) return;

    this.isLoading.set(true);
    this.auth.signup(this.form.getRawValue()).subscribe({
      next: async (res) => {
        this.isLoading.set(false);
        this.toast.show({
          title: 'Registration submitted',
          message: res?.message ?? 'Check your email to verify your account.',
          variant: 'success',
        });
        await this.router.navigateByUrl('/auth/login', {
          state: { signupSuccess: true, email: this.email.value },
        });
      },
      error: (err) => {
        this.isLoading.set(false);
        const message = typeof err?.error?.message === 'string' ? err.error.message : 'Signup failed.';
        this.toast.show({ title: 'Signup failed', message, variant: 'error' });
      },
    });
  }
}
