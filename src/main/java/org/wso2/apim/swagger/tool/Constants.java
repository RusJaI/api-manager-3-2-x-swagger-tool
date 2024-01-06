package org.wso2.apim.swagger.tool;

public class Constants {
    public static final String OPENAPI_VERSION_NOT_FOUND_ERROR_CODE = "OpenAPI version Not found or invalid version found in the definition";
    public static final String OPENAPI_NAME_NOT_FOUND_ERROR_CODE = "OpenAPI name Not found in the definition";
    public static final String OPENAPI_PARSE_EXCEPTION_ERROR_MESSAGE = "Error while parsing OpenAPI definition";
    public static final String UNABLE_TO_RENDER_THE_DEFINITION_ERROR = "Unable to render this definition, " +
            "The provided definition does not specify a valid version field.";


    public static final String SCHEMA_REF_PATH = "#/components/schemas/";

    public enum SwaggerVersion {
        SWAGGER,
        OPEN_API,
        ERROR,
    }

}
