package org.wso2.apim.swagger.tool;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceStub;

import java.io.FileWriter;
import java.io.IOException;

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

        fileWriter.append(String.format("%-20s %-20s %-20s %-20s%n", "Provider", "API Name", "Version", "Results"));

        String [] providers = resourceAdminServiceStub.getCollectionContent(resourcePath).getChildPaths();
        for (String provider : providers) {
            String [] apiNames =  resourceAdminServiceStub.getCollectionContent(provider)
                    .getChildPaths();

            for (String apiName : apiNames) {
                String [] versions = resourceAdminServiceStub.getCollectionContent(apiName).getChildPaths();

                for (String version : versions) {
                    String swaggerJson = resourceAdminServiceStub.getTextContent(version +
                            "/swagger.json").toString();

                    fileWriter.append(String.format("%-20s %-20s %-20s %-20s%n",
                            provider.substring(resourcePath.length() + 1),
                            apiName.substring(provider.length() + 1),
                            version.substring(apiName.length() + 1), ""));

                    SwaggerValidateUtils.validateSwaggerContent(swaggerJson, fileWriter);
                }
            }
        }
        SwaggerValidateUtils.writeStatsSummary(fileWriter);
    }
}
