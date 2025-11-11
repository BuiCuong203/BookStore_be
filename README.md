# Book Shop Project

## Prerequisites

- JDK 17 or higher
- Maven 3.8+
- MySQL 8.0+

## Project Setup

### 1. Clone Repository

```bash
git clone <repository-url>
cd Graduation_prj_be
```

### 2. Environment Configuration

Create environment files based on your needs:

1. Copy example environment files:

```bash
cp .env.example .env
```

2. Update the environment variables in each .env file according to your setup.

Example `.env`:

```properties
SPRING_PROFILES_ACTIVE=dev
CORS_ALLOWED_ORIGIN=http://localhost:5173
SERVER_PORT=8080
DB_URL=jdbc:mysql://localhost:3308/book_shop
DB_DRIVER=com.mysql.cj.jdbc.Driver
DB_PORT=3308
DB_USER=root
DB_PASSWORD=your_password
JWT_SECRET=your_secret_key
```

### 3. Database Setup

1. Create MySQL database:

```sql
CREATE
DATABASE book_shop;
```

2. Configure database connection in `.env`

### 4. Build & Run

#### Using Maven Wrapper

```bash
# Development environment
./mvnw spring-boot:run
```

#### Using Maven

```bash
mvn spring-boot:run
```

### 5. API Access

- Base URL: `http://localhost:8080`
- API Documentation: `http://localhost:8080/swagger-ui.html`
