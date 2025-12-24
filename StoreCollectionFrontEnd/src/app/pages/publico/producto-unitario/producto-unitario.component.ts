// src/app/componente/producto-unitario/producto-unitario.component.ts

import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoPublic, VariantePublic } from '../../../model/index.dto';
import { ProductoPublicService } from '../../../service/producto-public.service';
import { TiendaService } from '../../../service/tienda.service';
import { CarritoService } from '../../../service/carrito.service';

@Component({
  selector: 'app-producto-unitario',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './producto-unitario.component.html',
  styleUrls: ['./producto-unitario.component.css']
})
export class ProductoUnitarioComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private productoService = inject(ProductoPublicService);
  private tiendaService = inject(TiendaService);
  private carritoService = inject(CarritoService);

  producto!: ProductoPublic;
  tienda: any = null;
  loading = true;

  fallbackImage = 'https://via.placeholder.com/800x800.png?text=Sin+Imagen';

  varianteSeleccionada: VariantePublic | null = null;
  imagenActual: string = '';
  cantidad: number = 1;
  agregandoAlCarrito = false;
  mensajeExito = false;

  showQuoteModal = false;
  clienteNombre = '';
  clienteTelefono = '';
  clienteMensaje = '';
  enviandoCotizacion = false;

  // Selección por atributos independientes
  atributosAgrupados: { nombre: string; valores: string[] }[] = [];
  seleccionAtributos: { [atributoNombre: string]: string } = {};

  ngOnInit(): void {
    this.tiendaService.currentTienda$.subscribe(t => this.tienda = t);

    this.route.paramMap.subscribe(params => {
      const slug = params.get('productoSlug');
      if (slug) this.cargarProducto(slug);
    });
  }

  private cargarProducto(slug: string): void {
    this.loading = true;
    this.productoService.getBySlug(slug).subscribe({
      next: (prod) => {
        this.producto = prod;
        this.inicializarAtributos();
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  get tieneVariantesConImagen(): boolean {
    return this.producto?.variantes?.some(v => v.imagenUrl) ?? false;
  }

  private inicializarAtributos() {
    if (!this.producto.variantes || this.producto.variantes.length === 0) {
      this.atributosAgrupados = [];
      this.seleccionAtributos = {};
      this.varianteSeleccionada = null;
      this.actualizarImagen();
      return;
    }

    const mapa = new Map<string, Set<string>>();
    this.producto.variantes.forEach(variante => {
      variante.atributos.forEach(attr => {
        if (!mapa.has(attr.atributoNombre)) {
          mapa.set(attr.atributoNombre, new Set());
        }
        mapa.get(attr.atributoNombre)!.add(attr.valor);
      });
    });

    this.atributosAgrupados = Array.from(mapa.entries())
      .map(([nombre, valoresSet]) => ({
        nombre,
        valores: Array.from(valoresSet).sort()
      }))
      .sort((a, b) => a.nombre.localeCompare(b.nombre));

    this.seleccionAtributos = {};
    this.atributosAgrupados.forEach(attr => {
      this.seleccionAtributos[attr.nombre] = '';
    });

    this.atributosAgrupados.forEach(grupo => {
      if (grupo.valores.length === 1) {
        this.seleccionAtributos[grupo.nombre] = grupo.valores[0];
      }
    });

    this.seleccionarVarianteDisponible();

    if (!this.varianteSeleccionada) {
      this.actualizarVarianteSegunSeleccion();
    }
  }

  private seleccionarVarianteDisponible() {
    const varianteDisponible = this.producto.variantes.find(v => v.activo && v.stock > 0) ||
                               this.producto.variantes.find(v => v.activo);

    if (varianteDisponible) {
      varianteDisponible.atributos.forEach(attr => {
        if (this.seleccionAtributos.hasOwnProperty(attr.atributoNombre)) {
          this.seleccionAtributos[attr.atributoNombre] = attr.valor;
        }
      });
      this.varianteSeleccionada = varianteDisponible;
      this.actualizarImagen();
    }
  }

  seleccionarValorAtributo(atributoNombre: string, valor: string) {
    this.seleccionAtributos[atributoNombre] = valor;
    this.actualizarVarianteSegunSeleccion();
  }

  private actualizarVarianteSegunSeleccion() {
    if (!this.producto.variantes.length) {
      this.varianteSeleccionada = null;
      this.actualizarImagen();
      return;
    }

    const varianteCoincidente = this.producto.variantes.find(v => {
      const seleccionadosCount = Object.values(this.seleccionAtributos).filter(v => v !== '').length;
      if (v.atributos.length !== seleccionadosCount) return false;

      return v.atributos.every(attr =>
        this.seleccionAtributos[attr.atributoNombre] === attr.valor
      );
    });

    this.varianteSeleccionada = varianteCoincidente || null;
    this.actualizarImagen();
    this.cantidad = 1;
  }

  esValorDisponible(atributoNombre: string, valor: string): boolean {
    const otrasSelecciones = Object.entries(this.seleccionAtributos)
      .filter(([k]) => k !== atributoNombre)
      .some(([, v]) => v !== '');

    if (!otrasSelecciones) return true;

    return this.producto.variantes.some(variante => {
      const tieneEsteValor = variante.atributos.some(a =>
        a.atributoNombre === atributoNombre && a.valor === valor
      );
      if (!tieneEsteValor) return false;

      return variante.atributos.every(attr =>
        attr.atributoNombre === atributoNombre ||
        this.seleccionAtributos[attr.atributoNombre] === attr.valor ||
        this.seleccionAtributos[attr.atributoNombre] === ''
      );
    });
  }

  tieneAlgunaSeleccion(): boolean {
    return Object.values(this.seleccionAtributos).some(v => v !== '');
  }

  cambiarImagen(url: string | undefined) {
    this.imagenActual = url?.trim() || this.producto.imagenPrincipal || this.fallbackImage;
  }

  private actualizarImagen() {
    const url = this.varianteSeleccionada?.imagenUrl || this.producto.imagenPrincipal;
    this.imagenActual = url?.trim() || this.fallbackImage;
  }

  // ===== GETTERS =====
  get precioActual() { return this.varianteSeleccionada?.precio ?? this.producto.precioMinimo; }
  get hayStock() { return (this.varianteSeleccionada?.stock ?? 0) > 0; }
  get stockActual() { return this.varianteSeleccionada?.stock ?? 0; }

  get atributosTexto(): string {
    if (!this.varianteSeleccionada?.atributos?.length) return '';
    return this.varianteSeleccionada.atributos
      .map(a => `${a.atributoNombre}: ${a.valor}`)
      .join(' • ');
  }

  get whatsappNumero(): string {
    return this.tienda?.whatsapp?.replace(/\D/g, '') || '51987654321';
  }

  // ===== CARRITO =====
  aumentarCantidad() {
    if (this.cantidad < this.stockActual) this.cantidad++;
  }

  disminuirCantidad() {
    if (this.cantidad > 1) this.cantidad--;
  }

  agregarAlCarrito() {
    if (!this.hayStock || !this.varianteSeleccionada) return;

    this.agregandoAlCarrito = true;
    this.carritoService.agregarAlCarrito(this.varianteSeleccionada.id, this.cantidad).subscribe({
      next: () => {
        this.agregandoAlCarrito = false;
        this.mensajeExito = true;
        setTimeout(() => this.mensajeExito = false, 3000);
      },
      error: () => {
        this.agregandoAlCarrito = false;
        alert('Error al agregar al carrito');
      }
    });
  }

  // ===== WHATSAPP Y LLAMADA =====
  consultarWhatsApp() {
    const msg = encodeURIComponent(
      `¡Hola! Me interesa:\n\n*${this.producto.nombre}*\n${this.atributosTexto ? this.atributosTexto + '\n' : ''}S/ ${this.precioActual.toFixed(2)}\nCantidad: ${this.cantidad}\n\n¿Está disponible?`
    );
    window.open(`https://wa.me/${this.whatsappNumero}?text=${msg}`, '_blank');
  }

  llamarAhora() {
    window.location.href = `tel:${this.whatsappNumero}`;
  }

  // ===== COTIZACIÓN MODAL =====
  abrirCotizacion() {
    this.showQuoteModal = true;
    this.clienteMensaje = `Hola, me interesa: ${this.producto.nombre} ${this.atributosTexto ? '(' + this.atributosTexto + ')' : ''} (S/ ${this.precioActual.toFixed(2)})`;
  }

  cerrarModal() {
    this.showQuoteModal = false;
    this.clienteNombre = '';
    this.clienteTelefono = '';
    this.clienteMensaje = '';
  }

  get atributosFaltantesTexto(): string {
    if (this.atributosAgrupados.length === 0) return '';

    const faltantes = this.atributosAgrupados
      .filter(g => !this.seleccionAtributos[g.nombre] || this.seleccionAtributos[g.nombre] === '')
      .map(g => g.nombre);

    if (faltantes.length === 0) return '';
    if (faltantes.length === 1) return faltantes[0];

    return faltantes.slice(0, -1).join(', ') + ' y ' + faltantes[faltantes.length - 1];
  }

  enviarCotizacion() {
    if (!this.clienteNombre || !this.clienteTelefono) return;

    this.enviandoCotizacion = true;
    setTimeout(() => {
      alert(`¡Gracias ${this.clienteNombre}! Te contactaremos pronto por WhatsApp.`);
      this.cerrarModal();
      this.enviandoCotizacion = false;
    }, 1500);
  }
}