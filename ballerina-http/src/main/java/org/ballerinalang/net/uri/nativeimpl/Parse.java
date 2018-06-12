/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.net.uri.nativeimpl;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.net.http.HttpUtil;

import java.net.URI;
import java.net.URISyntaxException;

import static org.ballerinalang.mime.util.Constants.FIRST_PARAMETER_INDEX;

/**
 * Given a url as a string, construct ballerina URI object with host, port and scheme.
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "http",
        functionName = "parse",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = "URI", structPackage = "ballerina.http"),
        args = {@Argument(name = "uri", type = TypeKind.STRING)},
        returnType = {@ReturnType(type = TypeKind.STRING), @ReturnType(type = TypeKind.RECORD, structType = "Error")},
        isPublic = true
)
public class Parse extends BlockingNativeCallableUnit {
    @Override
    public void execute(Context context) {
        String url = context.getStringArgument(FIRST_PARAMETER_INDEX);
        BStruct uriObject = (BStruct) context.getRefArgument(FIRST_PARAMETER_INDEX);
        try {
            URI uri = new URI(url);
            uriObject.setStringField(0, uri.getScheme());
            uriObject.setStringField(1, uri.getHost());
            uriObject.setIntField(0, uri.getPort());
            context.setReturnValues();
        } catch (URISyntaxException e) {
            context.setReturnValues(HttpUtil.getError(context, "Error occurred while parsing uri. " + e
                    .getMessage()));
        }
    }
}
