// src/app/shared/table/table.component.ts
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-table',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './table.component.html',
  styleUrls: ['./table.component.css']
})
export class TableComponent {
  @Input() data: any[] = [];
  @Input() columns: { key: string; header: string }[] = [];
  @Output() edit = new EventEmitter<any>();
  @Output() toggle = new EventEmitter<any>();

  // Paginación falsa: solo visual, no corta los datos
  currentPage = 1;
  totalPages = 3; // Siempre muestra 3 páginas (falso, solo diseño)
}