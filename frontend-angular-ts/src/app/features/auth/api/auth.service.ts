import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginDto, RegistrationResponse, SignupDto } from '../models';

/**
 * Represents the authentication state of the user.
 */
type AuthState = {
  token: string | null;
  role: string | null;
  expiresAt: string | null;
};

const STORAGE_KEY = 'kaban.auth';

/**
 * Manages user authentication state, including login, signup, and logout.
 * This service interacts with the backend authentication API and persists the
 * user's session in local storage. It uses Angular signals to manage and
 * expose authentication state reactively.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  /**
   * The authentication state is stored in a signal, which is initialized
   * from local storage.
   */
  private readonly state = signal<AuthState>(this.readFromStorage());

  // A computed signal that returns true if the user is logged in.
  readonly isLoggedIn = computed(() => !!this.state().token);

  // A computed signal for the authentication token.
  readonly token = computed(() => this.state().token);

  // A computed signal for the user's role.
  readonly role = computed(() => this.state().role);

  // A computed signal for the token's expiration date.
  readonly expiresAt = computed(() => this.state().expiresAt);

  /**
   * Sends a login request to the backend and persists the session on success.
   * @param credentials The user's login credentials.
   * @returns An observable of the authentication response.
   */
  login(credentials: LoginDto): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', credentials).pipe(
      tap((response) => this.persistSession(response)),
    );
  }

  /**
   * Sends a signup request to the backend.
   * @param request The user's registration details.
   * @returns An observable of the registration response.
   */
  signup(request: SignupDto): Observable<RegistrationResponse> {
    return this.http.post<RegistrationResponse>('/api/auth/signup', request);
  }

  /**
   * Requests a new verification email for an existing, unverified account.
   * @param email Email address to (re)verify.
   * @returns An observable of a generic message response.
   */
  resendVerification(email: string): Observable<RegistrationResponse> {
    return this.http.post<RegistrationResponse>('/api/auth/resend-verification', { email });
  }

  /**
   * Logs the user out by clearing the authentication state and removing the
   * session from local storage.
   */
  logout(): void {
    this.state.set({ token: null, role: null, expiresAt: null });
    window.localStorage.removeItem(STORAGE_KEY);
  }

  /**
   * Persists the authentication response to the state signal and local storage.
   * @param auth The authentication response from the backend.
   */
  private persistSession(auth: AuthResponse): void {
    const next: AuthState = {
      token: auth.token,
      role: auth.role,
      expiresAt: auth.expiresAt,
    };
    this.state.set(next);
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  }

  /**
   * Reads the authentication state from local storage.
   * @returns The authentication state, or a default state if not found or invalid.
   */
  private readFromStorage(): AuthState {
    try {
      const raw = window.localStorage.getItem(STORAGE_KEY);
      if (!raw) return { token: null, role: null, expiresAt: null };

      const parsed = JSON.parse(raw) as Partial<AuthState>;
      return {
        token: parsed.token ?? null,
        role: parsed.role ?? null,
        expiresAt: parsed.expiresAt ?? null,
      };
    } catch {
      return { token: null, role: null, expiresAt: null };
    }
  }
}
