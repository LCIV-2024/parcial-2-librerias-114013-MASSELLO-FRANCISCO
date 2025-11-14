[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/htlnVKxL)
# Sistema de Gestión de Librería

Sistema de gestión de inventario para una pequeña librería que integra información de libros desde una API externa, gestiona usuarios y maneja reservas con cálculo automático de tarifas por demora.

## Características

- **Gestión de Inventario**: Sincronización de libros desde API externa con control de stock
- **CRUD de Usuarios**: Gestión completa de usuarios de la librería
- **Sistema de Reservas**: Reserva de libros por días con cálculo automático de tarifas
- **Cálculo de Multas**: Aplicación automática del 15% del precio del libro por cada día de demora
- **API REST**: Endpoints completos para todas las operaciones
- **Persistencia**: Base de datos H2 con JPA/Hibernate
- **Dockerización**: Configuración completa con Docker y Docker Compose
- **Testing**: Tests unitarios y de integración

## Tecnologías

- Spring Boot 3.5.7
- Java 17
- H2 Database
- JPA/Hibernate
- RestTemplate
- Docker & Docker Compose
- JUnit 5 & Mockito

## Requisitos Previos

- Java 17 o superior
- Maven 3.6+
- Docker y Docker Compose (para ejecución con contenedores)

## Configuración

### Variables de Entorno

El archivo `application.yaml` contiene la configuración de la base de datos y la URL de la API externa:

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/libreria_db
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  h2:
    console:
      enabled: true
      path: /h2-console

external:
  api:
    books:
      url: https://my-json-server.typicode.com/Gabriel-Arriola-UTN/libros/books
```

### Consola H2

La consola H2 está habilitada y disponible en: `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:file:./data/libreria_db` (o `jdbc:h2:file:/data/libreria_db` en Docker)
- Usuario: `sa`
- Contraseña: (vacía)

## Ejecución

### Opción 1: Con Docker Compose (Recomendado)

1. Construir y ejecutar los contenedores:
```bash
docker-compose up --build
```

2. La aplicación estará disponible en `http://localhost:8080`
3. La base de datos H2 se almacenará en el volumen Docker `h2_data`

### Opción 2: Sin Docker

1. Ejecuta la aplicación:
```bash
./mvnw spring-boot:run
```

2. La base de datos H2 se creará automáticamente en el directorio `./data/libreria_db.mv.db`

## API Endpoints

### Usuarios

- `POST /api/users` - Crear usuario
- `GET /api/users` - Obtener todos los usuarios
- `GET /api/users/{id}` - Obtener usuario por ID
- `PUT /api/users/{id}` - Actualizar usuario
- `DELETE /api/users/{id}` - Eliminar usuario

### Libros

- `POST /api/books/sync` - Sincronizar libros desde API externa
- `GET /api/books` - Obtener todos los libros
- `GET /api/books/{externalId}` - Obtener libro por ID externo
- `PUT /api/books/{externalId}/stock?stockQuantity={cantidad}` - Actualizar stock

### Reservas

- `POST /api/reservations` - Crear reserva
- `GET /api/reservations` - Obtener todas las reservas
- `GET /api/reservations/{id}` - Obtener reserva por ID
- `GET /api/reservations/user/{userId}` - Obtener reservas de un usuario
- `GET /api/reservations/active` - Obtener reservas activas
- `GET /api/reservations/overdue` - Obtener reservas vencidas
- `POST /api/reservations/{id}/return` - Devolver libro

## Ejemplos de Uso

### 1. Sincronizar libros desde la API externa

```bash
curl -X POST http://localhost:8080/api/books/sync
```

### 2. Crear un usuario

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Juan Pérez",
    "email": "juan@example.com",
    "phoneNumber": "123456789"
  }'
```

### 3. Crear una reserva

```bash
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "bookExternalId": 258027,
    "rentalDays": 7,
    "startDate": "2024-01-15"
  }'
```

### 4. Devolver un libro

```bash
curl -X POST http://localhost:8080/api/reservations/1/return \
  -H "Content-Type: application/json" \
  -d '{
    "returnDate": "2024-01-22"
  }'
```

## Cálculo de Tarifas

- **Tarifa Base**: Precio del libro × días de alquiler
- **Multa por Demora**: 15% del precio del libro × días de demora

Ejemplo:
- Libro: $15.99
- Días de alquiler: 7
- Tarifa base: $15.99 × 7 = $111.93
- Si se devuelve 3 días tarde: Multa = $15.99 × 0.15 × 3 = $7.20

## Testing

### Tests Unitarios

Implementar los tests de la capa de servicio


## Estructura del Proyecto

```
src/
├── main/
│   ├── java/com/example/libreria/
│   │   ├── config/          # Configuraciones (RestTemplate)
│   │   ├── controller/       # Controladores REST
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── model/           # Entidades JPA
│   │   ├── repository/      # Repositorios JPA
│   │   └── service/         # Lógica de negocio
│   └── resources/
│       └── application.yaml # Configuración
└── test/
    └── java/com/example/libreria/
        └── service/         # Tests unitarios
```

## Notas

- La primera vez que se ejecuta, es necesario sincronizar los libros desde la API externa usando el endpoint `/api/books/sync`
- El stock inicial de los libros sincronizados es de 10 unidades por defecto
- Las reservas activas reducen automáticamente la cantidad disponible de libros
- Al devolver un libro, se calcula automáticamente la multa si hay demora

## PUNTAJE
- UserController: 10 puntos
- ReservationRepository: 10 puntos
- ReservationService: 30 puntos
- ExternalBookService: 10 puntos
- ReservationServiceTest: 20 puntos
- DockerFile y Docker-Compose: 20 puntos
