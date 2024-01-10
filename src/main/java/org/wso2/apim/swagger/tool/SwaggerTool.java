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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.authenticator.stub.LogoutAuthenticationExceptionException;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    protected static Map<String, List<String>> errorResultsMap = new HashMap<String, List<String>>();

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

            String currentDate = new SimpleDateFormat("yyyyMMdd_HH-mm-ss").format(new Date());
            // Define the folder structure
            String folderPath = "results/" + currentDate;
            String filePath = folderPath + "/result.txt";

            // Create the folder structure
            File folder = new File(folderPath);
            if (!folder.exists()) {
                boolean success = folder.mkdirs();
                if (!success) {
                    log.error("Error creating folder structure.");
                    return;
                }
            }

            // Create the results.txt file
            File file = new File(filePath);
            try {
                file.createNewFile();
            } catch (IOException e) {
                log.error("Error creating file: " + filePath);
                e.printStackTrace();
            }

            // create the output file
            FileWriter fileWriter = null;
            try {
                fileWriter = new FileWriter(file, true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            AdminServiceClientManager.invokeAdminServiceClient(baseUrl, userName, password, hostName);

            SwaggerValidateUtils.writeStatsSummary(fileWriter);

        } catch (IOException | LoginAuthenticationExceptionException | ResourceAdminServiceExceptionException |
                 LogoutAuthenticationExceptionException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

}
