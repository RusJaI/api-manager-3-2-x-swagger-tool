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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceStub;
import org.wso2.carbon.registry.resource.stub.beans.xsd.ResourceTreeEntryBean;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class contains the logic to invoke the ResourceAdminServices.
 */
public class ResourceAdminServiceAdminClient {
    private final String serviceName = "ResourceAdminService";
    private String resourcePath = "_system/governance/apimgt/applicationdata/provider/";
    private ResourceAdminServiceStub resourceAdminServiceStub;
    private String endPoint;

    private static final Log log = LogFactory.getLog(ResourceAdminServiceAdminClient.class);

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

        List<String> resourceFilePathList = new ArrayList<>();
        traverseApiResourceTree(resourcePath, resourceFilePathList);

        for (String resourceFilePath : resourceFilePathList) {
            String swaggerJson = resourceAdminServiceStub.getTextContent(resourceFilePath).toString();
            boolean hasErrors = SwaggerValidateUtils.validateSwaggerContent(resourceFilePath.
                    substring(resourcePath.length() + 1), swaggerJson);
            if (hasErrors) {
                SwaggerTool.errorSwaggers.put(getSwaggerName(resourceFilePath.substring(
                        resourcePath.length() + 1)), swaggerJson);
            }
        }
    }

    private String getSwaggerName(String swaggerPath) {
        String[] parts = swaggerPath.split("/");
        int pathSize = parts.length;
        return parts[pathSize-3] + "-" + parts[pathSize-2] + ".json";
    }

    /**
     * Get the leaf nodes traversing the api directories. resourceTreeEntry.getCollection() returns whether
     * it's a directory or a leaf node containing the api and the swagger.json.
     *
     * @param path String path to access the resource
     * @param resourcePathList List<String> list of swagger paths
     * @return List<String> list of swagger paths
     * @throws RemoteException
     * @throws ResourceAdminServiceExceptionException
     */
    protected List<String> traverseApiResourceTree(String path, List<String> resourcePathList) throws
            RemoteException, ResourceAdminServiceExceptionException {
        if (path != null) {
            ResourceTreeEntryBean resourceTreeEntry =  resourceAdminServiceStub.getResourceTreeEntry(path);
            if (resourceTreeEntry.getCollection()) { // collection=true if a directory
                String[] childPaths = resourceTreeEntry.getChildren();
                for (String childPath : childPaths) {
                    traverseApiResourceTree(childPath, resourcePathList);
                }
            } else {
                if (path.endsWith("swagger.json")) {
                    resourcePathList.add(path);
                    log.info("leaf file path: " + path);
                }
            }
        }
        return resourcePathList;
    }

}
