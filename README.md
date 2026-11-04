Habilio es unaplicación móvil Android que conecta a trabajadores de oficios con clientes que buscan servicios. Los trabajadores construyen su reputación con evidencia fotográfica real de sus trabajos y reseñas. Los clientes buscan, comparan, solicitan el servicio y dan seguimiento.

¿Qué es Habilio?
Habilio nació como proyecto de la asignatura Análisis y Diseño de Software en la Universidad Politécnica de Pénjamo. Su objetivo es ayudar a las personas que tienen oficios informales y no cuentan con habilidades de marketing a crear perfiles llamativos con los que puedan promocionar mejor su trabajo, sin depender únicamente del boca en boca, y así aumentar su número de clientes.
Originalmente se planeó incorporar una inteligencia artificial conversacional con la que el trabajador pudiera platicar para generar su perfil. Como se trata de un hackatón interno, esa función se descartó al menos para esta primera entrega y queda como mejora planeada para el futuro.
La aplicación está programada completamente con Kotlin y Jetpack Compose.

Instalación de la app (APK)
Requisitos
Teléfono Android.
Conexión a internet (la app usa un servidor en la nube).
Pasos
Desde el teléfono, abre la sección Releases del repositorio.
Descarga el archivo habilio.apk (en Assets).
Ábrelo desde la notificación de descarga o desde la app Archivos.
Si Android lo pide, activa "Permitir instalar apps de esta fuente" para tu navegador o administrador de archivos.
Si aparece una advertencia de Play Protect, elige "Instalar de todos modos".
Abre Habilio e inicia sesión con alguna cuenta de prueba.

Credenciales de prueba
Son cuentas de demostración con datos ficticios.
#	Rol	        Correo	                  Contraseña
1	Trabajador	pedroplomero@habilio.com	habilio123
2	Trabajador	robmdz@habilio.com	      habilio123
3	Cliente	    camhrdz@habilio.com	        654321
4	Cliente	    jperez@habilio.com	        123456

Funcionalidades
Cliente
Registro e inicio de sesión.
Buscar especialistas por texto y filtrar por categoría de oficio (Albañilería, Plomería, Electricidad, Carpintería, Herrería, Pintura).
Ver el perfil de un especialista: calificación, reseñas, portafolio de fotos de trabajos reales.
Solicitar un servicio (descripción, ubicación y presupuesto).
Dar seguimiento a sus solicitudes desde Mi Perfil.
Evaluar al trabajador con estrellas y comentario.
Trabajador
Registro con oficio, especialidad, zona y modalidad de trabajo (a domicilio, en taller o ambos).
Subir fotos de trabajos a su portafolio (Mi Taller).
Recibir y gestionar solicitudes: aceptar, rechazar y marcar como terminado, con filtros (Todas, Nuevas, En curso, Completadas).

Casos de prueba
Pruebas de los tres flujos críticos. Se ejecutan con las credenciales de prueba.
CP-01 — Inicio de sesión	
Objetivo: Demostrar que las credenciales de prueba funcionan como deben
Resultado esperado: con credenciales correctas se entra al panel del rol, con contraseña incorrecta aparece un mensaje de error en rojo y no se accede.
CP-02 — Subida de fotos al portafolio del trabajador
Objetivo:	Comprobar que un trabajador puede publicar una foto de trabajo y que un cliente la puede ver.
Resultado esperado: la evidencia se publica con su foto, aparece en el taller del trabajador y es visible para el cliente.
CP-03 — Creación de una solicitud de un cliente
Objetivo: Verificar que el cliente puede crear una solicitud y el trabajador recibirla
Resultado esperado: la solicitud se crea en estado pendiente, el cliente la ve en su perfil y el trabajador la recibe en su bandeja.


Incidencias conocidas
1. La sesión no se conserva al cerrar la app, hay que iniciar sesión de nuevo.
2. Si el presupuesto escrito no es un número válido, se guarda un valor por defecto.
3. Se descarga toda la información lo que provoca	lentitud con muchos datos	aunque funciona para este MVP.
4. Zona de servicio única (Pénjamo, Gto.) y catálogo de categorías fijo.

Tecnologías y arquitectura
Lenguaje:	Kotlin
Interfaz:	Jetpack Compose
Carga de imágenes:	Coil (AsyncImage)
Selección de fotos:	Photo Picker de Android
Concurrencia:	Corrutinas de Kotlin (Dispatchers.IO)
Backend	Supabase: Auth (cuentas), PostgreSQL con API REST (datos) y Storage (fotos)
Comunicación:	Peticiones HTTP a la API de Supabase con HttpURLConnection y org.json

Declaración de uso de IA
Conforme a la convocatoria, se declara el uso de herramientas de inteligencia artificial como apoyo al desarrollo:
Gemini: Asistente utilizado para apoyar la generación y el desarrollo del código Kotlin de la aplicación para entregar un trabajo minimamente viable ya que no contamos con el suficiente tiempo para entregar algo fiel y realista a nuestra vision sin ayuda de esta herramienta.

Proyecto académico sin fines comerciales. Todos los datos de las cuentas de demostración son ficticios.
