import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastHostComponent } from './components/common/toast/toast-host.component';

/**
 * The root component of the application.
 * It serves as the main container for the application's content, hosting the
 * router outlet for navigation and the toast host for notifications.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastHostComponent],
  template: `
    <!-- The router-outlet directive is a placeholder that Angular dynamically fills
         with the component corresponding to the current router state. -->
    <router-outlet />

    <!-- The app-toast-host component is included here to be available globally
         for displaying toast notifications. -->
    <app-toast-host />
  `,
})
export class App {
  /**
   * A signal holding the title of the application.
   */
  protected readonly title = signal('kaban-frontend');
}
