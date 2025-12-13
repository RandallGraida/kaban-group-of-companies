import { Routes } from '@angular/router';
import { LandingPageComponent } from './features/landing/pages/landing.page';
import { authRoutes } from './features/auth/auth.routes';
import { DashboardPageComponent } from './features/dashboard/pages/dashboard.page';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', component: LandingPageComponent },
  { path: 'auth', children: authRoutes },
  { path: 'dashboard', component: DashboardPageComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '' },
];
