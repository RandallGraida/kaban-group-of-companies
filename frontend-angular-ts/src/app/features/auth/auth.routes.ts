import { Routes } from '@angular/router';
import { LoginPageComponent } from './pages/login.page';
import { SignupPageComponent } from './pages/signup.page';

/**
 * Defines the routes for the authentication feature area.
 * This includes routes for login, signup, and a default redirect to the login page.
 */
export const authRoutes: Routes = [
  // The login page route.
  { path: 'login', component: LoginPageComponent },

  // The signup page route.
  { path: 'signup', component: SignupPageComponent },

  // A redirect from the base auth path to the login page.
  { path: '', pathMatch: 'full', redirectTo: 'login' },
];
