// src/app/core/services/swal.service.ts
import { Injectable } from '@angular/core';
import Swal, { SweetAlertIcon, SweetAlertResult } from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class SwalService {

  // ── Notificaciones básicas ──────────────────────────────────────────────

  success(title: string, text?: string): Promise<SweetAlertResult> {
    return Swal.fire({
      icon: 'success',
      title,
      text,
      confirmButtonText: '¡Genial!',
      confirmButtonColor: '#28a745',
      timer: 3500,
      timerProgressBar: true,
      showConfirmButton: false
    });
  }

  error(title: string, text?: string): Promise<SweetAlertResult> {
    return Swal.fire({
      icon: 'error',
      title,
      text,
      confirmButtonText: 'Entendido',
      confirmButtonColor: '#dc3545'
    });
  }

  warning(title: string, text?: string): Promise<SweetAlertResult> {
    return Swal.fire({
      icon: 'warning',
      title,
      text,
      confirmButtonText: 'Entendido',
      confirmButtonColor: '#fd7e14'
    });
  }

  info(title: string, text?: string): Promise<SweetAlertResult> {
    return Swal.fire({
      icon: 'info',
      title,
      text,
      confirmButtonText: 'Aceptar',
      confirmButtonColor: '#0d6efd'
    });
  }

  // ── Confirmaciones ──────────────────────────────────────────────────────

  confirmDelete(
    itemName: string = 'este elemento',
    extraText?: string
  ): Promise<SweetAlertResult> {
    return Swal.fire({
      title: '¿Eliminar?',
      html: `¿Realmente deseas eliminar <b>${itemName}</b>?<br>
             ${extraText || 'Esta acción no se puede deshacer.'}`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
      reverseButtons: true
    });
  }

  confirmAction({
    title = '¿Estás seguro?',
    text = 'Esta acción no se puede deshacer',
    confirmButtonText = 'Sí, continuar',
    cancelButtonText = 'Cancelar',
    icon = 'question' as SweetAlertIcon
  } = {}): Promise<SweetAlertResult> {
    return Swal.fire({
      title,
      text,
      icon,
      showCancelButton: true,
      confirmButtonColor: '#3085d6',
      cancelButtonColor: '#d33',
      confirmButtonText,
      cancelButtonText,
      reverseButtons: true
    });
  }

  // ── Utilidades adicionales ──────────────────────────────────────────────

  /** Muestra un toast (notificación flotante pequeña) */
  toast(
    message: string,
    icon: SweetAlertIcon = 'success',
    timer = 3000
  ): Promise<SweetAlertResult> {
    return Swal.fire({
      toast: true,
      position: 'top-end',
      icon,
      title: message,
      showConfirmButton: false,
      timer,
      timerProgressBar: true,
      didOpen: (toast) => {
        toast.addEventListener('mouseenter', Swal.stopTimer);
        toast.addEventListener('mouseleave', Swal.resumeTimer);
      }
    });
  }

  /** Loading / Esperando... */
  loading(title = 'Procesando...', text?: string) {
    return Swal.fire({
      title,
      text,
      allowOutsideClick: false,
      allowEscapeKey: false,
      didOpen: () => {
        Swal.showLoading();
      }
    });
  }

  /** Cierra cualquier swal abierto (útil para loading) */
  close(): void {
    Swal.close();
  }


}