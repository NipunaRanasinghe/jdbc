/*
*  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/

package org.ballerinalang.mime.util;

import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BStringArray;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.jvnet.mimepull.Header;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.ballerinalang.mime.util.Constants.ASSIGNMENT;
import static org.ballerinalang.mime.util.Constants.CONTENT_DISPOSITION;
import static org.ballerinalang.mime.util.Constants.CONTENT_ID;
import static org.ballerinalang.mime.util.Constants.CONTENT_ID_INDEX;
import static org.ballerinalang.mime.util.Constants.CONTENT_TYPE;
import static org.ballerinalang.mime.util.Constants.ENTITY_HEADERS_INDEX;
import static org.ballerinalang.mime.util.Constants.FIRST_ELEMENT;
import static org.ballerinalang.mime.util.Constants.MULTIPART_AS_PRIMARY_TYPE;
import static org.ballerinalang.mime.util.Constants.SEMICOLON;

/**
 * Utility methods for parsing headers.
 *
 * @since 0.963.0
 */
public class HeaderUtil {

    /**
     * Given a header value, get it's parameters.
     *
     * @param headerValue Header value as a string
     * @return Parameter map
     */
    public static BMap<String, BValue> getParamMap(String headerValue) {
        BMap<String, BValue> paramMap = null;
        if (headerValue.contains(SEMICOLON)) {
            extractValue(headerValue);
            List<String> paramList = Arrays.stream(headerValue.substring(headerValue.indexOf(SEMICOLON) + 1)
                    .split(SEMICOLON)).map(String::trim).collect(Collectors.toList());
            paramMap = validateParams(paramList) ? getHeaderParamBMap(paramList) : null;
        }
        return paramMap;
    }

    /**
     * Get header value without parameters.
     *
     * @param headerValue Header value with parameters as a string
     * @return Header value without parameters
     */
    public static String getHeaderValue(String headerValue) {
        return extractValue(headerValue.trim());
    }

    /**
     * Extract header value.
     *
     * @param headerValue Header value with parameters as a string
     * @return Header value without parameters
     */
    private static String extractValue(String headerValue) {
        String value = headerValue.substring(0, headerValue.indexOf(SEMICOLON)).trim();
        if (value.isEmpty()) {
            throw new BallerinaException("invalid header value: " + headerValue);
        }
        return value;
    }

    private static boolean validateParams(List<String> paramList) {
        //validate header values which ends with semicolon without params
        return !(paramList.size() == 1 && paramList.get(0).isEmpty());
    }

    /**
     * Given a list of string parameter list, create ballerina specific header parameter map.
     *
     * @param paramList List of parameters
     * @return Ballerina map
     */
    private static BMap<String, BValue> getHeaderParamBMap(List<String> paramList) {
        BMap<String, BValue> paramMap = new BMap<>();
        for (String param : paramList) {
            if (param.contains("=")) {
                String[] keyValuePair = param.split("=");
                if (keyValuePair.length != 2 || keyValuePair[0].isEmpty()) {
                    throw new BallerinaException("invalid header parameter: " + param);
                }
                paramMap.put(keyValuePair[0].trim(), new BString(keyValuePair[1].trim()));
            } else {
                //handle when parameter value is optional
                paramMap.put(param.trim(), null);
            }
        }
        return paramMap;
    }

    static boolean isHeaderExist(List<String> headers) {
        return headers != null && headers.get(FIRST_ELEMENT) != null && !headers.get(FIRST_ELEMENT).isEmpty();
    }

    /**
     * Set body part headers.
     *
     * @param bodyPartHeaders Represent decoded mime part headers
     * @param headerMap       Represent ballerina header map
     * @return a populated ballerina map with body part headers
     */
    static BMap<String, BValue> setBodyPartHeaders(List<? extends Header> bodyPartHeaders,
                                                   BMap<String, BValue> headerMap) {
        for (final Header header : bodyPartHeaders) {
            if (headerMap.keySet().contains(header.getName())) {
                BStringArray valueArray = (BStringArray) headerMap.get(header.getName());
                valueArray.add(valueArray.size(), header.getValue());
            } else {
                BStringArray valueArray = new BStringArray(new String[]{header.getValue()});
                headerMap.put(header.getName(), valueArray);
            }
        }
        return headerMap;
    }

    /**
     * Extract the header value from a body part for a given header name.
     *
     * @param bodyPart   Represent a ballerina body part
     * @param headerName Represent an http header name
     * @return a header value for the given header name
     */
    public static String getHeaderValue(BStruct bodyPart, String headerName) {
        BMap<String, BValue> headerMap = (bodyPart.getRefField(ENTITY_HEADERS_INDEX) != null) ?
                (BMap<String, BValue>) bodyPart.getRefField(ENTITY_HEADERS_INDEX) : null;
        if (headerMap != null) {
            BStringArray headerValue = (BStringArray) headerMap.get(headerName);
            return headerValue.get(0);
        }
        return null;
    }

    /**
     * Get the header value intact with parameters.
     *
     * @param headerValue Header value as a string
     * @param map         Represent a parameter map
     * @return Header value along with it's parameters as a string
     */
    static String appendHeaderParams(StringBuilder headerValue, BMap map) {
        int index = 0;
        if (map != null && !map.isEmpty()) {
            Set<String> keys = map.keySet();
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    BString paramValue = (BString) map.get(key);
                    if (index == keys.size() - 1) {
                        headerValue.append(key).append(ASSIGNMENT).append(paramValue.toString());
                    } else {
                        headerValue.append(key).append(ASSIGNMENT).append(paramValue.toString()).append(SEMICOLON);
                        index = index + 1;
                    }
                }
            }
        }
        return headerValue.toString();
    }

    /**
     * Add a given header name and a value to entity headers.
     *
     * @param entityHeaders A map of entity headers
     * @param headerName    Header name as a string
     * @param headerValue   Header value as a string
     */
    private static void addToEntityHeaders(BMap<String, BValue> entityHeaders, String headerName, String headerValue) {
        if (entityHeaders.keySet().contains(headerName)) {
            BStringArray valueArray = (BStringArray) entityHeaders.get(headerName);
            valueArray.add(valueArray.size(), headerValue);
        } else {
            BStringArray valueArray = new BStringArray(new String[]{headerValue});
            entityHeaders.put(headerName, valueArray);
        }
    }

    /**
     * Given an entity, get its header map. If it's null then return an empty map.
     *
     * @param entityStruct Represent a ballerina entity
     * @return A map of headers
     */
    static BMap<String, BValue> getEntityHeaderMap(BStruct entityStruct) {
        return entityStruct.getRefField(ENTITY_HEADERS_INDEX) != null ?
                (BMap) entityStruct.getRefField(ENTITY_HEADERS_INDEX) : new BMap<>();
    }

    /**
     * Add MediaType struct info as an entity header.
     *
     * @param bodyPart      Represent a ballerina body part
     * @param entityHeaders Map of entity headers
     */
    static void setContentTypeHeader(BStruct bodyPart, BMap<String, BValue> entityHeaders) {
        String contentType = MimeUtil.getContentTypeWithParameters(bodyPart);
        addToEntityHeaders(entityHeaders, CONTENT_TYPE, contentType);
    }

    /**
     * Add ContentDisposition struct info as an entity header.
     *
     * @param bodyPart      Represent a ballerina body part
     * @param entityHeaders Map of entity headers
     */
    static void setContentDispositionHeader(BStruct bodyPart, BMap<String, BValue> entityHeaders) {
        String contentDisposition = MimeUtil.getContentDisposition(bodyPart);
        if (MimeUtil.isNotNullAndEmpty(contentDisposition)) {
            addToEntityHeaders(entityHeaders, CONTENT_DISPOSITION, contentDisposition);
        }
    }

    /**
     * Add content id as an entity header.
     *
     * @param bodyPart      Represent a ballerina body part
     * @param entityHeaders Map of entity headers
     */
    static void setContentIdHeader(BStruct bodyPart, BMap<String, BValue> entityHeaders) {
        String contentId = bodyPart.getStringField(CONTENT_ID_INDEX);
        if (MimeUtil.isNotNullAndEmpty(contentId)) {
            addToEntityHeaders(entityHeaders, CONTENT_ID, contentId);
        }
    }

    public static boolean isMultipart(String contentType) {
        return contentType != null && contentType.startsWith(MULTIPART_AS_PRIMARY_TYPE);
    }
}
