// src/app/admin/products/products.component.ts
import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface Product {
  id: number;
  name: string;
  price: number;
  description: string;
  category: string;
  active: boolean;
}

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './products.component.html',
  styleUrls: ['./products.component.css']
})
export class ProductsComponent {
  products = signal<Product[]>([
    { id: 1, name: 'iPhone 15 Pro Max', price: 1599, description: 'Titanio, cámara 5x', category: 'Celulares', active: true },
    { id: 2, name: 'Camiseta Oversize Premium', price: 39.99, description: 'Algodón 100%, tallas S-3XL', category: 'Ropa', active: true },
    { id: 3, name: 'Hamburguesa Gourmet', price: 18.50, description: 'Carne Angus 200g + papas', category: 'Comida', active: false },
    { id: 4, name: 'AirPods Pro 2', price: 249, description: 'Cancelación activa de ruido', category: 'Accesorios', active: true },
    { id: 5, name: 'Zapatillas Nike Air', price: 149, description: 'Edición limitada', category: 'Ropa', active: true },
  ]);

  categories = ['Ropa', 'Celulares', 'Comida', 'Accesorios', 'Hogar', 'Belleza', 'Deportes'];

  showModal = false;
  editingProduct: Partial<Product> = {};

  openModal(product?: Product) {
    this.editingProduct = product ? { ...product } : {
      id: 0,
      name: '',
      price: 0,
      description: '',
      category: 'Ropa',
      active: true
    };
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
  }

  saveProduct() {
    if (!this.editingProduct.name ) return;

    if (this.editingProduct.id === 0) {
      // Nuevo producto
      const newId = Math.max(...this.products().map(p => p.id), 0) + 1;
      this.products.update(list => [...list, { ...this.editingProduct, id: newId } as Product]);
    } else {
      // Editar existente
      this.products.update(list =>
        list.map(p => p.id === this.editingProduct.id ? this.editingProduct as Product : p)
      );
    }
    this.closeModal();
  }

  toggleProduct(product: Product) {
    this.products.update(list =>
      list.map(p => p.id === product.id ? { ...p, active: !p.active } : p)
    );
  }
}