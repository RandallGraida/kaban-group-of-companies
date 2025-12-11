import { Component } from '@angular/core';
import { HeaderComponent } from '../../../components/common/header/header.component';

@Component({
  standalone: true,
  imports: [HeaderComponent],
  selector: 'app-landing-page',
  template: `
    <app-header />
    <main class="min-h-screen bg-slate-50 text-slate-900">
      <section class="mx-auto max-w-5xl px-6 py-12">
        <div class="rounded-2xl bg-white p-10 shadow-lg">
          <p class="text-sm font-semibold uppercase text-blue-600">Welcome</p>
          <h1 class="mt-2 text-3xl font-bold tracking-tight text-slate-900">
            Secure, real-time banking for Kaban customers
          </h1>
          <p class="mt-4 text-slate-600">
            Manage accounts, transfers, and notifications with enterprise-grade security.
          </p>
        </div>
      </section>
    </main>
  `,
})
export class LandingPageComponent {}
