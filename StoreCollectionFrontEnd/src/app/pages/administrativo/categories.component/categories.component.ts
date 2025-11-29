// src/app/admin/categories/categories.component.ts
import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface Category {
  id: number;
  name: string;
  active: boolean;
}

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './categories.component.html',
  styleUrls: ['./categories.component.css']
})
export class CategoriesComponent {
  categories = signal<Category[]>([
    { id: 1, name: 'Ropa', active: true },
    { id: 2, name: 'Celulares', active: true },
    { id: 3, name: 'Belleza', active: true },
    { id: 4, name: 'Accesorios', active: true },
    { id: 5, name: 'Hogar', active: true },
    { id: 6, name: 'Comida', active: false },
    { id: 7, name: 'Deportes', active: true },
    { id: 8, name: 'Juguetes', active: false },
  ]);

  showModal = false;
  newCategory = { name: '' };

  openModal() {
    this.newCategory.name = '';
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
  }

  saveCategory() {
    if (!this.newCategory.name.trim()) return;

    const newId = Math.max(...this.categories().map(c => c.id), 0) + 1;
    this.categories.update(list => [...list, {
      id: newId,
      name: this.newCategory.name.trim(),
      active: true
    }]);

    this.closeModal();
  }

  toggle(cat: Category) {
    this.categories.update(list =>
      list.map(c => c.id === cat.id ? { ...c, active: !c.active } : c)
    );
  }
}