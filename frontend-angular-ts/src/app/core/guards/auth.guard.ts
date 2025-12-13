import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../../features/auth/api/auth.service';

/**
 * A route guard that checks if a user is logged in before allowing access to a route.
 * If the user is not logged in, they are redirected to the login page.
 *
 * @returns A boolean indicating whether the route can be activated.
 */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  // If the user is logged in, allow access.
  if (auth.isLoggedIn()) return true;

  // Otherwise, redirect to the login page and deny access.
  router.navigateByUrl('/auth/login');
  return false;
};
