package org.wso2.apim.swagger.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.authenticator.stub.LogoutAuthenticationExceptionException;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;

import java.io.IOException;

public class SwaggerTool {

    private static final Logger log = LoggerFactory.getLogger(SwaggerTool.class);
    /**
     * No parameters are supported when executing the tool.
     */
    public static void main(String[] args) {
        try {
            AdminServiceClientManager.invokeAdminServiceClient();
        } catch (IOException | LoginAuthenticationExceptionException | ResourceAdminServiceExceptionException |
                 LogoutAuthenticationExceptionException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

}
