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

import com.beust.jcommander.Parameter;


/**
 * This class contains the parameter definitions for Jcommander Arguments.
 */
public class JcommanderArgs {

    @Parameter(names = {"--baseurl", "-b"}, description = "Base URL of the currently executing API Manager Instance")
    private String baseurl = "https://localhost:9443";
    @Parameter(names = {"--username", "-u"}, description = "Username to log into carbon console")
    private String username;

    @Parameter(names = {"--password", "-p"}, description = "Password to log into carbon console", password = true)
    private String password;

    @Parameter(names = {"--download", "-d"}, description = "Download Invalid and Malformed Swaggers")
    private boolean doDownload = false;

    @Parameter(names = {"--truststorepath", "-trustpath"}, description = "Absolute path to trust store")
    private String truststorepath;

    @Parameter(names = {"--truststorepassword", "trustpass"}, description = "Trust store password", password = true)
    private String truststorepassword;

    public JcommanderArgs() {
    }


    protected String getBaseurl() {
        return baseurl;
    }

    protected String getUsername() {
        return username;
    }

    protected String getPassword() {
        return password;
    }

    protected Boolean getDoDownload() {
        return doDownload;
    }

    protected String getTruststorepath() {
        return truststorepath;
    }

    protected String getTruststorepassword() {
        return truststorepassword;
    }

}
