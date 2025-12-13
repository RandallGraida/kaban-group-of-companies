import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../../features/auth/api/auth.service';

/**
 * An HTTP interceptor that adds a JWT Bearer token to the Authorization header
 * of outgoing requests. The token is retrieved from the AuthService.
 *
 * @param req The outgoing HTTP request.
 * @param next The next interceptor in the chain.
 * @returns An observable of the HTTP event stream.
 */
export const authTokenInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();

  // If there's no token or the Authorization header is already set,
  // pass the request through without modification.
  if (!token || req.headers.has('Authorization')) return next(req);

  // Clone the request and add the Authorization header.
  return next(
    req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    }),
  );
};
