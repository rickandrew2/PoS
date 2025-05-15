# Point of Sale (PoS) System

A Spring Boot-based Point of Sale system with secure authentication and database management.

## Prerequisites

Before you begin, ensure you have the following installed:
- Java Development Kit (JDK) 17 
- Maven (latest version)
- MySQL Server 8.0 or higher
- Git
- An IDE (IntelliJ IDEA or VS Code recommended)

## Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/rickandrew2/PoS.git
cd PoS
```

### 2. Database Setup
1. Install MySQL Server if not already installed
2. Create a new database for the project
3. Update the database configuration in `src/main/resources/application.properties` with your MySQL credentials

### 3. Project Setup

#### Using IntelliJ IDEA:
1. Open IntelliJ IDEA
2. Select "Open" and choose the project directory
3. Wait for Maven to download all dependencies
4. Install the following plugins:
   - Lombok
   - Spring Boot
   - Maven

#### Using VS Code:
1. Open VS Code
2. Open the project folder
3. Install the following extensions:
   - Java Extension Pack
   - Spring Boot Extension Pack
   - Maven for Java
4. Wait for Maven to download all dependencies

### 4. Running the Application

#### Using Maven:
```bash
mvn spring-boot:run
```

#### Using IDE:
- Run the main class `PosSystemApplication`

The application will start on `http://localhost:8080`

## Project Structure
```
PoS/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
├── pom.xml
└── README.md
```

## Dependencies
- Spring Boot 3.2.2
- Spring Security
- Spring Data JPA
- MySQL Connector
- Thymeleaf
- Lombok
- JWT for authentication
- OpenCSV

## Troubleshooting

### Common Issues and Solutions:

1. **Port Conflict**
   - Error: "Web server failed to start. Port 8080 was already in use"
   - Solution: Change the port in `application.properties` or stop the application using port 8080

2. **Database Connection**
   - Error: "Cannot connect to database"
   - Solution: 
     - Verify MySQL service is running
     - Check database credentials in `application.properties`
     - Ensure database exists

3. **Maven Dependencies**
   - Error: "Failed to download dependencies"
   - Solution:
     - Check internet connection
     - Verify Maven settings
     - Try running `mvn clean install`

4. **Java Version**
   - Error: "Unsupported class file major version"
   - Solution: Ensure you're using JDK 17 or higher

## Contributing
1. Create a new branch for your feature
2. Make your changes
3. Submit a pull request

## Support
If you encounter any issues or have questions, please create an issue in the repository. 