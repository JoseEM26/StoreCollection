export interface UsuarioResponse {
  id: number;
  nombre: string;
  email: string;
  activo: boolean;
  celular: string;
  rol: 'ADMIN' | 'OWNER' | 'CUSTOMER';
}
export interface UsuarioRequest {
  nombre: string;
  email: string;
  password?: string;     // Opcional en edición
  celular?: string;
  activo?: boolean;
  rol: 'ADMIN' | 'OWNER' | 'CUSTOMER';
}
export interface UsuarioPage {
  content: UsuarioResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}