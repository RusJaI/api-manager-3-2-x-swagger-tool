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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the main class which contains the execution logic.
 */
public class SwaggerTool {

    private static final Log log = LogFactory.getLog(SwaggerTool.class);
    private static String baseUrl;
    private static String userName;
    private static String password;
    private static Boolean doDownload;
    private static String trustStoreAbsolutePath;
    private static String trustStorePassword;

    protected static Map<String, List<String>> errorResultsMap = new HashMap<String, List<String>>();
    protected static Map<String,String> errorSwaggers = new HashMap<>();

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
            trustStoreAbsolutePath = jcommanderArgs.getTruststorepath();
            trustStorePassword = jcommanderArgs.getTruststorepassword();
            doDownload = jcommanderArgs.getDoDownload();

            if (trustStoreAbsolutePath == null) {
                Path currentDirectoryPath = Paths.get(System.getProperty("user.dir"));
                trustStoreAbsolutePath = currentDirectoryPath.getParent().toString() +
                        Constants.TRUSTSTORE_DEFAULT_SUBPATH;
            }
            // remove / at the end of the base url
            if (baseUrl.substring(baseUrl.length() -1).equals("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            System.setProperty(Constants.TRUSTSTORE, trustStoreAbsolutePath);
            System.setProperty(Constants.TRUSTSTORE_PASSWORD, trustStorePassword);

            String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            // Define the folder structure
            String folderPath = "results/" + currentDate;
            String filePath = folderPath + "/result.txt";

            // Create the folder structure
            File folder = new File(folderPath);

            if (folder.exists()) {
                boolean isDeleteSuccess = deleteFiles(folder);
                if (!isDeleteSuccess) {
                    log.error("Error deleting the existing directory content. " +
                            "Please check if you have currently using the any of the files");
                    return;
                }
            } else {
                boolean isDirectoryCreationSuccess = folder.mkdirs();
                if (!isDirectoryCreationSuccess) {
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

            ResourceAdminServiceAdminClient resourceAdminServiceAdminClient = new
                    ResourceAdminServiceAdminClient(baseUrl, userName, password);
            resourceAdminServiceAdminClient.validateCollectionContent();

            SwaggerValidateUtils.writeStatsSummary(fileWriter);

            if (doDownload) {
                // need to download invalid/malformed swaggers
                downloadErrorFiles(folderPath);
            }

        } catch (IOException | ResourceAdminServiceExceptionException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static boolean deleteFiles(File folder) {
        boolean isDeleteSuccess;
        if (folder.isFile() || (folder.isDirectory() && folder.list().length == 0)) {
            isDeleteSuccess = folder.delete();
            if (!isDeleteSuccess) {
                return false;
            }
        } else {
            String[] entries = folder.list();
            for (String entry : entries) {
                File file = new File(folder.getPath(), entry);
                deleteFiles(file);
            }
        }
        return true;
    }

    protected static void downloadErrorFiles(String folderPath) throws IOException {
        folderPath = folderPath.concat(File.separator + "errorSwaggers");
        File folder = new File(folderPath);
        if (!folder.exists()) {
            boolean isDirectoryCreationSuccess = folder.mkdirs();
            if (!isDirectoryCreationSuccess) {
                log.error("Error creating folder structure.");
                return;
            }
        }
        for (String swaggerPathName : errorSwaggers.keySet()) {
            File outputFile = new File(folderPath + File.separator + swaggerPathName);
            try {
                outputFile.createNewFile();
            } catch (IOException e) {
                log.error("Error creating file: " + outputFile);
                e.printStackTrace();
            }
            FileWriter writer = new FileWriter(outputFile);
            writer.write(errorSwaggers.get(swaggerPathName));
            writer.close();
        }
    }
}
