import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { mensajeDe } from "../api/errores";

interface Toast {
  id: number;
  texto: string;
  tipo: "exito" | "error";
}

interface Notificador {
  exito: (texto: string) => void;
  error: (causa: unknown) => void;
}

const Contexto = createContext<Notificador | null>(null);

export function useToast(): Notificador {
  const n = useContext(Contexto);
  if (!n) throw new Error("useToast requiere ToastProvider");
  return n;
}

const DURACION_MS = 4000;
let siguienteId = 0;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toast, setToast] = useState<Toast | null>(null);

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(null), DURACION_MS);
    return () => clearTimeout(t);
  }, [toast]);

  const notificador = useMemo<Notificador>(
    () => ({
      exito: (texto) => setToast({ id: ++siguienteId, texto, tipo: "exito" }),
      error: (causa) => setToast({ id: ++siguienteId, texto: mensajeDe(causa), tipo: "error" }),
    }),
    [],
  );

  return (
    <Contexto.Provider value={notificador}>
      {children}
      {toast && (
        <div key={toast.id} role="status" aria-label="Notificación" className={`toast toast-${toast.tipo}`}>
          {toast.texto}
        </div>
      )}
    </Contexto.Provider>
  );
}
