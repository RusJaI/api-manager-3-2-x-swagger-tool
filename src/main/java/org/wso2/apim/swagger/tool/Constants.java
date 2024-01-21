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

public class Constants {
    public static final String OPENAPI_VERSION_NOT_FOUND_ERROR_CODE =
            "OpenAPI version Not found or invalid version found in the definition";
    public static final String OPENAPI_NAME_NOT_FOUND_ERROR_CODE = "OpenAPI name Not found in the definition";
    public static final String OPENAPI_PARSE_EXCEPTION_ERROR_MESSAGE = "Error while parsing OpenAPI definition";
    public static final String UNABLE_TO_RENDER_THE_DEFINITION_ERROR = "Unable to render this definition, " +
            "The provided definition does not specify a valid version field.";
    public static final String TRUSTSTORE = "javax.net.ssl.trustStore";
    public static final String TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword";
    public static final String TRUSTSTORE_DEFAULT_SUBPATH = "/repository/resources/security/client-truststore.jks";

    public enum SwaggerVersion {
        SWAGGER,
        OPEN_API,
        ERROR,
    }

}
