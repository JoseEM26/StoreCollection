import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TiendaResponse } from '../../../../model/admin/tienda-admin.model';
import { TiendaAdminService, TiendaCreateRequest } from '../../../../service/service-admin/tienda-admin.service';
import { lastValueFrom } from 'rxjs';
import { DropTownService, DropTownStandar } from '../../../../service/droptown.service';

@Component({
  selector: 'app-form-stores',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './form-stores.component.html',
  styleUrl: './form-stores.component.css'
})
export class FormStoresComponent implements OnInit, OnChanges {
  @Input() tienda?: TiendaResponse;
  @Output() success = new EventEmitter<TiendaResponse>();
  @Output() cancel = new EventEmitter<void>();

  isEditMode = false;
  loading = false;
  logoPreview: string | null = null;

  usuarios: DropTownStandar[] = [];
  usuariosLoading = false;

  form: TiendaCreateRequest = {
    nombre: '',
    slug: '',
    whatsapp: '+51',
    moneda: 'SOLES',
    descripcion: '',
    direccion: '',
    horarios: 'Lun - Sáb 9:00 - 21:00',
    mapa_url: '',
    logo_img_url: '',
    planId: 1,
    userId: 0 // será seleccionado del dropdown
  };

  planes = [
    { id: 1, nombre: 'Básico' },
    { id: 2, nombre: 'Pro' },
    { id: 3, nombre: 'Enterprise' }
  ];

  constructor(
    private tiendaService: TiendaAdminService,
    private dropTownService: DropTownService
  ) {}

  ngOnInit(): void {
    this.cargarUsuarios();
    this.resetForm();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['tienda'] && this.tienda) {
      this.isEditMode = true;
      this.form = {
        nombre: this.tienda.nombre,
        slug: this.tienda.slug,
        whatsapp: this.tienda.whatsapp || '+51',
        moneda: this.tienda.moneda as any,
        descripcion: this.tienda.descripcion || '',
        direccion: this.tienda.direccion || '',
        horarios: this.tienda.horarios || '',
        mapa_url: this.tienda.mapa_url || '',
        logo_img_url: this.tienda.logo_img_url || '',
        planId: this.tienda.planId || 1,
        userId: this.tienda.userId || 0
      };
      this.logoPreview = this.tienda.logo_img_url || null;
    } else {
      this.isEditMode = false;
      this.resetForm();
    }
  }

  private cargarUsuarios() {
    this.usuariosLoading = true;
    this.dropTownService.getUsuarios().subscribe({
      next: (data) => {
        this.usuarios = data;
        this.usuariosLoading = false;
      },
      error: (err) => {
        console.error('Error cargando usuarios:', err);
        alert('No se pudieron cargar los usuarios');
        this.usuariosLoading = false;
      }
    });
  }

  resetForm() {
    this.form = {
      nombre: '',
      slug: '',
      whatsapp: '+51',
      moneda: 'SOLES',
      descripcion: '',
      direccion: '',
      horarios: 'Lun - Sáb 9:00 - 21:00',
      mapa_url: '',
      logo_img_url: '',
      planId: 1,
      userId: 0
    };
    this.logoPreview = null;
  }

  onNombreChange() {
    if (!this.isEditMode) {
      this.form.slug = this.tiendaService.generarSlug(this.form.nombre);
    }
  }

  onLogoUrlChange() {
    this.logoPreview = this.form.logo_img_url || null;
  }

  async onSubmit() {
    if (!this.form.nombre.trim() || !this.form.slug.trim()) {
      alert('Nombre y slug son obligatorios');
      return;
    }

    if (this.form.userId === 0) {
      alert('Debe seleccionar un usuario');
      return;
    }

    this.loading = true;

    try {
      let resultado: TiendaResponse;

      if (this.isEditMode && this.tienda?.id) {
        resultado = await lastValueFrom(
          this.tiendaService.actualizarTienda(this.tienda.id, this.form)
        );
        alert('Tienda actualizada correctamente');
      } else {
        resultado = await lastValueFrom(
          this.tiendaService.crearTienda(this.form)
        );
        alert('Tienda creada exitosamente');
      }

      this.success.emit(resultado);
    } catch (err: any) {
      console.error('Error:', err);
      alert(err.message || 'Error al guardar la tienda');
    } finally {
      this.loading = false;
    }
  }

  onCancel() {
    this.cancel.emit();
  }
}