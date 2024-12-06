package fr.corentinbringer.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.corentinbringer.protocol.claims.Company;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static fr.corentinbringer.protocol.CustomProtocolMapperConstants.*;

@Slf4j
public class CustomProtocolMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String PROVIDER_ID = "custom-protocol-mapper";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    private static final ProviderConfigProperty JDBC_DRIVER_PROPERTY = new ProviderConfigProperty(
            CONFIG_KEY_JDBC_DRIVER,
            "JDBC Driver",
            "The JDBC driver class name",
            ProviderConfigProperty.STRING_TYPE,
            "org.mariadb.jdbc.Driver"
    );
    private static final ProviderConfigProperty JDBC_URL_PROPERTY = new ProviderConfigProperty(
            CONFIG_KEY_JDBC_URL,
            "JDBC URL",
            "The URL of your database",
            ProviderConfigProperty.STRING_TYPE,
            ""
    );
    private static final ProviderConfigProperty DB_USERNAME_PROPERTY = new ProviderConfigProperty(
            CONFIG_KEY_DB_USERNAME,
            "Database Username",
            "The username for database access",
            ProviderConfigProperty.STRING_TYPE,
            ""
    );
    private static final ProviderConfigProperty DB_PASSWORD_PROPERTY = new ProviderConfigProperty(
            CONFIG_KEY_DB_PASSWORD,
            "Database Password",
            "The password for database access",
            ProviderConfigProperty.STRING_TYPE,
            ""
    );

    static {
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, CustomProtocolMapper.class);
        configProperties.add(JDBC_DRIVER_PROPERTY);
        configProperties.add(JDBC_URL_PROPERTY);
        configProperties.add(DB_USERNAME_PROPERTY);
        configProperties.add(DB_PASSWORD_PROPERTY);
    }

    @Override
    public String getDisplayCategory() {
        return "Token Mapper";
    }

    @Override
    public String getDisplayType() {
        return "Custom Token Mapper";
    }

    @Override
    public String getHelpText() {
        return "Custom Protocol Mapper : Add database user companies";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel,
                            UserSessionModel userSession, KeycloakSession keycloakSession,
                            ClientSessionContext clientSessionCtx) {
        List<Company> companies = fetchCompaniesFromDatabase(userSession.getLoginUsername(), mappingModel);

        try {
            String companiesJson =  objectMapper.writeValueAsString(companies);

            if (log.isDebugEnabled()) {
                log.debug("setClaimIDToken, companiesJson={}", companiesJson);
            }

            OIDCAttributeMapperHelper.mapClaim(token, mappingModel, companiesJson);
        } catch (IOException e) {
            log.error("setClaim, companies={}, exception={}", companies, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private List<Company> fetchCompaniesFromDatabase(String username, ProtocolMapperModel mappingModel) {
        List<Company> companies = new ArrayList<>();

        String jdbcDriver = mappingModel.getConfig().get(CONFIG_KEY_JDBC_DRIVER);
        String jdbcUrl = mappingModel.getConfig().get(CONFIG_KEY_JDBC_URL);
        String dbUsername = mappingModel.getConfig().get(CONFIG_KEY_DB_USERNAME);
        String dbPassword = mappingModel.getConfig().get(CONFIG_KEY_DB_PASSWORD);

        try {
            Class.forName(jdbcDriver);
        } catch (ClassNotFoundException e) {
            log.error("JDBC Driver not found: {}", jdbcDriver, e);
            throw new RuntimeException("JDBC Driver not found: " + jdbcDriver, e);
        }

        String sqlQuery = """
            SELECT c.name, c.label
            FROM companies c
            JOIN users u ON u.id = c.label
            WHERE u.username = ?
            """;

        try (Connection connection = DriverManager.getConnection(jdbcUrl, dbUsername, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sqlQuery)) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Company company = Company.builder()
                            .name(resultSet.getString("name"))
                            .label(resultSet.getString("label"))
                            .build();
                    companies.add(company);
                }
            }
        } catch (SQLException e) {
            log.error("Database error while fetching companies for username={}", username, e);
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }

        return companies;
    }
}