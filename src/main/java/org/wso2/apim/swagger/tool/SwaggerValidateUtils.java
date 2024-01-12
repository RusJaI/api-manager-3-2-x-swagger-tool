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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.v3.parser.ObjectMapperFactory;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class contains the set of functions for validating swagger files
 */
public class SwaggerValidateUtils {
    private static final Logger log = LoggerFactory.getLogger(SwaggerTool.class);
    static int totalFileCount = 0;
    static int validationFailedFileCount = 0; // errors identifying while parsing
    static int validationSuccessFileCount = 0;
    static int totalMalformedSwaggerFiles = 0; //not a swagger (cannot parse at all)
    static int totalPartiallyParsedSwaggerFiles = 0;
    static List<String> errorList;


    /**
     * @param swaggerFileContent swagger file content to be validated
     */
    public static boolean validateSwaggerContent(String abstractedFilePath,
                                              String swaggerFileContent) {
        totalFileCount ++;

        errorList = new ArrayList();
        List<Object> swaggerTypeAndName = getSwaggerVersion(swaggerFileContent);

        if (swaggerTypeAndName.size() == 1) { //something went wrong while parsing OAS definition
            errorList.add("Error occurred while parsing OAS definition. " + swaggerTypeAndName.get(0).toString());
            totalMalformedSwaggerFiles++;
            return true;
        }

        // after parsing the OAS definition, errors found in its version or name
        if (swaggerTypeAndName.size() == 2) {
            if (swaggerTypeAndName.get(1) == null) {
                log.error("Invalid OpenAPI : Error: " + Constants.OPENAPI_NAME_NOT_FOUND_ERROR_CODE);
                errorList.add(Constants.OPENAPI_NAME_NOT_FOUND_ERROR_CODE);
                totalMalformedSwaggerFiles++;
                return true;

            } else if (swaggerTypeAndName.get(0).equals(Constants.SwaggerVersion.ERROR)) {
                log.error("Invalid OpenAPI : " + swaggerTypeAndName.get(1).toString() +
                        " , Error: " + Constants.OPENAPI_VERSION_NOT_FOUND_ERROR_CODE);
                errorList.add(Constants.OPENAPI_VERSION_NOT_FOUND_ERROR_CODE);
                totalMalformedSwaggerFiles++;
                return true;
            }
            // In this block, only the error scenarios are considered.
            // correct type, name combination is also of size 2 which is not addressed within the block
        }

        if (swaggerTypeAndName.get(0).equals(Constants.SwaggerVersion.SWAGGER)) {
            log.info("---------------- Parsing Started SwaggerName \"" + swaggerTypeAndName.get(1).toString() +
                    "\" ----------------");
            swagger2Validator(swaggerFileContent);
            log.info("---------------- Parsing Complete SwaggerName \"" + swaggerTypeAndName.get(1).toString() +
                    "\" ---------------- \n");
        } else if (swaggerTypeAndName.get(0).equals(Constants.SwaggerVersion.OPEN_API)) {
            log.info("---------------- Parsing Started openApiName \"" + swaggerTypeAndName.get(1).toString() +
                    "\" ----------------");
            swagger3Validator(swaggerFileContent);

            log.info("---------------- Parsing Complete openApiName \"" + swaggerTypeAndName.get(1).toString() +
                    "\" ----------------\n");
        }

        SwaggerTool.errorResultsMap.put(abstractedFilePath, errorList);

        return errorList.size() > 0;
    }

    /**
     *This method is used to get the swagger type and name from the swagger file
     * @param apiDefinition of type String
     * @return List containing swagger type and name
     * Following value combinations can be returned by the list :
     *  size =1 -> parse error
     *  size =2 -> parse success with following possibilities
     *           a) correct type and name
     *           b) error and name!=null
     *           c) error and name=null
     * Note : empty info block is captured when validating the swagger
     */
    public static List<Object> getSwaggerVersion(String apiDefinition) {

        List<Object> swaggerTypeAndName = new ArrayList<>();
        ObjectMapper mapper;
        if (apiDefinition.trim().startsWith("{")) {
            mapper = ObjectMapperFactory.createJson();
        } else {
            mapper = ObjectMapperFactory.createYaml();
        }
        JsonNode rootNode;
        ObjectNode node;
        try {
            rootNode = mapper.readTree(apiDefinition.getBytes());
            node = (ObjectNode) rootNode;
        } catch (Exception e) {
            log.error("Error occurred while parsing OAS definition. Verify the provided definition format: " +
                    e.getMessage());
            swaggerTypeAndName.add(e.getMessage());
            return swaggerTypeAndName;
        }
        String name = getSwaggerFileName(node.get("info"));
        JsonNode openapi = node.get("openapi");
        if (openapi != null && openapi.asText().startsWith("3.")) {
            swaggerTypeAndName.add(Constants.SwaggerVersion.OPEN_API);
            swaggerTypeAndName.add(name);
            return swaggerTypeAndName;
        }
        JsonNode swagger = node.get("swagger");
        if (swagger != null) {
            swaggerTypeAndName.add(Constants.SwaggerVersion.SWAGGER);
            swaggerTypeAndName.add(name);
            return swaggerTypeAndName;
        }

        log.error("Invalid OAS definition provided.");
        swaggerTypeAndName.add(Constants.SwaggerVersion.ERROR);
        swaggerTypeAndName.add(name);
        return swaggerTypeAndName;
    }

    public static String getSwaggerFileName(JsonNode node) {
        if (node != null) {
            if (node.has("title")) {
                return node.get("title").asText();
            }
        }
        return null;
    }

    public static void swagger2Validator(String swagger) {

        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult parseAttemptForV2 = parser.readWithInfo(swagger);

        if (parseAttemptForV2.getMessages().size() > 0) {

            for (String message : parseAttemptForV2.getMessages()) {
                StringBuilder errorMessageBuilder = new StringBuilder("Invalid Swagger");
                errorMessageBuilder.append(", Error: ").append(message);
                errorList.add(message);
                log.error(errorMessageBuilder.toString());
            }
            validationFailedFileCount++;

            if (parseAttemptForV2.getSwagger() != null) {
                log.info("Swagger parsed with errors, using may lead to functionality issues.");
                errorList.add("Swagger parsed with errors, using may lead to functionality issues");
                totalPartiallyParsedSwaggerFiles++;
            } else {
                log.error("Malformed Swagger, Please fix the listed issues before proceeding");
                errorList.add("Malformed Swagger, Please fix the listed issues before proceeding");
                totalMalformedSwaggerFiles++;
            }
        } else {
            if (parseAttemptForV2.getSwagger() != null) {
                log.info("Swagger file is valid");

                validationSuccessFileCount++;
            } else {
                log.error(Constants.UNABLE_TO_RENDER_THE_DEFINITION_ERROR);
                errorList.add(Constants.UNABLE_TO_RENDER_THE_DEFINITION_ERROR);
                validationFailedFileCount++;
            }
        }
    }

    public static void swagger3Validator(String swagger) {

        OpenAPIV3Parser openAPIV3Parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        SwaggerParseResult parseResult = openAPIV3Parser.readContents(swagger, null, options);
        if (parseResult.getMessages().size() > 0) {

            for (String message : parseResult.getMessages()) {

                StringBuilder errorMessageBuilder = new StringBuilder("Invalid OpenAPI");

                errorMessageBuilder.append(", Error: ").append(Constants.OPENAPI_PARSE_EXCEPTION_ERROR_MESSAGE)
                        .append(", Swagger Error: ").append(message);

                errorList.add(message);
                log.error(errorMessageBuilder.toString());

            }

            if (parseResult.getOpenAPI() != null) {
                log.info("OpenAPI parsed with errors, using may lead to functionality issues.");
                errorList.add("OpenAPI parsed with errors, using may lead to functionality issues");
                totalPartiallyParsedSwaggerFiles++;
            } else {
                log.error("Malformed OpenAPI, Please fix the listed issues before proceeding");
                errorList.add("Malformed OpenAPI, Please fix the listed issues before proceeding");
                ++totalMalformedSwaggerFiles;
            }

            validationFailedFileCount++;

        } else {
            if (parseResult.getOpenAPI() != null) {
                log.info("Swagger file is valid OpenAPI 3 definition");
                validationSuccessFileCount++;
            } else {
                log.error(Constants.UNABLE_TO_RENDER_THE_DEFINITION_ERROR);
                errorList.add(Constants.UNABLE_TO_RENDER_THE_DEFINITION_ERROR);
                validationFailedFileCount++;
            }
        }
    }

    static void writeStatsSummary (FileWriter fileWriter) throws IOException {

        writeResults(fileWriter, "API Name", "API Version", "API Provider",
                "Results");

        for (String key : SwaggerTool.errorResultsMap.keySet()) {
            List<String> values = SwaggerTool.errorResultsMap.get(key);
            if (values.size() > 0) {
                String [] apiData = key.split("/");
                for (int i = 0; i < values.size(); i++) {
                    if (i == 0) {
                        writeResults(fileWriter, apiData[apiData.length-3], apiData[apiData.length-2], getProvider(apiData), values.get(i));
                    } else {
                        writeResults(fileWriter, null, null, null, values.get(i));
                    }
                }
            }
        }

        fileWriter.append("\n---------------Summary ---------------- " +
                "\nTotal Files Processed: " + totalFileCount +
                "\nTotal Successful Files Count " + validationSuccessFileCount +
                "\nTotal Failed Files Count: " + validationFailedFileCount +
                "\nTotal Malformed Swagger File Count: " + totalMalformedSwaggerFiles +
                "\n\n");

        log.info("Summary --- Total Files Processed: " + totalFileCount + ". Total Successful Files Count "
                + validationSuccessFileCount + ". Total Failed Files Count: " + validationFailedFileCount + ". " +
                "Total Malformed Swagger File Count: " + totalMalformedSwaggerFiles);

        fileWriter.close();

    }

    private static String getProvider(String[] apiData) {
        String providerString = "";
        for (int i = 0; i < apiData.length-3; i++) {
            providerString = providerString + apiData[i];
        }
        return providerString;
    }

    protected static void writeResults(FileWriter fileWriter, String apiName, String apiVersion, String apiProvider, String results) throws IOException {
        fileWriter.append(String.format("%-30s %-20s %-30s %-60s%n", apiName == null ? "" : apiName,
                apiVersion == null ? "" : apiVersion, apiProvider == null ? "" : apiProvider,
                results == null ? "" : results + "\n"));
    }

}
