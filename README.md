# WSO2 API Manager 3.2.0 Swagger Validator Tool

## How to Build 
Clone the repository and run `mvn clean install` in the root of the repository.

## How to Execute
After building the repository you will get a zip file in the target directory.

Get a latest U2 updated API Manager 3.2.0 pack. Unzip the above zip file in the <APIM_Home> directory.
Open a terminal in the extracted directory (SwaggerTool) and execute the following command,  

`sh swaggertool.sh`

### You may need to provide the following arguments 
1. username
2. password
3. baseurl
4. download invalid swaggers
5. truststore path
6. truststore password