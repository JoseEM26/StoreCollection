import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import {
  ProductoAdminListItem,
  ProductoAdminListPage,
  VarianteResponse
} from '../../../../model/admin/producto-admin.model';
import { DropTownService } from '../../../../service/droptown.service';
import Swal from 'sweetalert2';
import { VentaDirectaRequest } from '../../../../model/boleta.model';
import { BoletaService } from '../../../../service/boleta.service';

interface ItemVenta {
  producto: ProductoAdminListItem;
  variante?: VarianteResponse;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

@Component({
  selector: 'app-crear-venta-directa-form',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './crear-venta-directa-form.component.html',
  styleUrls: ['./crear-venta-directa-form.component.css']
})
export class CrearVentaDirectaFormComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  busquedaForm: FormGroup;
  productos: ProductoAdminListItem[] = [];
  loadingProductos = false;
  loadingInicial = true;
  paginaActual = 0;
  hayMas = true;

  variantes: VarianteResponse[] = [];
  loadingVariantes = false;

  itemsVenta: ItemVenta[] = [];
  totalVenta = 0;

  productoSeleccionado?: ProductoAdminListItem;
  varianteSeleccionada?: VarianteResponse;
  cantidad = 1;

  constructor(
    private fb: FormBuilder,
    private dropTownService: DropTownService,
    private boletaService: BoletaService
  ) {
    this.busquedaForm = this.fb.group({
      nombre: ['']
    });
  }

  ngOnInit(): void {
    this.cargarProductos();

    this.busquedaForm.get('nombre')?.valueChanges
      .pipe(
        debounceTime(400),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        this.resetBusqueda();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private resetBusqueda(): void {
    this.paginaActual = 0;
    this.productos = [];
    this.hayMas = true;
    this.cargarProductos();
  }

  cargarProductos(loadMore = false): void {
    if (this.loadingProductos) return;

    if (!loadMore) {
      this.paginaActual = 0;
      this.productos = [];
    } else {
      this.paginaActual++;
    }

    this.loadingProductos = true;

    const nombre = this.busquedaForm.get('nombre')?.value?.trim() || undefined;

    this.dropTownService.getProductosConStock(nombre, this.paginaActual, 20)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page: ProductoAdminListPage) => {
          const nuevos = page.content;
          this.productos = loadMore ? [...this.productos, ...nuevos] : nuevos;
          this.hayMas = !page.last;
          this.loadingProductos = false;
          this.loadingInicial = false;
        },
        error: () => {
          this.loadingProductos = false;
          this.loadingInicial = false;
        }
      });
  }

  onScroll(event: Event): void {
    const element = event.target as HTMLElement;
    if (element.scrollHeight - element.scrollTop <= element.clientHeight + 100) {
      if (this.hayMas && !this.loadingProductos) {
        this.cargarProductos(true);
      }
    }
  }

  scrollToCarrito(): void {
    document.querySelector('.sticky-top')?.scrollIntoView({ behavior: 'smooth' });
  }

seleccionarProducto(producto: ProductoAdminListItem): void {
  // Si es el mismo producto ya seleccionado, no hacer nada
  if (this.productoSeleccionado?.id === producto.id) {
    return;
  }

  this.productoSeleccionado = producto;
  this.varianteSeleccionada = undefined;
  this.cantidad = 1;
  this.variantes = [];

  // Si no tiene variantes → listo
  if (!producto.tieneVariantes) {
    return;
  }

  // Solo pedimos variantes para este producto específico
  this.loadingVariantes = true; // opcional: puedes agregar un spinner en el detalle

  this.dropTownService.getProductosConStock(
    this.busquedaForm.get('nombre')?.value?.trim() || undefined,
    0,           // página 0 (no importa mucho aquí)
    20,
    producto.id  // ← pedimos variantes solo de este
  ).subscribe({
    next: (page: ProductoAdminListPage) => {
      // EN LUGAR DE REEMPLAZAR TODA LA LISTA:
      // Solo actualizamos el producto específico en la lista actual
      const productoActualizado = page.content.find(p => p.id === producto.id);

      if (productoActualizado?.variantes && productoActualizado.variantes.length > 0) {
        // Actualizamos solo este producto en la lista (para mantener imágenes estables)
        this.productos = this.productos.map(p =>
          p.id === producto.id ? { ...p, variantes: productoActualizado.variantes } : p
        );

        // Cargamos las variantes filtradas
        this.variantes = productoActualizado.variantes
          .filter(v => v.activo && v.stock > 0);

        if (this.variantes.length === 1) {
          this.varianteSeleccionada = this.variantes[0];
        }
      }

      this.loadingVariantes = false;
    },
    error: (err) => {
      console.error('Error cargando variantes:', err);
      this.loadingVariantes = false;
    }
  });
}
  get stockMaximo(): number {
    return this.varianteSeleccionada?.stock || this.productoSeleccionado?.stockTotal || 1;
  }

  obtenerDescripcionVariante(variante: VarianteResponse): string {
    if (!variante.atributos?.length) return 'Variante estándar';
    return variante.atributos
      .map(a => `${a.atributoNombre}: ${a.valor}`)
      .join(' • ');
  }

  agregarAlCarrito(): void {
    if (!this.productoSeleccionado) return;

    const precio = this.varianteSeleccionada?.precio || this.productoSeleccionado.precioMinimo;
    const stockDisponible = this.stockMaximo;

    if (this.cantidad < 1 || this.cantidad > stockDisponible) {
      alert(`Cantidad inválida. Stock disponible: ${stockDisponible}`);
      return;
    }

    const item: ItemVenta = {
      producto: this.productoSeleccionado,
      variante: this.varianteSeleccionada,
      cantidad: this.cantidad,
      precioUnitario: precio,
      subtotal: precio * this.cantidad
    };

    const existenteIndex = this.itemsVenta.findIndex(i =>
      i.producto.id === item.producto.id &&
      (i.variante?.id ?? null) === (item.variante?.id ?? null)
    );

    if (existenteIndex !== -1) {
      this.itemsVenta[existenteIndex].cantidad += this.cantidad;
      this.itemsVenta[existenteIndex].subtotal += item.subtotal;
    } else {
      this.itemsVenta.push(item);
    }

    this.actualizarTotal();
    this.cantidad = 1; // Permite agregar más rápido del mismo
  }

  eliminarItem(index: number): void {
    this.itemsVenta.splice(index, 1);
    this.actualizarTotal();
  }

  private actualizarTotal(): void {
    this.totalVenta = this.itemsVenta.reduce((sum, item) => sum + item.subtotal, 0);
  }

 confirmarVenta(): void {
  if (this.itemsVenta.length === 0) {
    alert('El carrito está vacío');
    return;
  }

  const request: VentaDirectaRequest = {
    tiendaId: this.itemsVenta[0].producto.tiendaId,  // todos los productos son de la misma tienda
    compradorNombre: prompt('Nombre del cliente (opcional)')?.trim() || undefined,
    compradorEmail: null,
    compradorTelefono: null,
    items: this.itemsVenta.map(item => ({
      varianteId: item.variante?.id || item.producto.id,  // si no tiene variante, usa el producto base (ajustar según tu backend)
      cantidad: item.cantidad
    }))
  };

  // Si tu backend requiere variante siempre, asegúrate de que los productos sin variantes también tengan una variante por defecto
  // o ajusta el backend para aceptar productoId si no hay variante

  this.boletaService.crearVentaDirecta(request).subscribe({
    next: (boleta) => {
      // Éxito
      Swal.fire({
        icon: 'success',
        title: '¡Venta realizada!',
        text: `Boleta #${boleta.id} creada por S/ ${boleta.total.toFixed(2)}`,
        timer: 3000,
        showConfirmButton: false
      });

      // Limpiar carrito
      this.itemsVenta = [];
      this.totalVenta = 0;
      this.productoSeleccionado = undefined;
      this.variantes = [];
      this.cantidad = 1;

      // Opcional: redirigir o imprimir
      // this.router.navigate(['/admin/boletas', boleta.id]);
    },
    error: (err) => {
      Swal.fire({
        icon: 'error',
        title: 'Error al crear venta',
        text: err.message || 'Ocurrió un error inesperado'
      });
    }
  });
}
}