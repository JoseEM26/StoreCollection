export interface UsuarioResponse {
  id: number;
  nombre: string;
  email: string;
  celular: string;
  rol: 'ADMIN' | 'OWNER' | 'CUSTOMER';
}

export interface UsuarioPage {
  content: UsuarioResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}