# WSO2 API Manager 3.2.0 Swagger Validator Tool

## How to Build 
This tool is supported on JDK 11.
Clone the repository and run `mvn clean install` in the root of the repository.

## How to Execute
After building the repository you will get a zip file in the target directory.

Get a latest U2 updated API Manager 3.2.0 pack. Unzip the above zip file in the <APIM_Home> directory.
Open a terminal in the extracted directory (api-manager-3-2-x-swagger-tool-x.y-z) and execute the following command,  

`sh swaggertool.sh`

### You may need to provide the following arguments
1. username - (* Required) Username to access the Admin console
2. password - (* Required) Password to access the Admin console
3. baseurl  - (* Required) Base url of the API Manager instance which is currently being executed
4. download - (Optional) Boolean value [true - if need to download invalid/malformed swaggers and false by default]
5. truststorepath - (Optional) Absolute path to the client truststore. Uses the client-truststore path in the <APIM_HOME>/repository/resources/security directory by default.
6. truststorepassword - (* Required) Password of the client truststore

Example :
`sh swaggertool.sh -d --username admin --password admin --baseurl https://localhost:9443 --truststorepath /Usrs/abc/security/client-truststore.jks --truststorepassword wso2carbon --download true`

Note : The user whose username and password are provided should be a super admin or a tenant admin.
