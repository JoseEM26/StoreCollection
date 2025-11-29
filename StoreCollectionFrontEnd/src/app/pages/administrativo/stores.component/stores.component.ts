// src/app/admin/stores/stores.component.ts
import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface Store {
  id: number;
  name: string;
  domain: string;
  active: boolean;
}

@Component({
  selector: 'app-stores',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './stores.component.html',
  styleUrl: './stores.component.css'
})
export class StoresComponent {
  stores = signal<Store[]>([
    { id: 1, name: 'Ropita TikTok', domain: 'ropita.storecollection.com', active: true },
    { id: 2, name: 'Gadgets Pro', domain: 'gadgets.storecollection.com', active: true },
    { id: 3, name: 'Belleza Viral', domain: 'bellezaviral.storecollection.com', active: true },
    { id: 4, name: 'Comida Rica', domain: 'comidarica.storecollection.com', active: false },
    { id: 5, name: 'Tech Store', domain: 'tech.storecollection.com', active: true },
    { id: 6, name: 'Moda Joven', domain: 'modajoven.storecollection.com', active: false },
  ]);

  showModal = false;
  newStore = { name: '', domain: '' };

  openModal() {
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.newStore = { name: '', domain: '' };
  }

  createStore() {
    if (!this.newStore.name.trim() || !this.newStore.domain.trim()) return;

    const newId = Math.max(...this.stores().map(s => s.id), 0) + 1;
    this.stores.update(list => [...list, {
      id: newId,
      name: this.newStore.name,
      domain: this.newStore.domain,
      active: true
    }]);

    this.closeModal();
  }

  toggleStore(store: Store) {
    this.stores.update(list =>
      list.map(s => s.id === store.id ? { ...s, active: !s.active } : s)
    );
  }
}