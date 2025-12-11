import { Component } from '@angular/core';

@Component({
  selector: 'app-header',
  standalone: true,
  template: `
    <header class="flex items-center justify-between bg-slate-900 text-white px-6 py-3 shadow-md">
      <div class="text-lg font-semibold tracking-tight">Kaban Banking</div>
      <nav class="flex items-center gap-3">
        <button
          class="rounded-lg border border-slate-500 px-3 py-2 text-sm font-medium text-slate-100 hover:border-slate-300 hover:text-white transition">
          Login
        </button>
        <button
          class="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500 transition">
          Signup
        </button>
      </nav>
    </header>
  `,
})
export class HeaderComponent {}
