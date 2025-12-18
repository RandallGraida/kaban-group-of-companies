import { Component } from '@angular/core';
import { HeaderComponent } from '../../../components/common/header/header.component';

@Component({
  standalone: true,
  selector: 'app-dashboard-page',
  imports: [HeaderComponent],
  template: `
    <app-header />
    <main class="min-h-screen bg-slate-50 text-slate-900">
      <section class="mx-auto max-w-5xl px-6 py-12">
        <div class="rounded-2xl bg-white p-10 shadow-lg border border-slate-200">
          <h1 class="text-2xl font-bold tracking-tight">Dashboard</h1>
          <p class="mt-2 text-slate-600">You are authenticated.</p>
        </div>
      </section>
    </main>
  `,
})
export class DashboardPageComponent {}
