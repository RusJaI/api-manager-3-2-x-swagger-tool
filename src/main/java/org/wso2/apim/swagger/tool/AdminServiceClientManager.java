/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.apim.swagger.tool;

import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.authenticator.stub.LogoutAuthenticationExceptionException;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;

import java.io.IOException;

public class AdminServiceClientManager {
    public static void invokeAdminServiceClient(String backendUrl, String userName, String password, String hostName)
            throws IOException, LoginAuthenticationExceptionException, ResourceAdminServiceExceptionException,
            LogoutAuthenticationExceptionException {

        LoginAdminServiceClient loginAdminServiceClient =
                new LoginAdminServiceClient(backendUrl);
        String sessionId = loginAdminServiceClient.authenticate(hostName, userName, password);

        ResourceAdminServiceAdminClient resourceAdminServiceAdminClient = new
                ResourceAdminServiceAdminClient(backendUrl, sessionId);

        resourceAdminServiceAdminClient.validateCollectionContent();

        loginAdminServiceClient.logOut();

    }
}