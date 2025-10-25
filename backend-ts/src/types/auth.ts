export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  date_of_birth: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  success: boolean;
  message: string;
  user?: {
    id: string;
    email: string;
    name: string;
    date_of_birth: string;
  };
  token?: string;
}

export interface ErrorResponse {
  success: false;
  message: string;
  errors?: string[];
}