package org.wso2.apim.swagger.tool;

public class Constants {

    public static final String SWAGGER_IS_MISSING_MSG = "swagger is missing";

    public static final String SWAGGER_OR_OPENAPI_IS_MISSING_MSG = "attribute swagger or openapi should present";
    public static final int INVALID_OAS2_FOUND_ERROR_CODE = 900761;
    public static final String INVALID_OAS2_FOUND_ERROR_MESSAGE = "Invalid OpenAPI V2 definition found";
    public static final String OPENAPI_IS_MISSING_MSG = "openapi is missing";
    public static final int INVALID_OAS3_FOUND_ERROR_CODE = 900762;

    public static final int OPENAPI_PARSE_EXCEPTION_ERROR_CODE = 900754;
    public static final String OPENAPI_PARSE_EXCEPTION_ERROR_MESSAGE = "Error while parsing OpenAPI definition";
    public static final String INVALID_OAS3_FOUND_ERROR_MESSAGE = "Invalid OpenAPI V3 definition found";
    public static final String MALFORMED_SWAGGER_ERROR = "malformed or unreadable swagger supplied";
    public static final String UNABLE_TO_RENDER_THE_DEFINITION_ERROR = "Unable to render this definition, " +
            "The provided definition does not specify a valid version field.";

    public static final String UNABLE_TO_LOAD_REMOTE_REFERENCE = "Unable to load RELATIVE ref:";

    public static final String SCHEMA_REF_PATH = "#/components/schemas/";

    public enum SwaggerVersion {
        SWAGGER,
        OPEN_API,
        ERROR,
    }

}
