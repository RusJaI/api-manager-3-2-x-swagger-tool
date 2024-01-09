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

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceStub;

import java.io.FileWriter;
import java.io.IOException;

/**
 * This class contains the logic to invoke the ResourceAdminServices.
 */
public class ResourceAdminServiceAdminClient {
    private final String serviceName = "ResourceAdminService";
    private String resourcePath = "_system/governance/apimgt/applicationdata/provider/";
    private ResourceAdminServiceStub resourceAdminServiceStub;
    private String endPoint;

    public ResourceAdminServiceAdminClient(String backEndUrl, String sessionCookie) throws AxisFault {
        this.endPoint = backEndUrl + "/services/" + serviceName;
        resourceAdminServiceStub = new ResourceAdminServiceStub(endPoint);

        //Authenticate Your stub from sessionCooke
        ServiceClient serviceClient;
        Options option;

        serviceClient = resourceAdminServiceStub._getServiceClient();
        option = serviceClient.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, sessionCookie);
    }

    public void validateCollectionContent()
            throws IOException, ResourceAdminServiceExceptionException {

        // create the output file
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("report.txt", true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        SwaggerValidateUtils.writeResults(fileWriter, "Provider", "API Name", "Version",
                "Results");

        String [] providers = resourceAdminServiceStub.getCollectionContent(resourcePath).getChildPaths();
        for (String provider : providers) {
            String [] apiNames =  resourceAdminServiceStub.getCollectionContent(provider)
                    .getChildPaths();

            for (String apiName : apiNames) {
                String [] versions = resourceAdminServiceStub.getCollectionContent(apiName).getChildPaths();

                for (String version : versions) {
                    //get the tree and check before getting the text
                    String swaggerJson = resourceAdminServiceStub.getTextContent(version +
                            "/swagger.json").toString();
        // add a null check. ex : swagger json can be missing
                    SwaggerValidateUtils.writeResults(fileWriter,
                            provider.substring(resourcePath.length() + 1),
                            apiName.substring(provider.length() + 1),
                            version.substring(apiName.length() + 1), null);

                    SwaggerValidateUtils.validateSwaggerContent(swaggerJson, fileWriter);
                }
            }
        }
        SwaggerValidateUtils.writeStatsSummary(fileWriter);
    }
}
