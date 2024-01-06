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

public class SwaggerValidateUtils {
    private static final Logger log = LoggerFactory.getLogger(SwaggerTool.class);
    static int totalFileCount = 0;
    static int validationFailedFileCount = 0; // errors identifying while parsing
    static int validationSuccessFileCount = 0;
    static int totalMalformedSwaggerFiles = 0; //not a swagger (cannot parse at all)
    static int totalPartiallyParsedSwaggerFiles = 0;


    /**
     * @param swaggerFileContent swagger file content to be validated
     */
    public static void validateSwaggerContent(String swaggerFileContent, FileWriter fileWriter) throws IOException {
        totalFileCount ++;

        List<Object> swaggerTypeAndName = getSwaggerVersion(swaggerFileContent);

        if (swaggerTypeAndName.size() == 1) { //something went wrong while parsing OAS definition

            writeResults(fileWriter, null, null, null,
                    "Error occurred while parsing OAS definition. " + swaggerTypeAndName.get(0).toString());

            totalMalformedSwaggerFiles++;

            return;
        }

        // after parsing the OAS definition, errors found in its version or name
        if (swaggerTypeAndName.size() == 2) {
            if (swaggerTypeAndName.get(1) == null) {
                log.error("Invalid OpenAPI : Error: " + Constants.OPENAPI_NAME_NOT_FOUND_ERROR_CODE);

                // writing API entry to report
                writeResults(fileWriter, null, null, null, "Invalid OpenAPI :  Error: "
                        + Constants.OPENAPI_NAME_NOT_FOUND_ERROR_CODE);
                totalMalformedSwaggerFiles++;
                return;

            } else if (swaggerTypeAndName.get(0).equals(Constants.SwaggerVersion.ERROR)) {
                log.error("Invalid OpenAPI : " + swaggerTypeAndName.get(1).toString() +
                        " , Error: " + Constants.OPENAPI_VERSION_NOT_FOUND_ERROR_CODE);

                // writing API entry to report
                writeResults(fileWriter, null, null, null, "Invalid OpenAPI : " +
                        swaggerTypeAndName.get(1).toString() + " , Error: " +
                        Constants.OPENAPI_VERSION_NOT_FOUND_ERROR_CODE);
                totalMalformedSwaggerFiles++;
                return;
            }
            // In this block, only the error scenarios are considered.
            // correct type, name combination is also of size 2 which is not addressed within the block
        }

        if (swaggerTypeAndName.get(0).equals(Constants.SwaggerVersion.SWAGGER)) {
            log.info("---------------- Parsing Started SwaggerName \"" + swaggerTypeAndName.get(1).toString() +
                    "\" ----------------");
            swagger2Validator(swaggerFileContent, fileWriter);
            log.info("---------------- Parsing Complete SwaggerName \"" + swaggerTypeAndName.get(1).toString() +
                    "\" ---------------- \n");
        } else if (swaggerTypeAndName.get(0).equals(Constants.SwaggerVersion.OPEN_API)) {
            log.info("---------------- Parsing Started openApiName \"" + swaggerTypeAndName.get(1).toString() +
                    "\" ----------------");
            swagger3Validator(swaggerFileContent, fileWriter);

            log.info("---------------- Parsing Complete openApiName \"" + swaggerTypeAndName.get(1).toString() +
                    "\" ----------------\n");
        }

    }

    public static List<Object> getSwaggerVersion(String apiDefinition) {
        // size =1 -> parse error
        // size =2 -> parse success with following possibilities
        // a) correct type and name
        // b) error and name!=null
        // c) error and name=null
        // empty info block is captured when validating

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

    public static void swagger2Validator(String swagger, FileWriter fileWriter) throws IOException {

        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult parseAttemptForV2 = parser.readWithInfo(swagger);

        if (parseAttemptForV2.getMessages().size() > 0) {

            for (String message : parseAttemptForV2.getMessages()) {
                StringBuilder errorMessageBuilder = new StringBuilder("Invalid Swagger, Error Code: ");

                // Since OpenAPIParser coverts the $ref to #/components/schemas/ when validating
                // we need to replace #/components/schemas/ with #/definitions/ before printing the message
                if (message.contains(Constants.SCHEMA_REF_PATH)) {
                    message = message.replace(Constants.SCHEMA_REF_PATH, "#/definitions/");
                }

                errorMessageBuilder.append(", Error: ").append(message);

                // write to file
                writeResults(fileWriter, null, null, null, message);

                log.error(errorMessageBuilder.toString());
            }
            validationFailedFileCount++;

            if (parseAttemptForV2.getSwagger() != null) {
                log.info("Swagger parsed with errors, using may lead to functionality issues.");
                writeResults(fileWriter, null, null, null,
                        "Swagger parsed with errors, using may lead to functionality issues");
                totalPartiallyParsedSwaggerFiles++;
            } else {
                log.error("Malformed Swagger, Please fix the listed issues before proceeding");
                writeResults(fileWriter, null, null, null,
                        "Malformed Swagger, Please fix the listed issues before proceeding");
                totalMalformedSwaggerFiles++;
            }
        } else {
            if (parseAttemptForV2.getSwagger() != null) {
                log.info("Swagger file is valid");
                writeResults(fileWriter, null, null, null, "Swagger file is valid");

                validationSuccessFileCount++;
            } else {
                log.error(Constants.UNABLE_TO_RENDER_THE_DEFINITION_ERROR);
                writeResults(fileWriter, null, null, null,
                        Constants.UNABLE_TO_RENDER_THE_DEFINITION_ERROR);

                validationFailedFileCount++;
            }
        }
    }

    public static void swagger3Validator(String swagger, FileWriter fileWriter) throws IOException {

        OpenAPIV3Parser openAPIV3Parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        SwaggerParseResult parseResult = openAPIV3Parser.readContents(swagger, null, options);
        if (parseResult.getMessages().size() > 0) {

            for (String message : parseResult.getMessages()) {

                StringBuilder errorMessageBuilder = new StringBuilder("Invalid OpenAPI, Error Code: ");
                errorMessageBuilder.append(", Error: ").append(message);

                // If the error message contains "schema is unexpected", we modify the error message notifying
                // that the schema object is not adhering to the OpenAPI Specification. Also, we add a note to
                // verify the reference object is of the format $ref: '#/components/schemas/{schemaName}'
                if (message.contains("schema is unexpected")) {
                    message = message.concat(". Please verify whether the schema object is adhering to " +
                            "the OpenAPI Specification. Make sure that the reference object is of " +
                            "format $ref: '#/components/schemas/{schemaName}'");
                }
                errorMessageBuilder.append(", Error: ").append(Constants.OPENAPI_PARSE_EXCEPTION_ERROR_MESSAGE)
                        .append(", Swagger Error: ").append(message);

                // write to file
                writeResults(fileWriter, null, null, null, message);

                log.error(errorMessageBuilder.toString());
            }


            if (parseResult.getOpenAPI() != null) {
                log.info("OpenAPI parsed with errors, using may lead to functionality issues.");
                writeResults(fileWriter, null, null, null,
                        "OpenAPI parsed with errors, using may lead to functionality issues");

                totalPartiallyParsedSwaggerFiles++;
            } else {
                log.error("Malformed OpenAPI, Please fix the listed issues before proceeding");
                writeResults(fileWriter, null, null, null,
                        "Malformed OpenAPI, Please fix the listed issues before proceeding");

                ++totalMalformedSwaggerFiles;
            }

            validationFailedFileCount++;

        } else {
            if (parseResult.getOpenAPI() != null) {
                log.info("Swagger file is valid OpenAPI 3 definition");
                writeResults(fileWriter, null, null, null,
                        "Swagger file is valid OpenAPI 3 definition");

                validationSuccessFileCount++;
            } else {
                log.error(Constants.UNABLE_TO_RENDER_THE_DEFINITION_ERROR);
                writeResults(fileWriter, null, null, null,
                        Constants.UNABLE_TO_RENDER_THE_DEFINITION_ERROR);

                validationFailedFileCount++;
            }
        }
    }

    static void writeStatsSummary (FileWriter fileWriter) throws IOException {

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

    protected static void writeResults(FileWriter fileWriter, String provider, String apiName,
                                     String apiVersion, String results) throws IOException {
        fileWriter.append(String.format("%-20s %-20s %-20s %-20s%n", provider == null ? "" : provider,
                apiName == null ? "" : apiName, apiVersion == null ? "" : apiVersion, results == null ? "" : results));

    }

}
