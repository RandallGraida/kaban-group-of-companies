import { Component, computed, inject } from '@angular/core';
import { ToastService } from '../../../core/services/toast.service';

/**
 * A standalone component that acts as a host for displaying toast notifications.
 * It subscribes to the ToastService to reactively show or hide toasts based on
 * the service's state. The appearance of the toast (e.g., color) is determined
 * by the toast's variant.
 */
@Component({
  selector: 'app-toast-host',
  standalone: true,
  template: `
    @if (toast.visible()) {
      <div class="fixed bottom-4 right-4 z-50 w-[min(92vw,420px)]">
        <div
          class="rounded-xl shadow-lg border px-4 py-3 bg-white"
          [class.border-emerald-200]="toast.variant() === 'success'"
          [class.border-rose-200]="toast.variant() === 'error'"
          [class.border-slate-200]="toast.variant() === 'info'"
        >
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <p class="text-sm font-semibold text-slate-900">{{ toast.title() }}</p>
              @if (toast.message()) {
                <p class="mt-1 text-sm text-slate-600 break-words">{{ toast.message() }}</p>
              }
            </div>
            <button
              type="button"
              class="shrink-0 rounded-md px-2 py-1 text-sm text-slate-500 hover:text-slate-900"
              (click)="toast.dismiss()"
              aria-label="Dismiss"
            >
              ✕
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class ToastHostComponent {
  /**
   * Injects the ToastService to access the toast's state.
   */
  protected readonly toast = inject(ToastService);
}
