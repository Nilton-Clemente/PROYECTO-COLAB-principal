# Evidencias del administrador de Django

- **Proyecto:** Colab
- **Integrante:** ______________________________
- **Fecha:** ______________________________
**Rama o versión evaluada:** ______________________________

## Indicaciones

Esta plantilla reúne las evidencias del administrador nativo de Django, disponible en `http://localhost:8000/admin/` cuando el servidor está activo. Abre este archivo en un editor de Markdown e inserta cada captura en el espacio correspondiente. Por ejemplo, si guardas las imágenes dentro de una carpeta `capturas` junto a este documento:

```markdown
![Descripción de la captura](capturas/nombre-de-la-imagen.png)
```

Usa datos de prueba y evita mostrar contraseñas, tokens o información personal real. Verifica que se vean el título de la pantalla, los datos relevantes y, cuando corresponda, el término buscado o el filtro seleccionado.

## Preparación

1. Inicia PostgreSQL y `backend-usuario` para contar con las tablas gestionadas por Flyway. En `/Users/niltonjavier/Desktop/PROYECTO COLAB (NEW)/Proyecto-Colab/backend-admin`, configura las variables de entorno, aplica las migraciones Django y crea el administrador si aún no existe.
2. Si necesitas datos de ejemplo, ejecuta `python manage.py seed_datos` en ese mismo directorio. El comando carga habilidades y tecnologías; **no crea usuarios, proyectos ni asociaciones**.
3. Inicia Django con `python manage.py runserver` y accede a `/admin/` con la cuenta administrativa.
4. Para evidenciar una relación no vacía, asegúrate de que exista una habilidad asignada a un usuario o una tecnología asociada a un proyecto mediante los flujos correspondientes del proyecto.

## 1. Acceso al administrador

**Objetivo:** mostrar que una cuenta administrativa ha ingresado al panel.

**Captura:** página inicial de `/admin/` después de iniciar sesión, con los modelos de la aplicación visibles. No captures la contraseña.

> **Espacio para insertar la captura 1 — Inicio del administrador**

## 2. Modelo Habilidad

### 2.1 Lista

**Captura:** listado de habilidades con las columnas ID, nombre, área y usuarios asignados. Incluye los registros de prueba si están cargados.

> **Espacio para insertar la captura 2 — Lista de habilidades**

### 2.2 Formulario de alta

**Captura:** selecciona «Añadir» en Habilidades y muestra los campos nombre y área. Puedes completar el formulario con un ejemplo sin necesidad de guardar datos adicionales.

> **Espacio para insertar la captura 3 — Alta de habilidad**

### 2.3 Búsqueda o filtro

**Captura:** busca una habilidad por nombre o selecciona un área en el filtro lateral. Deben verse el criterio aplicado y los resultados.

> **Espacio para insertar la captura 4 — Búsqueda o filtro de habilidades**

### 2.4 Relación de solo lectura

**Captura:** abre el detalle de una habilidad y muestra el campo «Usuarios asignados». Para demostrar una relación con datos, selecciona una habilidad que ya tenga un usuario asociado.

> **Espacio para insertar la captura 5 — Usuarios asociados a una habilidad**

**Dato de prueba utilizado:** __________________________________________

## 3. Modelo Tecnología

### 3.1 Lista

**Captura:** listado de tecnologías con ID, nombre y proyectos asociados.

> **Espacio para insertar la captura 6 — Lista de tecnologías**

### 3.2 Formulario de alta

**Captura:** selecciona «Añadir» en Tecnologías y muestra el campo nombre.

> **Espacio para insertar la captura 7 — Alta de tecnología**

### 3.3 Búsqueda

**Captura:** busca por nombre, por ejemplo `React`, mostrando el texto de búsqueda y los resultados.

> **Espacio para insertar la captura 8 — Búsqueda de tecnologías**

### 3.4 Relación de solo lectura (opcional si ya se presentó la captura 5)

**Captura:** abre una tecnología asociada a un proyecto y muestra el campo «Proyectos asociados».

> **Espacio para insertar la captura 9 — Proyectos asociados a una tecnología**

**Dato de prueba utilizado:** __________________________________________

## 4. Modelo Usuario

### 4.1 Lista

**Captura:** listado de usuarios con ID, correo, nombre, fecha de registro y estado de bloqueo. Usa cuentas de prueba.

> **Espacio para insertar la captura 10 — Lista de usuarios**

### 4.2 Búsqueda o filtro

**Captura:** busca por nombre o correo, o aplica el filtro «bloqueada». Deja visibles el criterio y los resultados.

> **Espacio para insertar la captura 11 — Búsqueda o filtro de usuarios**

### 4.3 Formulario de alta: limitación actual

El modelo `Usuario` registrado en el admin de Django **no permite altas**; su consulta está configurada de solo lectura y la creación de cuentas corresponde a `backend-usuario`. Por ello, actualmente no existe una captura de alta de este modelo dentro de `/admin/`.

Si la evaluación exige un formulario de alta **por cada modelo y dentro del admin nativo**, este punto queda pendiente. Consulta si se acepta como evidencia alternativa el formulario real de registro de `web-usuario`; no lo presentes como si fuera parte del admin de Django.

> **Espacio opcional para evidencia alternativa de alta de usuario, si el evaluador la acepta**

## Lista de comprobación antes de entregar

- [ ] Las listas de Habilidad, Tecnología y Usuario muestran datos de prueba.
- [ ] Se muestra el formulario de alta de Habilidad y de Tecnología.
- [ ] Al menos una captura muestra una búsqueda o filtro aplicado y sus resultados.
- [ ] Al menos una captura muestra una relación real no vacía y de solo lectura.
- [ ] Se explica la ausencia de alta de Usuario en el admin y se confirma con el evaluador si se requiere una solución adicional.
- [ ] Las capturas no contienen secretos ni datos personales reales.