// src/app/directives/global-image-fallback.directive.ts
import { Directive, ElementRef, HostListener } from '@angular/core';

@Directive({
  selector: 'img',                    // ← ¡Aplica a TODAS las <img> automáticamente!
  standalone: true
})
export class GlobalImageFallbackDirective {
  private readonly FALLBACK = 'https://res.cloudinary.com/dqznlmig0/image/upload/v1767658215/imagen_2026-01-05_191004692_bepdxz.png';
  private applied = false;

  constructor(private el: ElementRef<HTMLImageElement>) {}

  @HostListener('error')
  onError() {
    if (this.applied) return;  // Evita bucle infinito

    this.applied = true;
    this.el.nativeElement.src = this.FALLBACK;
    this.el.nativeElement.alt = this.el.nativeElement.alt || 'Imagen no disponible';
  }
}