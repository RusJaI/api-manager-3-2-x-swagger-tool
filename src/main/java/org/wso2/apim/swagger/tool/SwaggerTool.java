package org.wso2.apim.swagger.tool;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.toml.TomlFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.authenticator.stub.LogoutAuthenticationExceptionException;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;

import java.io.File;
import java.io.IOException;

public class SwaggerTool {

    private static final Logger log = LoggerFactory.getLogger(SwaggerTool.class);
    private static String baseUrl = "https://localhost:9443/";
    private static String hostName = "localhost";
    private static String userName;
    private static String password;

    private static String trustStoreAbsolutePath;
    private static String trustStorePassword;


    /**
     * No parameters are supported when executing the tool.
     */
    public static void main(String[] args) {
        try {
            parseConfigsToml();
            AdminServiceClientManager.invokeAdminServiceClient(baseUrl, userName, password, hostName,
                    trustStoreAbsolutePath, trustStorePassword);
        } catch (IOException | LoginAuthenticationExceptionException | ResourceAdminServiceExceptionException |
                 LogoutAuthenticationExceptionException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static void parseConfigsToml() throws IOException {
        TomlFactory tomlFactory = new TomlFactory();
        JsonParser parser = tomlFactory.createParser(new File("/Users/rusirijayodaillesinghe/Documents/" +
                "APIM_Repos/api-manager-3-2-x-swagger-tool/src/main/resources/config.toml"));

        while (!parser.isClosed()) {
            JsonToken token = parser.nextToken();

            /* null indicates end-of-input */
            if (token == null)
                break;

            String fieldName =  parser.getCurrentName();

            if ("carbon_server".equals(fieldName)) {
                while (token != JsonToken.END_OBJECT) {
                    if (fieldName.equalsIgnoreCase("baseurl")) { // remove / at the end of the base url
                        String extractedUrl = parser.getText();
                        if (extractedUrl.substring(extractedUrl.length() -1).equals("/")) {
                            baseUrl = extractedUrl.substring(0, extractedUrl.length() - 1);
                        } else {
                            baseUrl = extractedUrl;
                        }
                    }
                    if (fieldName.equalsIgnoreCase("hostname")) {
                        hostName = parser.getText();
                    }
                    if (fieldName.equalsIgnoreCase("username")) {
                        userName = parser.getText();
                    }
                    if (fieldName.equalsIgnoreCase("password")) {
                        password = parser.getText();
                    }
                    token = parser.nextToken();
                    fieldName = parser.getCurrentName();
                }
            }

            if ("truststore".equals(fieldName)) {
                while (token != JsonToken.END_OBJECT) {
                    if (fieldName.equalsIgnoreCase("absolute_path")) {
                        trustStoreAbsolutePath = parser.getText();
                    }
                    if (fieldName.equalsIgnoreCase("password")) {
                        trustStorePassword = parser.getText();
                    }
                    token = parser.nextToken();
                    fieldName = parser.getCurrentName();
                }
            }
        }
        parser.close();
    }
}
