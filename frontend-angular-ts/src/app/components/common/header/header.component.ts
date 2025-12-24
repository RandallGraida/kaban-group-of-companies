import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../features/auth/api/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink],
  template: `
    <header class="flex items-center bg-slate-900 text-white px-6 py-3 shadow-md">
      @if (!auth.isLoggedIn()) {
        <a class="text-lg font-semibold tracking-tight" routerLink="/"> Kaban Banking </a>
      }
      <nav class="flex items-center gap-3 ml-auto">
        @if (!auth.isLoggedIn()) {
          <a
            routerLink="/auth/login"
            class="rounded-lg border border-slate-500 px-3 py-2 text-sm font-medium text-slate-100 hover:border-slate-300 hover:text-white transition"
          >
            Login
          </a>
          <a
            routerLink="/auth/signup"
            class="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500 transition"
          >
            Signup
          </a>
        } @else {
          <a
            routerLink="/dashboard"
            class="rounded-lg border border-slate-500 px-3 py-2 text-sm font-medium text-slate-100 hover:border-slate-300 hover:text-white transition"
          >
            Dashboard
          </a>
          <button
            type="button"
            (click)="logout()"
            class="rounded-lg bg-rose-600 px-4 py-2 text-sm font-semibold text-white hover:bg-rose-500 transition"
          >
            Logout
          </button>
        }
      </nav>
    </header>
  `,
})
export class HeaderComponent {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/']);
  }
}
