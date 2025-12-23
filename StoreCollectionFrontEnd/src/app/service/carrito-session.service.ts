// src/app/service/carrito-session.service.ts

import { Injectable } from '@angular/core';
import { v4 as uuidv4 } from 'uuid';

const SESSION_KEY = 'carrito_session_id';

@Injectable({
  providedIn: 'root'
})
export class CarritoSessionService {

  getSessionId(): string {
    let sessionId = localStorage.getItem(SESSION_KEY);
    if (!sessionId) {
      sessionId = uuidv4();
      localStorage.setItem(SESSION_KEY, sessionId);
    }
    return sessionId;
  }

  clearSession(): void {
    localStorage.removeItem(SESSION_KEY);
  }

  regenerateSession(): string {
    const newId = uuidv4();
    localStorage.setItem(SESSION_KEY, newId);
    return newId;
  }
}