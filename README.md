Habilio es unaplicación móvil Android que conecta a trabajadores de oficios con clientes que buscan servicios. Los trabajadores construyen su reputación con evidencia fotográfica real de sus trabajos y reseñas. Los clientes buscan, comparan, solicitan el servicio y dan seguimiento.<br>
<h1>¿Qué es Habilio?</h1><br>
Habilio nació como proyecto de la asignatura Análisis y Diseño de Software en la Universidad Politécnica de Pénjamo. Su objetivo es ayudar a las personas que tienen oficios informales y no cuentan con habilidades de marketing a crear perfiles llamativos con los que puedan promocionar mejor su trabajo, sin depender únicamente del boca en boca, y así aumentar su número de clientes.<br>
Originalmente se planeó incorporar una inteligencia artificial conversacional con la que el trabajador pudiera platicar para generar su perfil. Como se trata de un hackatón interno, esa función se descartó al menos para esta primera entrega y queda como mejora planeada para el futuro.<br>
La aplicación está programada completamente con Kotlin y Jetpack Compose.<br>

<h1>Instalación de la app (APK)</h1><br>
<h2>Requisitos</h2><br>
Teléfono Android.<br>
Conexión a internet (la app usa un servidor en la nube).<br>
<h2>Pasos</h2><br>
Desde el teléfono, abre la sección Releases del repositorio.<br>
Descarga el archivo habilio.apk (en Assets).<br>
Ábrelo desde la notificación de descarga o desde la app Archivos.<br>
Si Android lo pide, activa "Permitir instalar apps de esta fuente" para tu navegador o administrador de archivos.<br>
Si aparece una advertencia de Play Protect, elige "Instalar de todos modos".<br>
Abre Habilio e inicia sesión con alguna cuenta de prueba.<br>

<h1>Credenciales de prueba</h1><br>
Son cuentas de demostración con datos ficticios.<br>
	Rol	        Correo	                  Contraseña<br>
	Trabajador	pedroplomero@habilio.com	habilio123<br>
	Trabajador	robmdz@habilio.com	      habilio123<br>
	Cliente	    camhrdz@habilio.com	        654321<br>
	Cliente	    jperez@habilio.com	        123456<br>

<h1>Funcionalidades</h1><br>
<h2>Cliente</h2><br>
Registro e inicio de sesión.<br>
Buscar especialistas por texto y filtrar por categoría de oficio (Albañilería, Plomería, Electricidad, Carpintería, Herrería, Pintura).<br>
Ver el perfil de un especialista: calificación, reseñas, portafolio de fotos de trabajos reales.<br>
Solicitar un servicio (descripción, ubicación y presupuesto).<br>
Dar seguimiento a sus solicitudes desde Mi Perfil.<br>
Evaluar al trabajador con estrellas y comentario.<br>
<h2>Trabajador</h2><br>
Registro con oficio, especialidad, zona y modalidad de trabajo (a domicilio, en taller o ambos).<br>
Subir fotos de trabajos a su portafolio (Mi Taller).<br>
Recibir y gestionar solicitudes: aceptar, rechazar y marcar como terminado, con filtros (Todas, Nuevas, En curso, Completadas).<br>

<h1>Casos de prueba</h1><br>
Pruebas de los tres flujos críticos. Se ejecutan con las credenciales de prueba.<br>
CP-01: Inicio de sesión<br>
Objetivo: Demostrar que las credenciales de prueba funcionan como deben<br>
Resultado esperado: con credenciales correctas se entra al panel del rol, con contraseña incorrecta aparece un mensaje de error en rojo y no se accede.<br>
CP-02: Subida de fotos al portafolio del trabajador<br>
Objetivo: Comprobar que un trabajador puede publicar una foto de trabajo y que un cliente la puede ver.<br>
Resultado esperado: la evidencia se publica con su foto, aparece en el taller del trabajador y es visible para el cliente.<br>
CP-03: Creación de una solicitud de un cliente<br>
Objetivo: Verificar que el cliente puede crear una solicitud y el trabajador recibirla<br>
Resultado esperado: la solicitud se crea en estado pendiente, el cliente la ve en su perfil y el trabajador la recibe en su bandeja.<br>

<h1>Incidencias conocidas</h1><br>
1. La sesión no se conserva al cerrar la app, hay que iniciar sesión de nuevo.<br>
2. Si el presupuesto escrito no es un número válido, se guarda un valor por defecto.<br>
3. Se descarga toda la información lo que provoca lentitud con muchos datos aunque funciona para este MVP.<br>
4. Zona de servicio única (Pénjamo, Gto.) y catálogo de categorías fijo.<br>

<h1>Tecnologías y arquitectura</h1><br>
Lenguaje:	Kotlin<br>
Interfaz:	Jetpack Compose<br>
Carga de imágenes:	Coil (AsyncImage)<br>
Selección de fotos:	Photo Picker de Android<br>
Concurrencia:	Corrutinas de Kotlin (Dispatchers.IO)<br>
Backend	Supabase: Auth (cuentas), PostgreSQL con API REST (datos) y Storage (fotos)<br>
Comunicación:	Peticiones HTTP a la API de Supabase con HttpURLConnection y org.json<br>

<h1>Declaración de uso de IA</h1><br>
Conforme a la convocatoria, se declara el uso de herramientas de inteligencia artificial como apoyo al desarrollo:<br>
Gemini: Asistente utilizado para apoyar la generación y el desarrollo del código Kotlin de la aplicación para entregar un trabajo minimamente viable ya que no contamos con el suficiente tiempo para entregar algo fiel y realista a nuestra vision sin ayuda de esta herramienta.<br>

Proyecto académico sin fines comerciales. Todos los datos de las cuentas de demostración son ficticios.
