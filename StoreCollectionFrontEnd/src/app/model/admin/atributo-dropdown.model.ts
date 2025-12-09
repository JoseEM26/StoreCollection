export interface AtributoConValores {
  id: number;
  nombre: string;
  valores: AtributoValorDto[];
}

export interface AtributoValorDto {
  id: number;
  valor: string;
}