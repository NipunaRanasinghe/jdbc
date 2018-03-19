/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.nativeimpl.runtime;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.model.types.BTypes;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;

/**
 * Native function to get userId from authentication context.
 *
 * @since 0.965.0
 */
@BallerinaFunction(
        packageName = "ballerina.runtime",
        functionName = "getUsername",
        receiver = @Receiver(type = TypeKind.STRUCT, structType = "AuthenticationContext", structPackage = "ballerina" +
                ".io"),
        returnType = {@ReturnType(type = TypeKind.STRING)},
        isPublic = true
)
public class GetUsername extends BlockingNativeCallableUnit {

    @Override
    public void execute(Context context) {
        AuthenticationContext authContext = AuthenticationContextUtils.getAuthenticationContext(context);
        String username = authContext.getUsername();
        if (username == null) {
            username = BTypes.typeString.getZeroValue().stringValue();
        }
        context.setReturnValues(new BString(username));
    }
}
