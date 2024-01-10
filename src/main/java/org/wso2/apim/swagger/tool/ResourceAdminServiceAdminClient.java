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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceStub;
import org.wso2.carbon.registry.resource.stub.beans.xsd.CollectionContentBean;

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

    private static final Logger log = LoggerFactory.getLogger(ResourceAdminServiceAdminClient.class);

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
        traverseResourceTree(resourcePath, resourceFilePathList);

        for (String resourceFilePath : resourceFilePathList) {
            String swaggerJson = resourceAdminServiceStub.getTextContent(resourceFilePath).toString();
            SwaggerValidateUtils.validateSwaggerContent(resourceFilePath.substring(resourcePath.length(),
                            resourceFilePath.length()), swaggerJson);
        }
    }

    protected List<String> traverseResourceTree(String path, List<String> resourcePathList)  {
        if (path != null) {
            try {
                CollectionContentBean collectionContentBean =  resourceAdminServiceStub.getCollectionContent(path);
                String[] childPaths = collectionContentBean.getChildPaths();
                if (childPaths != null) {
                    for (String childPath : childPaths) {
                        traverseResourceTree(childPath, resourcePathList);
                    }
                }
            } catch (RemoteException | ResourceAdminServiceExceptionException e) {
                //catch exception when the resource path is of a leaf node
                //12 = "swagger.json".length();
                if (path.substring(path.length()-12, path.length()).equals("swagger.json")) {
                    //add to list
                    resourcePathList.add(path);
                    log.info("leaf file path: " + path);
                }
            }
        }
        return resourcePathList;
    }
}
