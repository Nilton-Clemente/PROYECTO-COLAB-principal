# Iconos locales de catálogos

El backend incluye una selección de SVG de Devicon v2.17.0: `java`, `kotlin`,
`react`, `spring` y `postgresql`. Sus archivos originales y la licencia MIT
del proyecto están en `src/main/resources/static/iconos/devicon/`. Los logotipos
son marcas de sus respectivos titulares; revisar sus condiciones de uso.

Las respuestas de habilidades, habilidades del perfil y tecnologías incorporan
el campo opcional `icono`. Si tiene valor, los clientes pueden solicitar el SVG
con `GET /iconos/devicon/{icono}.svg`; esta ruta de imágenes es pública. Si no
hay icono o la descarga falla, los clientes deben mostrar una alternativa.

Flyway V13 agrega columnas anulables sin modificar los datos existentes. Esta
fase no asigna iconos automáticamente ni ofrece una API de escritura o selector:
la validación y administración de identificadores se implementarán en la etapa
de backend-admin. Hasta entonces, solo deben utilizarse identificadores de los
archivos incluidos. Los clientes deben tratar la referencia como identificador,
no como una URL o una ruta arbitraria.

Para desarrollo, la URL base es la del backend configurada por cada cliente.
En el emulador Android `localhost` se refiere al emulador: debe usarse la URL
configurada para alcanzar el servidor anfitrión. En un dispositivo físico es
necesaria una dirección de la máquina que sea accesible desde su red.