# Database Migration Guide - BookStore Backend

## Overview
This project uses **Flyway** for database schema versioning and migrations. Your migration file is located at:
- `src/main/resources/db/migration/V1__create_table.sql`

## Prerequisites

### 1. MySQL Database
Ensure MySQL is installed and running on your system.

### 2. Create Database
```sql
CREATE DATABASE bookstore CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Environment Variables
Create a `.env` file in the project root directory:

```env
# Database Configuration
DB_URL=jdbc:mysql://localhost:3306/bookstore?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
DB_USER=root
DB_PASSWORD=your_password_here

# Server Configuration
SERVER_PORT=8080

# Email Configuration (Optional for migrations)
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=your_email@gmail.com
EMAIL_PASSWORD=your_app_password

# Google OAuth (Optional for migrations)
GOOGLE_CLIENT_ID=your_client_id
GOOGLE_CLIENT_SECRET=your_client_secret
GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google

# Cloudinary (Optional for migrations)
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

---

## Method 1: Automatic Migration (Recommended)

### ✅ Migrations run automatically when starting the application

**Using Maven Wrapper (Windows):**
```powershell
.\mvnw.cmd spring-boot:run
```

**Using Maven (if installed):**
```powershell
mvn spring-boot:run
```

**Using IntelliJ IDEA:**
1. Open `src/main/java/com/vn/backend/BackendApplication.java`
2. Click the green play button or press `Shift + F10`

### What happens:
1. Application starts
2. Flyway connects to database
3. Creates `flyway_schema_history` table (if not exists)
4. Checks for pending migrations
5. Executes `V1__create_table.sql`
6. Creates all 20+ tables in your database
7. Records migration in history table

---

## Method 2: Manual Migration via Maven Plugin

### Run migration only (without starting the app):
```powershell
.\mvnw.cmd flyway:migrate -Dflyway.url=jdbc:mysql://localhost:3306/bookstore -Dflyway.user=root -Dflyway.password=your_password
```

### Check migration status:
```powershell
.\mvnw.cmd flyway:info -Dflyway.url=jdbc:mysql://localhost:3306/bookstore -Dflyway.user=root -Dflyway.password=your_password
```

### Validate migrations:
```powershell
.\mvnw.cmd flyway:validate -Dflyway.url=jdbc:mysql://localhost:3306/bookstore -Dflyway.user=root -Dflyway.password=your_password
```

### Repair migration checksums:
```powershell
.\mvnw.cmd flyway:repair -Dflyway.url=jdbc:mysql://localhost:3306/bookstore -Dflyway.user=root -Dflyway.password=your_password
```

---

## Method 3: Docker Compose (If Available)

If you have Docker and your `docker-compose.yml` includes database setup:

```powershell
docker-compose up -d
.\mvnw.cmd spring-boot:run
```

---

## Verifying Migrations

### Check if migration ran successfully:
```sql
-- Connect to your database
USE bookstore;

-- Check Flyway history
SELECT * FROM flyway_schema_history;

-- List all tables (should see 20+ tables)
SHOW TABLES;

-- Verify specific tables
DESC users;
DESC products;
DESC orders;
```

### Expected Tables Created:
1. authors
2. cart_items
3. carts
4. categories
5. coupons
6. invalid_tokens
7. order_coupons
8. order_items
9. orders
10. password_reset_tokens
11. permissions
12. product_images
13. product_previews
14. products
15. refresh_tokens
16. reviews
17. role_permissions
18. roles
19. user_roles
20. users
21. wishlists
22. flyway_schema_history (created by Flyway)

---

## Adding New Migrations

### Naming Convention
Follow this pattern: `V{version}__{description}.sql`

Examples:
- `V2__add_user_verification_column.sql`
- `V3__create_notification_table.sql`
- `V4__update_product_price_type.sql`

### Create a new migration:
1. Create file in `src/main/resources/db/migration/`
2. Name it with next version number: `V2__description.sql`
3. Add your SQL statements:
```sql
-- V2__add_user_verification_column.sql
ALTER TABLE users ADD COLUMN email_verified BIT(1) DEFAULT 0 AFTER email;
```
4. Restart application or run `flyway:migrate`

---

## Troubleshooting

### Issue: "Table already exists"
**Cause:** Database tables were created manually before migration.

**Solution:**
```sql
-- Option 1: Drop all tables and re-run migration
DROP DATABASE bookstore;
CREATE DATABASE bookstore;

-- Option 2: Use baseline (already configured in application.yml)
-- Flyway will mark existing schema as V1 and continue with V2+
```

### Issue: "Checksum mismatch"
**Cause:** Migration file was modified after being executed.

**Solution:**
```powershell
# Repair the Flyway metadata
.\mvnw.cmd flyway:repair -Dflyway.url=jdbc:mysql://localhost:3306/bookstore -Dflyway.user=root -Dflyway.password=your_password
```

### Issue: Environment variables not found
**Cause:** `.env` file not loaded or missing variables.

**Solution:**
- Verify `.env` file exists in project root
- Check that `spring-dotenv` dependency is in pom.xml (✅ already included)
- Restart your IDE/terminal

### Issue: Connection refused
**Cause:** MySQL server not running.

**Solution:**
```powershell
# Windows - Start MySQL service
net start MySQL80

# Or using MySQL Workbench/XAMPP/WAMP
```

---

## Configuration Files

### Current Flyway Configuration (application.yml):
```yaml
flyway:
  enabled: true                    # Auto-run migrations on startup
  baseline-on-migrate: true        # Handle existing databases
  clean-disabled: true             # Prevent accidental data loss
```

### To disable auto-migration:
Change `enabled: false` in `application.yml` (not recommended for development)

---

## Best Practices

1. **Never modify executed migrations** - Create new migrations instead
2. **Test migrations on local database first** - Before production
3. **Keep migrations small and focused** - One logical change per file
4. **Use descriptive names** - Make purpose clear from filename
5. **Version control** - Always commit migration files to Git
6. **Backup production data** - Before running migrations

---

## Quick Start Checklist

- [ ] MySQL installed and running
- [ ] Database `bookstore` created
- [ ] `.env` file created with correct credentials
- [ ] Run `.\mvnw.cmd spring-boot:run`
- [ ] Check logs for "Migrating schema" messages
- [ ] Verify tables in database

---

## Need Help?

Check Flyway documentation: https://flywaydb.org/documentation/

Check the application logs when running for detailed migration information.

