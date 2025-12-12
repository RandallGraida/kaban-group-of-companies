import { Injectable, signal } from '@angular/core';

/**
 * Defines the possible variants for a toast notification, which typically
 * correspond to different colors and icons.
 */
export type ToastVariant = 'success' | 'error' | 'info';

/**
 * A service for displaying toast notifications (snackbars).
 * It manages the state of the toast, including its visibility, title, message,
 * and variant. The state is managed using Angular signals to allow for
 * reactive updates in the UI.
 */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private timeoutId: number | null = null;

  /**
   * A signal to control the visibility of the toast.
   */
  readonly visible = signal(false);

  /**
   * A signal for the title of the toast.
   */
  readonly title = signal('');

  /**
   * A signal for the message body of the toast.
   */
  readonly message = signal<string | null>(null);

  /**
   * A signal for the variant (e.g., 'success', 'error') of the toast.
   */
  readonly variant = signal<ToastVariant>('info');

  /**
   * Shows a toast notification with the given options.
   * @param opts The options for the toast, including title, message, variant,
   * and an optional auto-close duration.
   */
  show(opts: { title: string; message?: string; variant?: ToastVariant; autoCloseMs?: number }): void {
    const autoCloseMs = opts.autoCloseMs ?? 3500;

    this.title.set(opts.title);
    this.message.set(opts.message ?? null);
    this.variant.set(opts.variant ?? 'info');
    this.visible.set(true);

    // If there's an existing timeout, clear it.
    if (this.timeoutId !== null) window.clearTimeout(this.timeoutId);
    // Set a new timeout to automatically dismiss the toast.
    this.timeoutId = window.setTimeout(() => this.dismiss(), autoCloseMs);
  }

  /**
   * Dismisses the currently visible toast.
   */
  dismiss(): void {
    this.visible.set(false);
    if (this.timeoutId !== null) {
      window.clearTimeout(this.timeoutId);
      this.timeoutId = null;
    }
  }
}
