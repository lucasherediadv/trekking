import { setupServer } from "msw/node";

// API simulada en la frontera HTTP. Cada test agrega sus handlers con server.use(...).
export const server = setupServer();
