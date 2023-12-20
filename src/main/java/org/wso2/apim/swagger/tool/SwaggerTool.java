package org.wso2.apim.swagger.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.parser.OpenAPIParser;
import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.v3.parser.ObjectMapperFactory;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

public class SwaggerTool {

    private static final Logger log = LoggerFactory.getLogger(SwaggerTool.class);
    static int totalFileCount = 0;
    static int validationFailedFileCount = 0; // errors identifying while parsing
    static int validationSuccessFileCount = 0;
    static int totalMalformedSwaggerFiles = 0; //not a swagger (cannot parse)
    static int totalPartiallyParsedSwaggerFiles = 0;

    /**
     * No parameters are supported when executing the tool.
     */
    public static void main(String[] args) {

        String publisherApiEndpoint = "https://localhost:9443/api/am/publisher/v1";
        String username = "admin";
        String password = "admin";

        ObjectMapper objectMapper   = new ObjectMapper();
        String swaggerContent = "";

        // create the output file
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("report.txt", true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            // Create an HTTP client

            // Path to the keystore file
            String keystorePath = "/Users/rusirijayodaillesinghe/Documents/APIM_Repos/api-manager-3-2-x-swagger-tool" +
                    "/src/main/resources/jks/wso2carbon.jks";

            // Keystore password
            String keystorePassword = "wso2carbon";

            // Create an HTTP client with custom SSL settings
            HttpClient httpClient = HttpClients.custom()
                    .setSSLContext(SSLContextBuilder.create().loadTrustMaterial(new File(keystorePath),
                            keystorePassword.toCharArray()).build())
                    .build();

            // Create an HTTP GET request with query parameters
            URIBuilder uriBuilder = new URIBuilder(publisherApiEndpoint + "/apis");
            uriBuilder.setParameter("limit", String.valueOf(100000));
            uriBuilder.setParameter("offset", "0");
            uriBuilder.setParameter("expand", String.valueOf(false));

            // Build the URI with query parameters
            URI uri = uriBuilder.build();

            // Create an HTTP GET request to retrieve APIs
            HttpGet httpGet = new HttpGet(uri);

            // Set basic authentication credentials
            String credentials = username + ":" + password;
            String base64Credentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
            httpGet.setHeader("Authorization", "Basic " + base64Credentials);
            httpGet.setHeader("accept","application/json");

            // Execute the request and get the response
            HttpResponse response = httpClient.execute(httpGet);

            // Check if the request was successful (status code 200)
            if (response.getStatusLine().getStatusCode() == 200) {
                // Get the response content as a string
                String responseBody = EntityUtils.toString(response.getEntity());

                JsonNode swaggerNode = objectMapper.readTree(responseBody);

                JsonNode apiListNode = swaggerNode.get("list");

                totalFileCount = apiListNode.size();

                if (apiListNode.size() > 0) {
                    fileWriter.append(String.format("%-20s %-20s %-20s%n", "API ID", "API Name", "Errors"));

                    String swaggerEndpoint = "https://localhost:9443/api/am/publisher/v1/apis/";

                    //apiListNode should be an array
                    for (JsonNode apiNode : apiListNode) {
                        //get the apiid and remove double quotes
                        String apiId = apiNode.get("id").toString().replaceAll("^\"|\"$", "");

                        // start writing API entry to report
                        fileWriter.append(String.format("%-20s %-20s%n", apiId, apiNode.get("name").toString()
                                .replaceAll("^\"|\"$", "")));

                        uriBuilder = new URIBuilder(swaggerEndpoint + apiId + "/swagger");

                        // Create an HTTP GET request to retrieve APIs
                        httpGet = new HttpGet(uriBuilder.build());
                        httpGet.setHeader("Authorization", "Basic " + base64Credentials);
                        httpGet.setHeader("accept","application/json");

                        // Execute the request and get the response
                        response = httpClient.execute(httpGet);

                        if (response.getStatusLine().getStatusCode() == 200) {
                            // Get the response content as a string
                            swaggerContent = EntityUtils.toString(response.getEntity());

                            validateSwaggerContent(swaggerContent, fileWriter);

                        } else {
                            log.error("Error: " + response.getStatusLine().getStatusCode() + " - " +
                                    response.getStatusLine().getReasonPhrase());
                        }

                    }

                } else {
                    fileWriter.append("---------No APIs found in the API Manager Instance----------");
                    log.error("No APIs found in the API Manager Instance");
                }

            } else {
                log.error("Error: " + response.getStatusLine().getStatusCode() + " - " +
                        response.getStatusLine().getReasonPhrase());
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }

        try {
            fileWriter.append("\n---------------Summary ---------------- " +
                    "\nTotal Files Processed: " + totalFileCount +
                    "\nTotal Successful Files Count " + validationSuccessFileCount +
                    "\nTotal Failed Files Count: " + validationFailedFileCount +
                    "\nTotal Malformed Swagger File Count: " + totalMalformedSwaggerFiles +
                    "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.info("Summary --- Total Files Processed: " + totalFileCount + ". Total Successful Files Count "
                + validationSuccessFileCount + ". Total Failed Files Count: " + validationFailedFileCount + ". " +
                "Total Malformed Swagger File Count: " + totalMalformedSwaggerFiles);

        try {
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param swaggerFileContent swagger file content to be validated
     */
    public static void validateSwaggerContent(String swaggerFileContent, FileWriter fileWriter) throws IOException {
        List<Object> swaggerTypeAndName = getSwaggerVersion(swaggerFileContent);

        if (swaggerTypeAndName.size() == 1) { //something went wrong while parsing OAS definition

            fileWriter.append(String.format("%-20s %-20s %-20s%n", "", "", "Error occurred while parsing OAS definition."
                    + swaggerTypeAndName.get(0).toString()));

            totalMalformedSwaggerFiles++;

            return;
        }

        // after parsing the OAS definition, errors found in its version or name
        if (swaggerTypeAndName.size() == 2) {
            if (swaggerTypeAndName.get(1) == null) {
                log.error("Invalid OpenAPI : Error: " + Constants.OPENAPI_NAME_NOT_FOUND_ERROR_CODE);

                // writing API entry to report
                fileWriter.append(String.format("%-20s %-20s %-20s%n", "", "", "Invalid OpenAPI :  Error: " +
                        Constants.OPENAPI_NAME_NOT_FOUND_ERROR_CODE));
                totalMalformedSwaggerFiles++;
                return;

            } else if (swaggerTypeAndName.get(0).equals(Constants.SwaggerVersion.ERROR)) {
                log.error("Invalid OpenAPI : " + swaggerTypeAndName.get(1).toString() +
                        " , Error: " + Constants.OPENAPI_VERSION_NOT_FOUND_ERROR_CODE);

                // writing API entry to report
                fileWriter.append(String.format("%-20s %-20s %-20s%n", "", "", "Invalid OpenAPI : " +
                        swaggerTypeAndName.get(1).toString() + " , Error: " +
                        Constants.OPENAPI_VERSION_NOT_FOUND_ERROR_CODE));
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
            boolean isOpenAPIMissing = swagger3Validator(swaggerFileContent, fileWriter);
            if (isOpenAPIMissing) {
                swagger2Validator(swaggerFileContent, fileWriter);
            }
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

    public static boolean swagger2Validator(String swagger, FileWriter fileWriter) throws IOException {
        boolean isSwaggerMissing = false;
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult parseAttemptForV2 = parser.readWithInfo(swagger);

        if (parseAttemptForV2.getMessages().size() > 0) {

            for (String message : parseAttemptForV2.getMessages()) {
                StringBuilder errorMessageBuilder = new StringBuilder("Invalid Swagger, Error Code: ");
                if (message.contains(Constants.SWAGGER_IS_MISSING_MSG)) {
                    isSwaggerMissing = true;
                } else {
                    // Since OpenAPIParser coverts the $ref to #/components/schemas/ when validating
                    // we need to replace #/components/schemas/ with #/definitions/ before printing the message
                    if (message.contains(Constants.SCHEMA_REF_PATH)) {
                        message = message.replace(Constants.SCHEMA_REF_PATH, "#/definitions/");
                    }
                }
                errorMessageBuilder.append(", Error: ").append(message);

                // write to file
                fileWriter.append(String.format("%-20s %-20s %-20s%n", "", "", message));

                log.error(errorMessageBuilder.toString());
            }
            validationFailedFileCount++;

            if (parseAttemptForV2.getSwagger() != null) {
                log.info("Swagger parsed with errors, using may lead to functionality issues.");
                fileWriter.append(String.format("%-20s %-20s %-20s%n", "", "",
                        "Swagger parsed with errors, using may lead to functionality issues"));
                totalPartiallyParsedSwaggerFiles++;
            } else {
                log.error("Malformed Swagger, Please fix the listed issues before proceeding");
                fileWriter.append(String.format("%-20s %-20s %-20s%n", "", "",
                        "Malformed Swagger, Please fix the listed issues before proceeding"));

                totalMalformedSwaggerFiles++;
            }
        } else {
            if (parseAttemptForV2.getSwagger() != null) {
                log.info("Swagger file is valid");
                fileWriter.append(String.format("%-20s %-20s %-20s%n", "", "",
                        "Swagger file is valid"));

                validationSuccessFileCount++;
            } else {
                log.error(Constants.UNABLE_TO_RENDER_THE_DEFINITION_ERROR);
                fileWriter.append(String.format("%-20s %-20s %-20s%n", "", "",
                        Constants.UNABLE_TO_RENDER_THE_DEFINITION_ERROR));

                validationFailedFileCount++;
            }
        }
        return isSwaggerMissing;
    }

    public static boolean swagger3Validator(String swagger, FileWriter fileWriter) throws IOException {
        boolean isOpenAPIMissing = false;

        OpenAPIV3Parser openAPIV3Parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        SwaggerParseResult parseResult = openAPIV3Parser.readContents(swagger, null, options);
        if (parseResult.getMessages().size() > 0) {

            for (String message : parseResult.getMessages()) {

                StringBuilder errorMessageBuilder = new StringBuilder("Invalid OpenAPI, Error Code: ");
                errorMessageBuilder.append(", Error: ").append(message);

                if (message.contains(Constants.OPENAPI_IS_MISSING_MSG)) {
                    isOpenAPIMissing = true;
                } else {
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
                }

                // write to file
                fileWriter.append(String.format("%-20s %-20s %-20s%n", "", "", message));

                log.error(errorMessageBuilder.toString());
            }

            if (!isOpenAPIMissing) {
                if (parseResult.getOpenAPI() != null) {
                    log.info("OpenAPI parsed with errors, using may lead to functionality issues.");
                    fileWriter.append(String.format("%-20s %-20s %-20s%n", "", "",
                            "OpenAPI parsed with errors, using may lead to functionality issues"));

                    totalPartiallyParsedSwaggerFiles++;
                } else {
                    log.error("Malformed OpenAPI, Please fix the listed issues before proceeding");
                    fileWriter.append(String.format("%-20s %-20s %-20s%n", "", "",
                            "Malformed OpenAPI, Please fix the listed issues before proceeding"));

                    ++totalMalformedSwaggerFiles;
                }

                validationFailedFileCount++;

            }
        } else {
            if (parseResult.getOpenAPI() != null) {
                log.info("Swagger file is valid OpenAPI 3 definition");
                fileWriter.append(String.format("%-20s %-20s %-20s%n", "", "",
                        "Swagger file is valid OpenAPI 3 definition"));

                validationSuccessFileCount++;
            } else {
                log.error(Constants.UNABLE_TO_RENDER_THE_DEFINITION_ERROR);
                fileWriter.append(String.format("%-20s %-20s %-20s%n", "", "",
                        Constants.UNABLE_TO_RENDER_THE_DEFINITION_ERROR));

                validationFailedFileCount++;
            }
        }
        return isOpenAPIMissing;
    }

}
