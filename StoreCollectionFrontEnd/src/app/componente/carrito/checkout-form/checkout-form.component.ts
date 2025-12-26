import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-checkout-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './checkout-form.component.html',
  styleUrls: ['./checkout-form.component.css'] // puedes dejar vacío si no usas CSS extra
})
export class CheckoutFormComponent implements OnInit {
  @Input() tiendaId!: number; // Obligatorio - pasar desde el padre
  @Input() prefillData?: Partial<any>; // Opcional - datos previos

  @Output() onSubmit = new EventEmitter<any>();
  @Output() onCancel = new EventEmitter<void>();

  checkoutForm!: FormGroup;
  isSubmitting = false;

  departamentos = [
    'Amazonas', 'Áncash', 'Apurímac', 'Arequipa', 'Ayacucho', 'Cajamarca',
    'Callao', 'Cusco', 'Huancavelica', 'Huánuco', 'Ica', 'Junín',
    'La Libertad', 'Lambayeque', 'Lima', 'Loreto', 'Madre de Dios',
    'Moquegua', 'Pasco', 'Piura', 'Puno', 'San Martín', 'Tacna',
    'Tumbes', 'Ucayali'
  ];

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.checkoutForm = this.fb.group({
      compradorNombre:    ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
      compradorEmail:     ['', [Validators.required, Validators.email, Validators.maxLength(120)]],
      compradorTelefono:  ['', [Validators.pattern(/^\+?\d{1,3}[-.\s]?\d{1,4}[-.\s]?\d{1,4}[-.\s]?\d{1,9}$/)]],

      direccionEnvio:     ['', [Validators.required, Validators.minLength(5), Validators.maxLength(150)]],
      referenciaEnvio:    ['', [Validators.maxLength(100)]],
      distrito:           ['', [Validators.required, Validators.minLength(2), Validators.maxLength(60)]],
      provincia:          ['', [Validators.required, Validators.minLength(2), Validators.maxLength(60)]],
      departamento:       ['', [Validators.required]],
      codigoPostal:       ['', [Validators.maxLength(10)]],

      tipoEntrega:        ['DOMICILIO', [Validators.required]]
    });

    // Prellenar si vienen datos
    if (this.prefillData) {
      this.checkoutForm.patchValue(this.prefillData);
    }
  }

  // ¡Esta es la línea clave que soluciona el error de 'f'!
  get f() {
    return this.checkoutForm.controls;
  }

  submit(): void {
    if (this.checkoutForm.invalid) {
      this.checkoutForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;

    const formData = {
      ...this.checkoutForm.value,
      tiendaId: this.tiendaId,
      sessionId: '', // Se llena en el servicio
      userId: null   // Opcional
    };

    this.onSubmit.emit(formData);
  }

  cancel(): void {
    this.onCancel.emit();
  }
}