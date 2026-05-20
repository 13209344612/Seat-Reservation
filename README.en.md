# Seat Reservation

Library Seat Reservation System

## Project Introduction

Seat Reservation is a library seat reservation system developed with Spring Boot 3.x, using MyBatis-Plus as the persistence layer, Redis for caching, and JWT for authentication. The system supports user registration and login, seat reservation, check-in, cancellation of reservations, and provides an admin interface for managing study rooms.

## Technology Stack

- **Backend Framework**: Spring Boot 3.x  
- **Persistence Layer**: MyBatis-Plus  
- **Database**: MySQL  
- **Caching**: Redis  
- **Security**: Spring Security + JWT  
- **Others**: Lombok, Validation  

## Features

### User Features
- User registration and login  
- Seat reservation  
- Check-in for reservations  
- Cancel reservation  
- View my reservation history  

### Admin Features
- Create, read, update, and delete study rooms  
- Manage time slots  

### System Features
- Automatic cancellation of expired reservations (checked every 5 minutes)  
- Cache support  
- Concurrent reservation control  

## Quick Start

### Prerequisites

- JDK 17+  
- Maven 3.8+  
- MySQL 8.0+  
- Redis 6.0+  

### Configuration

Configure database and Redis connection details in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/seat_reservation
    username: your_username
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379

jwt:
  secret: your-secret-key
  expiration: 86400000
```

### Build and Run

```bash
mvn clean install
java -jar target/seat-reservation-0.0.1-SNAPSHOT.jar
```

## API Endpoints

### Authentication Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST   | /api/auth/register | User registration |
| POST   | /api/auth/login | User login |

### Reservation Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST   | /api/reservations | Create a reservation |
| GET    | /api/reservations | List my reservations |
| GET    | /api/reservations/{id} | Get reservation details |
| POST   | /api/reservations/{id}/sign | Check in |
| POST   | /api/reservations/{id}/cancel | Cancel reservation |

### Study Room Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET    | /api/rooms | List study rooms |
| GET    | /api/rooms/{id} | Get study room details |
| POST   | /api/rooms | Create a study room (Admin only) |
| PUT    | /api/rooms/{id} | Update a study room (Admin only) |
| DELETE | /api/rooms/{id} | Delete a study room (Admin only) |

## Database Tables

- `user` - User table  
- `study_room` - Study room table  
- `time_slot` - Time slot table  
- `reservation` - Reservation table  

## License

MIT License