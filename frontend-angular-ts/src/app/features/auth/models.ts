/**
 * Data Transfer Object (DTO) for login requests.
 */
export interface LoginDto {
  email: string;
  password: string;
}

/**
 * Data Transfer Object (DTO) for signup requests.
 */
export interface SignupDto {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

/**
 * Represents the response from a successful authentication request.
 */
export interface AuthResponse {
  token: string;
  role: string;
  expiresAt: string;
}

/**
 * Represents the response from a registration request.
 */
export interface RegistrationResponse {
  message: string;
}
