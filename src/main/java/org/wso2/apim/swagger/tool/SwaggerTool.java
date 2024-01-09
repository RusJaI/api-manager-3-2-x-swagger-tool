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

import com.beust.jcommander.JCommander;
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

/**
 * This is the main class which contains the execution logic.
 */
public class SwaggerTool {

    private static final Logger log = LoggerFactory.getLogger(SwaggerTool.class);
    private static String baseUrl;
    private static String hostName;
    private static String userName;
    private static String password;
    private static Boolean doDownload;
    private static String trustStoreAbsolutePath;
    private static String trustStorePassword;


    /**
     * No parameters are supported when executing the tool.
     */
    public static void main(String[] args) {
        try {
            JcommanderArgs jcommanderArgs = new JcommanderArgs();
            JCommander.newBuilder()
                    .addObject(jcommanderArgs)
                    .build()
                    .parse(args);

            userName = jcommanderArgs.getUsername();
            password = jcommanderArgs.getPassword();
            baseUrl = jcommanderArgs.getBaseurl();
            hostName = jcommanderArgs.getHostname();
            trustStoreAbsolutePath = jcommanderArgs.getTruststorepath();
            trustStorePassword = jcommanderArgs.getTruststorepassword();
            doDownload = jcommanderArgs.getDoDownload();

            // remove / at the end of the base url
            if (baseUrl.substring(baseUrl.length() -1).equals("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            System.setProperty(Constants.TRUSTSTORE, trustStoreAbsolutePath);
            System.setProperty(Constants.TRUSTSTORE_PASSWORD, trustStorePassword);

            AdminServiceClientManager.invokeAdminServiceClient(baseUrl, userName, password, hostName);

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
