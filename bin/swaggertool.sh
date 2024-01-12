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
jar_path="../target/apim-swagger-validator-1.0-0.jar"

# Specify the classpath (replace with your actual classpath)
current_directory=$(pwd)
custom_classpath="$current_directory/lib/"

# Execute the Java command with options -u and -d
java -cp "$custom_classpath" -jar "$jar_path" "$@"

# java -cp /Users/abc/Documents/APIM_Repos/api-manager-3-2-x-swagger-tool/lib/ -jar apim-swagger-validator-1.0-0.jar


