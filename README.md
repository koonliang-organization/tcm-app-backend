# TCM App Backend

A comprehensive Traditional Chinese Medicine (TCM) herbs management system built with Spring Boot. This backend service provides APIs for managing TCM herb data, including their properties, flavors, formulas, indications, meridians, and images, with a built-in dataset publishing system.

## 🌿 Features

- **Herb Management**: Complete CRUD operations for TCM herbs and their attributes
- **Data Relationships**: Manage herb flavors, formulas, indications, meridians, and images
- **Search & Filtering**: Advanced search capabilities with pagination and sorting
- **Dataset Publishing**: Automated dataset export and publishing system
- **API Documentation**: Integrated OpenAPI/Swagger documentation
- **Data Validation**: Comprehensive input validation with detailed error messages
- **Security**: Built-in Spring Security integration
- **Health Monitoring**: Spring Boot Actuator for application monitoring

## 🛠 Technology Stack

- **Java**: 21
- **Framework**: Spring Boot 3.2.5
- **Database**: MySQL with Spring Data JPA
- **Documentation**: SpringDoc OpenAPI 3 (Swagger)
- **Build Tool**: Maven
- **Testing**: JUnit 5, Mockito, AssertJ
- **Security**: Spring Security
- **Migration**: Flyway (configurable)

## 📋 Prerequisites

- Java 21 or higher
- Maven 3.6 or higher
- MySQL 8.0 or higher

## 🚀 Getting Started

### Database Setup

1. **Install MySQL** and create a database:
   ```sql
   CREATE DATABASE tcm_recipes;
   CREATE USER 'admin'@'localhost' IDENTIFIED BY 'pass1234';
   GRANT ALL PRIVILEGES ON tcm_recipes.* TO 'admin'@'localhost';
   FLUSH PRIVILEGES;
   ```

2. **Configure Database Connection** (optional):
   Update `src/main/resources/application.yml` if you need different credentials:
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/tcm_recipes
       username: admin
       password: pass1234
   ```

### Installation

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd tcm-app-backend
   ```

2. **Build the project**:
   ```bash
   mvn clean compile
   ```

3. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080`

## 📚 API Documentation

Once the application is running, access the interactive API documentation at:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## 🌐 API Endpoints

### Herbs Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/herbs` | List all herbs with pagination and sorting |
| GET | `/api/v1/herbs/{id}` | Get herb by ID |
| GET | `/api/v1/herbs/by-source-url` | Get herb by source URL |
| GET | `/api/v1/herbs/search` | Search herbs by name |
| POST | `/api/v1/herbs` | Create new herb |
| PUT | `/api/v1/herbs/{id}` | Update existing herb |
| DELETE | `/api/v1/herbs/{id}` | Delete herb |

### Herb Attributes Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/herbs/{herbId}/flavors` | Add flavor to herb |
| DELETE | `/api/v1/herbs/{herbId}/flavors/{flavorId}` | Remove flavor from herb |
| POST | `/api/v1/herbs/{herbId}/formulas` | Add formula to herb |
| DELETE | `/api/v1/herbs/{herbId}/formulas/{formulaId}` | Remove formula from herb |
| POST | `/api/v1/herbs/{herbId}/images` | Add image to herb |
| DELETE | `/api/v1/herbs/{herbId}/images/{imageId}` | Remove image from herb |
| POST | `/api/v1/herbs/{herbId}/indications` | Add indication to herb |
| DELETE | `/api/v1/herbs/{herbId}/indications/{indicationId}` | Remove indication from herb |
| POST | `/api/v1/herbs/{herbId}/meridians` | Add meridian to herb |
| DELETE | `/api/v1/herbs/{herbId}/meridians/{meridianId}` | Remove meridian from herb |

### Dataset Publishing

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/publish/releases` | List all published releases |
| POST | `/api/v1/publish/releases` | Create new release |
| GET | `/api/v1/publish/releases/{id}` | Get release by ID |
| PUT | `/api/v1/publish/releases/{id}` | Update release |
| DELETE | `/api/v1/publish/releases/{id}` | Delete release |

## 📊 Example Usage

### Get herbs with pagination and sorting:
```bash
curl -X GET "http://localhost:8080/api/v1/herbs?page=0&size=10&sort=nameZh"
```

### Search herbs by name:
```bash
curl -X GET "http://localhost:8080/api/v1/herbs/search?searchTerm=ginseng"
```

### Create a new herb:
```bash
curl -X POST "http://localhost:8080/api/v1/herbs" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceUrl": "https://example.com/herb1",
    "nameZh": "人参",
    "namePinyin": "renshen",
    "descZh": "大补元气，复脉固脱",
    "descEn": "Tonifies original qi, restores pulse",
    "appearance": "Root is thick and fleshy",
    "property": "Sweet, slightly bitter, warm"
  }'
```

## 🗂 Database Schema

The application uses the following main entities:

- **Herb**: Main herb entity with basic information
- **HerbFlavor**: Flavor properties of herbs
- **HerbFormula**: Traditional formulas containing the herb
- **HerbIndication**: Medical indications for herb usage
- **HerbMeridian**: Meridian systems affected by the herb
- **HerbImage**: Image attachments for herbs
- **PublishRelease**: Dataset publishing releases

## ⚙️ Configuration

### Application Properties

Key configuration options in `application.yml`:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # Auto-create/update database schema
    show-sql: true      # Show SQL queries in logs

  flyway:
    enabled: false      # Disable Flyway migrations

publisher:
  storage:
    local-directory: build/datasets  # Dataset storage directory
  min-app-version: 1.0.0            # Minimum app version for datasets

logging:
  level:
    com.tcm.backend: DEBUG  # Debug logging for application
```

## 🧪 Testing

Run the test suite:

```bash
# Run all tests
mvn test

# Run tests with coverage
mvn test jacoco:report
```

## 🔧 Development

### Adding New Endpoints

1. Create DTOs in `com.tcm.backend.dto`
2. Define entities in `com.tcm.backend.domain`
3. Create repositories in `com.tcm.backend.repository`
4. Implement services in `com.tcm.backend.service.impl`
5. Add controllers in `com.tcm.backend.api`

### Error Handling

The application uses a global exception handler (`GlobalExceptionHandler`) that provides consistent error responses:

```json
{
  "result": "ERROR",
  "message": "Detailed error description",
  "data": null
}
```

### Success Responses

All successful API responses follow this format:

```json
{
  "result": "SUCCESS",
  "message": "Operation completed successfully",
  "data": { ... }
}
```

## 🚀 Deployment

### Production Configuration

1. Update database credentials in `application-prod.yml`
2. Enable Flyway for production migrations
3. Configure proper logging levels
4. Set up health check endpoints via Actuator

### Building for Production

```bash
# Build executable JAR
mvn clean package

# Run the JAR
java -jar target/tcm-app-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 📝 Logging

The application provides comprehensive logging:

- **Application logs**: All business logic operations
- **SQL queries**: Database query logging (configurable)
- **HTTP requests**: Request/response logging
- **Error tracking**: Detailed error logging with stack traces

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Troubleshooting

### Common Issues

1. **Database Connection Error**:
   - Verify MySQL is running on port 3306
   - Check database credentials in `application.yml`
   - Ensure database `tcm_recipes` exists

2. **Port Already in Use**:
   - Change server port in `application.yml`: `server.port: 8081`
   - Or kill the process using port 8080

3. **Invalid Sort Parameter Error**:
   - Use valid entity properties for sorting (e.g., `nameZh`, `namePinyin`)
   - Check API documentation for available sort fields

4. **Build Failures**:
   - Ensure Java 21 is installed and configured
   - Run `mvn clean` before building
   - Check for dependency conflicts

For additional support, please check the logs in `logs/` directory or enable debug logging by setting `logging.level.com.tcm.backend: DEBUG`.