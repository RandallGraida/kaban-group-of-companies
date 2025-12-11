import { Routes } from '@angular/router';
import { LandingPageComponent } from './features/landing/pages/landing.page';

export const routes: Routes = [
  { path: '', component: LandingPageComponent },
  { path: '**', redirectTo: '' },
];
