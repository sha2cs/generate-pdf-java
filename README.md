# generate-pdf-java

Librería Java para generar documentos **PDF** sencillos: título, imagen PNG o JPG, campos con formato «Etiqueta: valor» y párrafos.

Una sola clase, `com.sha2cs.generatepdf.DocumentoPDF`, sin dependencias externas.

Complementa a [inputfile-java](https://github.com/sha2cs/inputfile-java): esa librería selecciona la foto; esta la incluye en el PDF.

## Requisitos

- JDK 11 o superior.
- Proyecto Java (consola o Swing).
- Ninguna otra librería: generate-pdf-java no tiene dependencias.
- Sistema operativo: Windows o Linux. Las instrucciones indican la diferencia donde la hay.

Versión del JDK instalada:

| Sistema | Dónde | Comando |
|---|---|---|
| Windows | **Símbolo del sistema** o **PowerShell** (tecla Windows → escribir `cmd`) | `java -version` |
| Linux | **Terminal** (`Ctrl+Alt+T`) | `java -version` |

La primera línea de la salida debe indicar versión 11 o superior (por ejemplo `openjdk version "21.0.2"`).

## 1. Descarga del `.jar`

1. Abrir el archivo [`dist/generate-pdf-java.jar`](dist/generate-pdf-java.jar) en este repositorio.
2. Pulsar el botón **Download raw file** (ícono de descarga, a la derecha).

Ubicación del archivo descargado:

| Sistema | Carpeta |
|---|---|
| Windows | `C:\Users\<usuario>\Downloads` (en el **Explorador de archivos**: **Descargas**) |
| Linux | `~/Descargas` o `~/Downloads`, según el idioma del sistema |

## 2. Instalación en IntelliJ IDEA

### 2.1. Carpeta `lib`

1. En el panel **Project**, clic derecho sobre la carpeta raíz del proyecto → **New → Directory** → nombre `lib`.
2. Copiar `generate-pdf-java.jar` dentro de `lib/`. Cualquiera de estas formas sirve:
   - **Arrastrar** el archivo desde la carpeta de descargas hasta la carpeta `lib` en el panel **Project** de IntelliJ.
   - **Copiar y pegar:** `Ctrl+C` sobre el archivo en la carpeta de descargas, clic sobre `lib` en IntelliJ y `Ctrl+V`.
   - **Desde el gestor de archivos del sistema:** clic derecho sobre `lib` en IntelliJ → **Open In** → **Explorer** (Windows) o **Files** / **Directory** (Linux). Se abre la carpeta `lib` y el `.jar` se pega allí.

Los atajos de teclado de IntelliJ indicados en esta guía son los mismos en Windows y en Linux.

### 2.2. Registro de la librería

**Opción A (la más corta):**

1. Clic derecho sobre `lib/generate-pdf-java.jar` → **Add as Library…**
2. En **Level** dejar **Project Library** y en **Add to module** el módulo del proyecto → **OK**.

**Opción B (desde Project Structure):**

1. **File → Project Structure…** (`Ctrl+Alt+Shift+S`).
2. **Libraries → + → Java**.
3. Seleccionar `lib/generate-pdf-java.jar` → **OK**.
4. Confirmar el módulo al que se agrega → **OK** → **Apply** → **OK**.

### 2.3. Verificación

La librería queda instalada si el siguiente `import` no se marca en rojo:

```java
import com.sha2cs.generatepdf.DocumentoPDF;
```

La librería aparece además en el panel **Project → External Libraries → generate-pdf-java**.

### 2.4. Repositorio del proyecto

La carpeta `lib/` con el `.jar` **se sube al repositorio** del proyecto. Sin ella, el proyecto no compila en otro computador.

## 3. Métodos

| Método | Resultado |
|---|---|
| `new DocumentoPDF(titulo)` | Crea el documento. El título encabeza la primera página. |
| `.imagen(ruta)` | Agrega una imagen PNG o JPG a partir de su ruta en texto. |
| `.imagen(archivo)` | Igual que el anterior, a partir de un `File`. |
| `.campo(etiqueta, valor)` | Agrega una línea «Etiqueta: valor», con la etiqueta en negrita. El valor puede ser texto, número o `null` (se escribe vacío). |
| `.parrafo(texto)` | Agrega un párrafo. Las líneas largas se ajustan al ancho de la página. |
| `.guardar(ruta)` | Genera el archivo PDF y devuelve el `File` creado. Lanza `IOException` si falla. |

Uso general, en tres momentos:

```java
DocumentoPDF documento = new DocumentoPDF("Título");   // 1. se crea el objeto
documento.campo("Etiqueta", "valor");                    // 2. se le agrega contenido
documento.guardar("archivo.pdf");                        // 3. se genera el archivo
```

Los elementos aparecen en el PDF **en el mismo orden en que se agregan**: si `imagen(...)` se llama antes que los campos, la foto queda arriba.

## 4. Paso a paso: PDF de un objeto `Usuario`

El caso de uso: una ventana `FormDetalle` recibe un objeto `Usuario` y un botón **Generar PDF** crea un PDF con la foto y la información de ese usuario, **excepto la contraseña**.

### 4.1. Clase `Usuario`

El PDF se construye a partir de los `get` del objeto. Atributos de referencia:

```java
public class Usuario {

    private int id;
    private String tipoIdentificacion;
    private String numeroIdentificacion;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private String username;
    private String password;
    private String telefono;
    private String correo;
    private String rutaFoto;

    // constructor, get y set
}
```

### 4.2. Ventana que recibe el usuario

`FormDetalle` guarda en un atributo el usuario que recibe por el constructor:

```java
public class FormDetalle extends javax.swing.JFrame {

    private Usuario usuario;

    public FormDetalle(Usuario usuario) {
        initComponents();
        this.usuario = usuario;
    }
}
```

### 4.3. Botón Generar PDF

1. En el diseñador de IntelliJ, agregar un `JButton` con el texto **Generar PDF** y field name `btnGenerarPdf`.
2. Asociar el evento: clic derecho sobre el botón → **Create Listener** → **ActionListener**, o en código con `btnGenerarPdf.addActionListener(e -> generarPdf());`.

### 4.4. Imports

Al inicio de `FormDetalle.java`:

```java
import com.sha2cs.generatepdf.DocumentoPDF;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import javax.swing.JOptionPane;
```

### 4.5. Método que genera el PDF

```java
private void generarPdf() {
    try {
        // 1. Crear el documento con su título
        DocumentoPDF documento = new DocumentoPDF("Ficha de usuario");

        // 2. Agregar la foto (queda arriba porque se agrega primero)
        documento.imagen(usuario.getRutaFoto());

        // 3. Agregar un campo por cada atributo, excepto la contraseña
        documento.campo("Id", usuario.getId());
        documento.campo("Tipo de identificación", usuario.getTipoIdentificacion());
        documento.campo("Número de identificación", usuario.getNumeroIdentificacion());
        documento.campo("Primer nombre", usuario.getPrimerNombre());
        documento.campo("Segundo nombre", usuario.getSegundoNombre());
        documento.campo("Primer apellido", usuario.getPrimerApellido());
        documento.campo("Segundo apellido", usuario.getSegundoApellido());
        documento.campo("Username", usuario.getUsername());
        documento.campo("Teléfono", usuario.getTelefono());
        documento.campo("Correo", usuario.getCorreo());

        // 4. Generar el archivo PDF
        File pdf = documento.guardar("usuario-" + usuario.getId() + ".pdf");

        // 5. Avisar y abrir el PDF
        JOptionPane.showMessageDialog(this, "PDF generado: " + pdf.getAbsolutePath());
        Desktop.getDesktop().open(pdf);

    } catch (IOException e) {
        JOptionPane.showMessageDialog(this, "No se pudo generar el PDF: " + e.getMessage());
    }
}
```

Explicación línea por línea:

| Línea | Qué hace |
|---|---|
| `DocumentoPDF documento = new DocumentoPDF("Ficha de usuario");` | Crea el objeto `documento`. El texto entre paréntesis es el título del PDF. Todavía no existe ningún archivo. |
| `documento.imagen(usuario.getRutaFoto());` | Agrega la foto a partir de la ruta guardada en el usuario. Se escala a un máximo de 160 × 200 puntos sin deformarse. |
| `documento.campo("Id", usuario.getId());` | Agrega la línea «**Id:** 15». El primer valor es la etiqueta; el segundo, el dato. Acepta texto o números. |
| `documento.campo("Segundo nombre", usuario.getSegundoNombre());` | Si el atributo es `null` o vacío, la etiqueta aparece con el valor en blanco. El programa no falla. |
| — | `getPassword()` **no se llama**: la contraseña no va en el PDF. |
| `File pdf = documento.guardar("usuario-" + usuario.getId() + ".pdf");` | **Aquí se crea el archivo**, por ejemplo `usuario-15.pdf`, en la carpeta del proyecto. Devuelve un `File` con la ubicación del archivo. |
| `JOptionPane.showMessageDialog(...)` | Muestra la ruta completa del PDF generado. |
| `Desktop.getDesktop().open(pdf);` | Abre el PDF con el visor predeterminado del sistema (Windows o Linux). |
| `try { ... } catch (IOException e) { ... }` | `guardar(...)` exige `try` / `catch`. El `catch` se ejecuta si la foto ya no existe en esa ruta, si no es una imagen o si el archivo no se puede escribir. |

### 4.6. Prueba

1. Ejecutar el programa, registrar un usuario con foto y abrir su detalle.
2. Pulsar **Generar PDF**.
3. Resultado esperado: el mensaje con la ruta del archivo y el PDF abierto en el visor, con el título, la foto y los datos del usuario, sin la contraseña.

### 4.7. Variantes

**Nombre completo en un solo campo**, omitiendo los nombres opcionales vacíos:

```java
String nombre = usuario.getPrimerNombre();
if (usuario.getSegundoNombre() != null && !usuario.getSegundoNombre().isEmpty()) {
    nombre = nombre + " " + usuario.getSegundoNombre();
}
nombre = nombre + " " + usuario.getPrimerApellido();
if (usuario.getSegundoApellido() != null && !usuario.getSegundoApellido().isEmpty()) {
    nombre = nombre + " " + usuario.getSegundoApellido();
}

documento.campo("Nombre completo", nombre);
```

**Usuario sin foto:** `imagen(...)` lanza error si la ruta no existe. Para generar el PDF igualmente, la foto se agrega solo cuando hay ruta:

```java
DocumentoPDF documento = new DocumentoPDF("Ficha de usuario");
if (usuario.getRutaFoto() != null && new File(usuario.getRutaFoto()).isFile()) {
    documento.imagen(usuario.getRutaFoto());
}
documento.campo("Id", usuario.getId());
// ... resto de campos
documento.guardar("usuario-" + usuario.getId() + ".pdf");
```

**Carpeta de salida propia** (por ejemplo `pdf/`), creada si no existe:

```java
File carpeta = new File("pdf");
carpeta.mkdirs();
documento.guardar(new File(carpeta, "usuario-" + usuario.getId() + ".pdf"));
```

### 4.8. Forma encadenada (opcional)

Los métodos `imagen`, `campo` y `parrafo` terminan con `return this;`, es decir, **devuelven el mismo documento**. Por eso se puede llamar el siguiente método directamente sobre el resultado del anterior, sin repetir el nombre de la variable:

```java
File pdf = new DocumentoPDF("Ficha de usuario")
        .imagen(usuario.getRutaFoto())
        .campo("Id", usuario.getId())
        .campo("Correo", usuario.getCorreo())
        .guardar("usuario-" + usuario.getId() + ".pdf");
```

El resultado es **idéntico** al del paso 4.5. Es la misma técnica de `StringBuilder`: `sb.append("a").append("b")`.

La forma paso a paso del 4.5 es la recomendada: se lee una instrucción por línea y permite poner un `if` en la mitad, como en la variante «Usuario sin foto».

### 4.9. Repositorio del proyecto

Los PDF generados **no se suben al repositorio**. Se excluyen agregando esta línea al archivo `.gitignore` del proyecto:

```
*.pdf
```

## 5. Formato del documento

| Aspecto | Valor |
|---|---|
| Tamaño de página | A4, márgenes de 2 cm |
| Fuente | Helvetica: título 18 pt en negrita, texto 11 pt |
| Caracteres | Tildes, eñes, `¿`, `¡` y `€`. Caracteres fuera del alfabeto latino (emojis, chino, etc.) aparecen como `?` |
| Imágenes | PNG o JPG, máximo 160 × 200 puntos, con su proporción original. La transparencia de un PNG queda en blanco |
| Páginas | Salto de página automático cuando el contenido no cabe |

## 6. Errores frecuentes

| Síntoma | Causa | Solución |
|---|---|---|
| `package com.sha2cs.generatepdf does not exist` | La librería no está registrada en el módulo. | Repetir el paso 2.2. |
| `unreported exception java.io.IOException; must be caught or declared to be thrown` | `guardar(...)` se llamó fuera de un `try` / `catch`. | Envolver el código como en el paso 4.5. |
| `No se encontró la imagen: ...` | La ruta de la foto ya no existe (archivo movido o borrado). | Ver la variante «Usuario sin foto» en 4.7. |
| `El archivo no es una imagen PNG o JPG: ...` | El archivo tiene extensión de imagen pero no lo es. | Elegir otra imagen. |
| `(Access is denied)` en Windows o `(Permission denied)` en Linux | El PDF está abierto en el visor mientras se vuelve a generar, o la carpeta no permite escribir. | Cerrar el PDF y generar de nuevo. |
| `(The system cannot find the path specified)` / `(No such file or directory)` | La carpeta de destino no existe. | Crearla con `mkdirs()` (ver 4.7). |
| El proyecto compila en un equipo y no en otro | La carpeta `lib/` no se subió al repositorio. | Subir `lib/generate-pdf-java.jar`. |

## 7. Compilación desde el código fuente

| Sistema | Dónde | Comando |
|---|---|---|
| Windows | **Símbolo del sistema** abierto en la carpeta del repositorio | `build.bat` |
| Linux | **Terminal** abierta en la carpeta del repositorio | `./build.sh` |

Genera `dist/generate-pdf-java.jar`, que incluye las clases compiladas y el código fuente (IntelliJ muestra la documentación de cada método al usarlo).
