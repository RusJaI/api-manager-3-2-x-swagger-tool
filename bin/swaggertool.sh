#!/bin/sh
# ----------------------------------------------------------------------------

#Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
#
# WSO2 LLC. licenses this file to you under the Apache License,
# Version 2.0 (the "License"); you may not use this file except
# in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.

#!/bin/bash

# Specify the path to the JAR file
jar_path="./api-manager-3-2-x-swagger-tool-1.0-0.jar"

# Specify the classpath (replace with your actual classpath)
current_directory=$(pwd)
custom_classpath="$jar_path"

string1='com.fasterxml.jackson.*'
string='org.wso2.carbon.authenticator.stub*';
declare -a arr=("com.fasterxml.jackson" "org.wso2.carbon.authenticator.stub" "org.wso2.carbon.registry.resource.stub" "jcommander" "commons-logging" "axis2" "org.wso2.carbon.um.ws.api.stub" "axiom_" "wsdl4j.wso2" "XmlSchema" "neethi" "activation" "org.wso2.securevault" "commons-httpclient" "httpclient_" "httpcore_" "commons-codec-" "swagger-parser_" "slf4j-api" "org.yaml.snakeyaml" "swagger-core" "org.wso2.carbon.utils_" "org.wso2.carbon.base_")

for t in "$current_directory"/repository/components/plugins/*.jar
do
    for i in "${arr[@]}"
    do
    if [[ $t == *$i* ]]; then
        custom_classpath="$custom_classpath":$t
    fi
    done
done

for t in "$current_directory"/lib/*.jar
do
    for i in "${arr[@]}"
    do
    if [[ $t == *$i* ]]; then
        custom_classpath="$custom_classpath":$t
    fi
    done
done

echo "\n"
echo "$custom_classpath"
echo "\n"

# Execute the Java command with options -u and -d
java  -Dlog4j2.configurationFile=/Users/rusirijayodaillesinghe/Documents/patch/3.2.0/Test_swagger_tool/wso2am-3.2.0/log4j2.xml -cp "$custom_classpath" org.wso2.apim.swagger.tool.SwaggerTool  "$@"

# java -cp /Users/abc/Documents/APIM_Repos/api-manager-3-2-x-swagger-tool/lib/ -jar apim-swagger-validator-1.0-0.jar


