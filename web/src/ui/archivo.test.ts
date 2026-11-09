import { errorDeArchivo, LIMITE_ARCHIVO_BYTES, leerBase64 } from "./archivo";

describe("archivo", () => {
  it("lee el contenido en base64 sin prefijo data:", async () => {
    expect(await leerBase64(new File(["hola"], "a.txt"))).toBe("aG9sYQ==");
  });

  it("acepta hasta 10 MB y rechaza más", () => {
    const con = (size: number) => Object.defineProperty(new File([], "a.pdf"), "size", { value: size });
    expect(errorDeArchivo(con(LIMITE_ARCHIVO_BYTES))).toBeUndefined();
    expect(errorDeArchivo(con(LIMITE_ARCHIVO_BYTES + 1))).toBe("El archivo supera el máximo de 10 MB.");
  });
});
