// src/app/pages/publico/producto-unitario/producto-unitario.component.ts
import { Component, inject, AfterViewInit, Renderer2, ElementRef, OnDestroy } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ProductosService } from '../../../service/productos.service';
import { Producto } from '../../../model/producto.model';

@Component({
  selector: 'app-producto-unitario',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './producto-unitario.component.html',
  styleUrls: ['./producto-unitario.component.css']
})
export class ProductoUnitarioComponent implements AfterViewInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private service = inject(ProductosService);
  private renderer = inject(Renderer2);
  private el = inject(ElementRef);

  producto: Producto | undefined;
  private listeners: (() => void)[] = [];

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.producto = this.service.getById(id);
  }

  get hayStock(): boolean {
    return this.producto ? this.producto.stock > 0 : false;
  }

  ngAfterViewInit(): void {
    const container = this.el.nativeElement.querySelector('.imagen-zoom-container') as HTMLElement;
    const img = container?.querySelector('#imgZoom') as HTMLImageElement;
    const lupa = container?.querySelector('#lupa') as HTMLElement;

    if (!container || !img || !lupa) return;

    let hovering = false;

    const moveHandler = (e: MouseEvent) => {
      if (!hovering) return;

      const rect = container.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;

      const xPercent = (x / rect.width) * 100;
      const yPercent = (y / rect.height) * 100;

      this.renderer.setStyle(lupa, 'left', `${x}px`);
      this.renderer.setStyle(lupa, 'top', `${y}px`);
      this.renderer.setStyle(lupa, 'backgroundImage', `url(${img.src})`);
      this.renderer.setStyle(lupa, 'backgroundSize', `${rect.width * 3}px ${rect.height * 3}px`);
      this.renderer.setStyle(lupa, 'backgroundPosition', `${xPercent}% ${yPercent}%`);
    };

    const enterHandler = () => {
      hovering = true;
      this.renderer.setStyle(lupa, 'opacity', '1');
    };

    const leaveHandler = () => {
      hovering = false;
      this.renderer.setStyle(lupa, 'opacity', '0');
    };

    this.listeners = [
      this.renderer.listen(container, 'mousemove', moveHandler),
      this.renderer.listen(container, 'mouseenter', enterHandler),
      this.renderer.listen(container, 'mouseleave', leaveHandler)
    ];
  }

  ngOnDestroy(): void {
    this.listeners.forEach(unlisten => unlisten());
  }
}