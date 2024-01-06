package org.wso2.apim.swagger.tool;

import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.authenticator.stub.LogoutAuthenticationExceptionException;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;

import java.io.IOException;

public class AdminServiceClientManager {
    public static void invokeAdminServiceClient(String backendUrl, String userName, String password, String hostName,
                                                String trustStorePath, String trustStorePassword)
            throws IOException, LoginAuthenticationExceptionException, ResourceAdminServiceExceptionException,
            LogoutAuthenticationExceptionException {

        System.setProperty("javax.net.ssl.trustStore", trustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);


        LoginAdminServiceClient loginAdminServiceClient =
                new LoginAdminServiceClient(backendUrl);
        String sessionId = loginAdminServiceClient.authenticate(hostName, userName, password);

        ResourceAdminServiceAdminClient resourceAdminServiceAdminClient = new
                ResourceAdminServiceAdminClient(backendUrl, sessionId);

        resourceAdminServiceAdminClient.validateCollectionContent();

        loginAdminServiceClient.logOut();

    }
}