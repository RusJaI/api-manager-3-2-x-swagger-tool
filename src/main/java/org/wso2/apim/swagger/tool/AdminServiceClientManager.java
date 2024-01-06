package org.wso2.apim.swagger.tool;

import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.authenticator.stub.LogoutAuthenticationExceptionException;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;

import java.io.IOException;

public class AdminServiceClientManager {
    public static void invokeAdminServiceClient() throws IOException, LoginAuthenticationExceptionException, ResourceAdminServiceExceptionException, LogoutAuthenticationExceptionException {

        System.setProperty("javax.net.ssl.trustStore",
                "/Users/rusirijayodaillesinghe/Documents/APIM_Repos/api-manager-3-2-x-swagger-tool" +
                        "/src/main/resources/security/client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");


        LoginAdminServiceClient loginAdminServiceClient =
                new LoginAdminServiceClient("https://localhost:9443");
        String sessionId = loginAdminServiceClient.authenticate("admin", "admin");

        ResourceAdminServiceAdminClient resourceAdminServiceAdminClient = new
                ResourceAdminServiceAdminClient("https://localhost:9443", sessionId);

        resourceAdminServiceAdminClient.validateCollectionContent();

        loginAdminServiceClient.logOut();

    }
}