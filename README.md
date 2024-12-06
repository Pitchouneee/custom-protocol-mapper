# Custom Protocol Mapper for Keycloak

## Description

This project provides an example of a **Custom Protocol Mapper** for Keycloak. It adds a list of companies associated
with a user, retrieved from database, to the **claims** of the token.

The goal is to provide a starting point for developers looking to implement their own custom protocol mapper tailored 
to specific requirements.

### Features :
- Add a list of companies (fetched from database) to the token claims
- Allows dynamic configuration of database connection parameters via the Keycloak interface

## Prerequisites

- **Java** : Version 11 or higher
- **Keycloak** : Version 15 or higher
- A relational database (e.g. MariaDB)
- Maven environment for building the project

## Database schema

For this example, we use a database with two tables : `users` and `companies`

### Schema

```sql
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE companies (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    label VARCHAR(50) NOT NULL,
    user_id INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### Example data

```sql
INSERT INTO users (username) VALUES ('john_doe'), ('jane_doe');

INSERT INTO companies (name, label, user_id) VALUES
('Company A', 'Label A', 1),
('Company B', 'Label B', 1),
('Company C', 'Label C', 2);
```

## Installation and configuration

### Step 1 : Build the project
1. Clone the repository :
   ```bash
   git clone https://github.com/Pitchouneee/custom-protocol-mapper.git
   cd custom-protocol-mapper
   ```
2. Build and package the project :
   ```bash
   mvn clean install
   ```

### Step 2 : deploy to Keycloak
1. Coy the generated JAR (`target/custom-protocol-mapper-<version>.jar`)to the `providers` directory of your Keycloak 
   installation
2. Restart Keycloak server :
   ```bash
   ./bin/kc.sh start
   ```

### Step 3 : Configuration in Keycloak
1. Log in to the Keycloak admin console
2. Got to the **Clients** tab, select the desired client
3. Navigate to the **Client scopes** tab and select the dedicated client
4. Now, in the **Mappers** tab, add a mapper by configuration :
    - **Mapper Type** : `Custom Token Mapper`.
    - Set the name of the claim (e.g. `companies`)
    - Set the JDBC parameters :
        - `JDBC Driver` : `org.mariadb.jdbc.Driver`
        - `JDBC URL` : e.g., `jdbc:mariadb://localhost:3306/mydb`
        - `Database Username` : `root`
        - `Database Password` : `password`
5. Save the configuration

## Example usage

### Generated token with companies 

Once configured, the token will include a custom claim containing the list of companies associated with the authenticated
user

**Example of the added claim :**
```json
{
  "companies": [
    {
      "name": "Company A",
      "label": "Label A"
    },
    {
      "name": "Company B",
      "label": "Label B"
    }
  ]
}
```

## Customization

To adapt this mapper to your requirements :
- Modify the SQL query in the `fetchCompaniesFromDatabase` method
- Add additional configuration parameters using `ProviderConfigProperty`, if needed

## Contribution

Contributions are welcome ! If you want to report an issue or suggest feature, feel free to open an issue or submit a 
pull request