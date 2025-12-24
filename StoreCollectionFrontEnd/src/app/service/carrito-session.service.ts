import { Injectable } from '@angular/core';

const SESSION_KEY = 'carrito_session_id';

@Injectable({
  providedIn: 'root'
})
export class CarritoSessionService {

  getSessionId(): string {
    let sessionId = localStorage.getItem(SESSION_KEY);
    if (!sessionId) {
      sessionId = crypto.randomUUID();
      localStorage.setItem(SESSION_KEY, sessionId);
    }
    return sessionId;
  }

  clearSession(): void {
    localStorage.removeItem(SESSION_KEY);
  }

  regenerateSession(): string {
    const newId = crypto.randomUUID();
    localStorage.setItem(SESSION_KEY, newId);
    return newId;
  }
}