import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HeaderComponent } from '../../../components/common/header/header.component';
import { ToastService } from '../../../core/services/toast.service';
import { AuthService } from '../api/auth.service';
import { finalize, map, takeWhile } from 'rxjs/operators';
import { timer } from 'rxjs';

/**
 * A standalone component that provides a user interface for logging in.
 * It includes a form for email and password, handles user input, and
 * communicates with the AuthService to perform authentication.
 */
@Component({
  standalone: true,
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink, HeaderComponent],
  template: `
    <app-header />

    <main class="min-h-screen bg-slate-50 text-slate-900">
      <section class="mx-auto max-w-md px-6 py-12">
        <div class="rounded-2xl bg-white p-8 shadow-lg border border-slate-200">
          <h1 class="text-2xl font-bold tracking-tight">Login</h1>
          <p class="mt-1 text-sm text-slate-600">Welcome back to Kaban Banking.</p>

          <form class="mt-6 space-y-4" [formGroup]="form" (ngSubmit)="onSubmit()">
            @if (signupSuccessEmail()) {
              <div
                class="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-900"
              >
                Account created. Please verify your email before logging in.
              </div>
            }

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
                placeholder="••••••••"
              />
              @if (password.touched && password.invalid) {
                <p class="mt-1 text-sm text-rose-600">Password is required.</p>
              }
            </div>

            <button
              type="submit"
              [disabled]="isLoading() || form.invalid"
              class="w-full rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500 disabled:opacity-60"
            >
              @if (isLoading()) {
                Logging in...
              } @else {
                Login
              }
            </button>

            <!-- Resend Verification Email (only when needed) -->
            @if (showResendVerification()) {
              <div class="rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-800">
                <p class="text-slate-700">Didn't receive a verification email?</p>
                <div class="mt-2 flex items-center gap-3">
                  <button
                    type="button"
                    class="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-semibold text-slate-800 disabled:opacity-60"
                    (click)="onResendVerification()"
                    [disabled]="resendInProgress() || resendCooldownRemaining() > 0 || email.invalid"
                  >
                    Resend verification email
                  </button>

                  @if (resendCooldownRemaining() > 0) {
                    <span class="text-sm text-slate-600">
                      Try again in {{ resendCooldownRemaining() }}s
                    </span>
                  }
                </div>

                @if (resendStatusMessage()) {
                  <p class="mt-2 text-sm text-slate-700">
                    {{ resendStatusMessage() }}
                  </p>
                }
              </div>
            }
          </form>

          <p class="mt-5 text-sm text-slate-600">
            No account?
            <a routerLink="/auth/signup" class="font-semibold text-blue-600 hover:text-blue-500"
              >Sign up</a
            >
          </p>
        </div>
      </section>
    </main>
  `,
})
export class LoginPageComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  private readonly cooldownSeconds = 60;

  // A signal to track the loading state of the login process.
  readonly isLoading = signal(false);

  readonly signupSuccessEmail = signal<string | null>(null);
  readonly unverifiedEmail = signal<string | null>(null);

  readonly resendInProgress = signal(false);
  readonly resendCooldownRemaining = signal(0);
  readonly resendStatusMessage = signal<string | null>(null);

  // The reactive form group for the login form.
  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  // A getter for easy access to the email form control.
  get email() {
    return this.form.controls.email;
  }

  // A getter for easy access to the password form control.
  get password() {
    return this.form.controls.password;
  }

  constructor() {
    const state = globalThis.history.state as { signupSuccess?: boolean; email?: unknown } | null;
    if (state?.signupSuccess && typeof state.email === 'string' && state.email.length > 0) {
      this.signupSuccessEmail.set(state.email);
      this.unverifiedEmail.set(state.email);
    }
  }

  showResendVerification(): boolean {
    return !!this.unverifiedEmail();
  }

  /**
   * Handles the form submission. It calls the AuthService to log in the user
   * and navigates to the dashboard on success, or shows an error toast on failure.
   */
  onSubmit(): void {
    if (this.form.invalid || this.isLoading()) return;

    this.isLoading.set(true);
    this.resendStatusMessage.set(null);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.isLoading.set(false);
        void this.router.navigateByUrl('/dashboard');
      },
      error: (err) => {
        this.isLoading.set(false);
        const message = typeof err?.error?.message === 'string' ? err.error.message : 'Login failed.';
        if (err?.status === 403 && message === 'Email not verified') {
          this.unverifiedEmail.set(this.email.value);
          this.toast.show({
            title: 'Email not verified',
            message: 'Please verify your email to continue. You can resend the verification email below.',
            variant: 'error',
          });
          return;
        }

        this.toast.show({ title: 'Login failed', message, variant: 'error' });
      },
    });
  }

  onResendVerification(): void {
    const email = this.unverifiedEmail() ?? this.email.value;
    if (!email || this.resendInProgress() || this.resendCooldownRemaining() > 0) return;
    if (this.email.invalid) return;

    this.resendInProgress.set(true);
    this.resendStatusMessage.set(null);
    this.startResendCooldown();

    this.auth
      .resendVerification(email)
      .pipe(finalize(() => this.resendInProgress.set(false)))
      .subscribe({
        next: (res) => this.resendStatusMessage.set(res?.message ?? 'Verification email sent.'),
        error: () =>
          this.resendStatusMessage.set(
            'If the email exists, a verification link was sent. Please check your inbox.',
          ),
      });
  }

  private startResendCooldown(): void {
    timer(0, 1000)
      .pipe(
        map((tick) => this.cooldownSeconds - tick),
        takeWhile((remaining) => remaining >= 0),
      )
      .subscribe((remaining) => this.resendCooldownRemaining.set(remaining));
  }
}
