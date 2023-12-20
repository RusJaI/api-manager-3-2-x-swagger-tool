package org.wso2.apim.swagger.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.parser.OpenAPIParser;
import io.swagger.parser.SwaggerParser;
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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;


public class SwaggerTool {

    private static final Logger log = LoggerFactory.getLogger(SwaggerTool.class);
    static int totalFileCount = 0;
    static int validationFailedFileCount = 0;
    static int validationSuccessFileCount = 0;
    static int totalMalformedSwaggerFiles = 0;
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

                if (apiListNode.size() > 0) {

                    String swaggerEndpoint = "https://localhost:9443/api/am/publisher/v1/apis/";

                    System.out.println("# APIs response:\n" + responseBody);
                    System.out.println("# APIs LIST:\n" + apiListNode);

                    //apiListNode should be an array
                    for (JsonNode apiNode : apiListNode) {
                        //get the apiid and remove double quotes
                        String apiId = apiNode.get("id").toString().replaceAll("^\"|\"$", "");
                        System.out.println("APIID : " + apiId);

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

                            System.out.println("SWAGGER for APIID " + apiId + " : " + responseBody);

                        } else {
                            System.err.println("Error: " + response.getStatusLine().getStatusCode() + " - " +
                                    response.getStatusLine().getReasonPhrase());
                        }

                    }

                } else {
                    System.out.println("No APIs found in the API Manager Instance");
                }

            } else {
                System.err.println("Error: " + response.getStatusLine().getStatusCode() + " - " +
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


        if (swaggerContent.startsWith("location:")) {
            validateSwaggerFromLocation(swaggerContent.replace("location:", ""));
        } else {
            validateSwaggerContent(swaggerContent);
        }
        log.info("Summary --- Total Files Processed: " + totalFileCount + ". Total Successful Files Count "
                + validationSuccessFileCount + ". Total Failed Files Count: " + validationFailedFileCount + ". " +
                "Total Malformed Swagger File Count: " + totalMalformedSwaggerFiles);
        
    }

    /**
     * @param url             url for the swagger file
     */
    static void validateSwaggerFromLocation(String url) {
        try {
            Path swaggerFilePath = Paths.get(url);
            if (Files.isRegularFile(swaggerFilePath)) {
                totalFileCount++;
                String swaggerFileContent = new String(Files.readAllBytes(swaggerFilePath), StandardCharsets.UTF_8);
                log.info("Start Parsing Swagger file " + swaggerFilePath.getFileName().toString());
                validateSwaggerContent(swaggerFileContent);
            } else if (Files.isDirectory(swaggerFilePath)) {
                try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(swaggerFilePath)) {
                    directoryStream.forEach((path) -> {
                        validateSwaggerFromLocation(path.toString());
                    });
                }
            } else {
                log.error("Error occurred while reading the provided file/folder, " +
                        "please verify the file/folder availability");
            }
        } catch (IOException e) {
            log.error("Error occurred while reading the swagger file from the give location " + url + ", hence the " +
                    "file will not be validated. ", e);
        }
    }

    /**
     * @param swaggerFileContent swagger file content to be validated
     */
    public static void validateSwaggerContent(String swaggerFileContent) {
        List<Object> swaggerTypeAndName = getSwaggerVersion(swaggerFileContent);

        if (swaggerTypeAndName.get(0).equals(Constants.SwaggerVersion.ERROR)) {
            if (swaggerTypeAndName.size() == 2) {
                log.info("---------------- Parsing Started SwaggerName \"" + swaggerTypeAndName.get(1).toString() +
                        "\" ----------------");
                boolean isOpenAPIMissing = swagger3Validator(swaggerFileContent);
                boolean isSwaggerMissing;
                if (isOpenAPIMissing) {
                    isSwaggerMissing = swagger2Validator(swaggerFileContent);
                    if (isSwaggerMissing) {
                        log.error("Invalid OpenAPI, Error Code: " + Constants.OPENAPI_PARSE_EXCEPTION_ERROR_CODE +
                                ", Error: " + Constants.OPENAPI_PARSE_EXCEPTION_ERROR_MESSAGE
                                + ", Swagger Error: " + Constants.SWAGGER_OR_OPENAPI_IS_MISSING_MSG);
                    }
                }
                log.info("---------------- Parsing Complete SwaggerName \"" + swaggerTypeAndName.get(1).toString() +
                        "\" ---------------- \n");
            }
            return;
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
            boolean isOpenAPIMissing = swagger3Validator(swaggerFileContent);
            if (isOpenAPIMissing) {
                swagger2Validator(swaggerFileContent);
            }
            log.info("---------------- Parsing Complete openApiName \"" + swaggerTypeAndName.get(1).toString() +
                    "\" ----------------\n");
        }

    }

    public static List<Object> getSwaggerVersion(String apiDefinition) {
        List<Object> swaggerTypeAndName = new ArrayList<>(2);
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
            log.error("Error occurred while parsing OAS definition. Verify the provided definition format: " + e.getMessage());
            swaggerTypeAndName.add(Constants.SwaggerVersion.ERROR);
            validationFailedFileCount++;
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
            return node.get("title").asText();
        }
        return "";
    }

    public static boolean swagger2Validator(String swagger) {
        boolean isSwaggerMissing = false;
        boolean isValidForAPIM = true;
        SwaggerParser swaggerParser = new SwaggerParser();
        OpenAPIParser parser= new OpenAPIParser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        options.setResolveFully(true);
        SwaggerParseResult parseAttemptForV2 = parser.readContents(swagger, new ArrayList<>(), options);
        if (parseAttemptForV2.getMessages().size() > 0) {

            for (String message : parseAttemptForV2.getMessages()) {
                StringBuilder errorMessageBuilder = new StringBuilder("Invalid Swagger, Error Code: ");
                if (message.contains(Constants.SWAGGER_IS_MISSING_MSG)) {
                    errorMessageBuilder.append(Constants.INVALID_OAS2_FOUND_ERROR_CODE)
                            .append(", Error: ").append(Constants.INVALID_OAS2_FOUND_ERROR_MESSAGE)
                            .append(", Swagger Error: ").append(Constants.SWAGGER_IS_MISSING_MSG);
                    log.error(errorMessageBuilder.toString());
                    isSwaggerMissing = true;
                } else if (message.contains(Constants.MALFORMED_SWAGGER_ERROR)) {
                    errorMessageBuilder.append(Constants.OPENAPI_PARSE_EXCEPTION_ERROR_CODE)
                            .append(", Error: ").append(Constants.OPENAPI_PARSE_EXCEPTION_ERROR_MESSAGE)
                            .append(", Swagger Error: ").append(message);
                    try {
                        swaggerParser.parse(swagger);
                        log.error(errorMessageBuilder.toString());
                    } catch (Exception e) {
                        if (e.getMessage().contains(Constants.UNABLE_TO_LOAD_REMOTE_REFERENCE)) {
                            logRemoteReferenceIssues(swagger);
                        } else {
                            errorMessageBuilder.append(", Cause by: ").append(e.getMessage());
                            log.error(errorMessageBuilder.toString());
                        }
                    }
                } else {
                    if (isSchemaMissing(message)) {
                        isValidForAPIM = false;
                    }
                    // Since OpenAPIParser coverts the $ref to #/components/schemas/ when validating
                    // we need to replace #/components/schemas/ with #/definitions/ before printing the message
                    if (message.contains(Constants.SCHEMA_REF_PATH)) {
                        message = message.replace(Constants.SCHEMA_REF_PATH, "#/definitions/");
                    }
                    errorMessageBuilder.append(Constants.OPENAPI_PARSE_EXCEPTION_ERROR_CODE)
                            .append(", Error: ").append(Constants.OPENAPI_PARSE_EXCEPTION_ERROR_MESSAGE)
                            .append(", Swagger Error: ").append(message);
                    log.error(errorMessageBuilder.toString());
                }
            }
            validationFailedFileCount++;

            if (parseAttemptForV2.getOpenAPI() != null) {
                log.info("Swagger passed with errors, using may lead to functionality issues.");
                totalPartiallyParsedSwaggerFiles++;
            } else {
                isValidForAPIM = false;
                log.error("Malformed Swagger, Please fix the listed issues before proceeding");
                totalMalformedSwaggerFiles++;
            }
        } else {
            if (parseAttemptForV2.getOpenAPI() != null) {
                log.info("Swagger file is valid");
                validationSuccessFileCount++;
            } else {
                isValidForAPIM = false;
                log.error(Constants.UNABLE_TO_RENDER_THE_DEFINITION_ERROR);
                validationFailedFileCount++;
            }
        }
        if (isValidForAPIM) {
            log.info("Swagger file will be accepted by the APIM 4.0.0 ");
        }
        return isSwaggerMissing;
    }

    private static boolean isSchemaMissing(String errorMessage) {
        return errorMessage.contains(Constants.SCHEMA_REF_PATH) && errorMessage.contains("is missing");
    }

    public static boolean swagger3Validator(String swagger) {
        boolean isOpenAPIMissing = false;

        OpenAPIV3Parser openAPIV3Parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        SwaggerParseResult parseResult = openAPIV3Parser.readContents(swagger, null, options);
        if (parseResult.getMessages().size() > 0) {

            for (String message : parseResult.getMessages()) {
                StringBuilder errorMessageBuilder = new StringBuilder("Invalid OpenAPI, Error Code: ");
                if (message.contains(Constants.UNABLE_TO_LOAD_REMOTE_REFERENCE)) {
                    logRemoteReferenceIssues(swagger);
                } else if (message.contains(Constants.OPENAPI_IS_MISSING_MSG)) {
                    errorMessageBuilder.append(Constants.INVALID_OAS3_FOUND_ERROR_CODE)
                            .append(", Error: ").append(Constants.INVALID_OAS3_FOUND_ERROR_MESSAGE);
                    log.error(errorMessageBuilder.toString());
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
                    errorMessageBuilder.append(Constants.OPENAPI_PARSE_EXCEPTION_ERROR_CODE)
                            .append(", Error: ").append(Constants.OPENAPI_PARSE_EXCEPTION_ERROR_MESSAGE)
                            .append(", Swagger Error: ").append(message);
                    log.error(errorMessageBuilder.toString());
                }
            }

            if (!isOpenAPIMissing) {
                if (parseResult.getOpenAPI() != null) {
                    log.info("OpenAPI passed with errors, using may lead to functionality issues.");
                    totalPartiallyParsedSwaggerFiles++;
                } else {
                    log.error("Malformed OpenAPI, Please fix the listed issues before proceeding");
                    ++totalMalformedSwaggerFiles;
                }

                validationFailedFileCount++;

            }
        } else {
            if (parseResult.getOpenAPI() != null) {
                log.info("Swagger file is valid OpenAPI 3 definition");
                validationSuccessFileCount++;
            } else {
                log.error(Constants.UNABLE_TO_RENDER_THE_DEFINITION_ERROR);
                validationFailedFileCount++;
            }
        }
        return isOpenAPIMissing;
    }

    /**
     * This method will log the remote references in the given Swagger or OpenAPI definition.
     * @param apiDefinition Swagger or OpenAPI definition
     */
    public static void logRemoteReferenceIssues(String apiDefinition) {
        log.warn("Validate the following remote references and make sure that they are valid and accessible:");

        // Parse the Swagger or OpenAPI definition and extract the remote references by picking
        // the values of the $ref ke
        ObjectMapper mapper;
        if (apiDefinition.trim().startsWith("{")) {
            mapper = ObjectMapperFactory.createJson();
        } else {
            mapper = ObjectMapperFactory.createYaml();
        }

        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(apiDefinition);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        List<JsonNode> refValues = findRefValues(rootNode);

        for (JsonNode refValue : refValues) {
            String remoteReference = refValue.toString();

            // If schema reference starts with #/components/schemas/ (OAS 3 ref objects) or #/definitions/ (Swagger ref objects), it is a local reference.
            // Hence, if reference does not start with a "#/", it is a remote reference.
            if (!remoteReference.startsWith("\"#/")) {
                log.warn(refValue.toString());
            }
        }
    }

    /**
     * This method will recursively traverse the given JSON node and return a list of all the $ref values.
     * @param node JSON node
     * @return List of $ref values
     */
    public static List<JsonNode> findRefValues(JsonNode node) {
        List<JsonNode> refValues = new ArrayList<>();

        if (node instanceof ObjectNode) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fields().forEachRemaining(entry -> {
                if (entry.getKey().equals("$ref")) {
                    refValues.add(entry.getValue());
                } else {
                    refValues.addAll(findRefValues(entry.getValue()));
                }
            });
        } else if (node instanceof ArrayNode) {
            ArrayNode arrayNode = (ArrayNode) node;
            arrayNode.forEach(element -> refValues.addAll(findRefValues(element)));
        }

        return refValues;
    }
}
