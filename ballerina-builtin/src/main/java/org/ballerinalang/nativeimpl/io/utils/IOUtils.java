/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.nativeimpl.io.utils;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BLangVMStructs;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.nativeimpl.io.Read;
import org.ballerinalang.nativeimpl.io.Write;
import org.ballerinalang.nativeimpl.io.channels.base.Channel;
import org.ballerinalang.nativeimpl.io.events.EventContext;
import org.ballerinalang.nativeimpl.io.events.EventManager;
import org.ballerinalang.nativeimpl.io.events.EventResult;
import org.ballerinalang.nativeimpl.io.events.bytes.ReadBytesEvent;
import org.ballerinalang.nativeimpl.io.events.bytes.WriteBytesEvent;
import org.ballerinalang.util.codegen.PackageInfo;
import org.ballerinalang.util.codegen.StructInfo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.ballerinalang.nativeimpl.io.IOConstants.IO_ERROR_STRUCT;
import static org.ballerinalang.nativeimpl.io.IOConstants.IO_PACKAGE;

/**
 * Represents the util functions of IO operations.
 */
public class IOUtils {

    /**
     * Returns the error struct for the corresponding message.
     *
     * @param context context of the native function.
     * @param message error message.
     * @return error message struct.
     */
    public static BStruct createError(Context context, String message) {
        PackageInfo ioPkg = context.getProgramFile().getPackageInfo(IO_PACKAGE);
        StructInfo error = ioPkg.getStructInfo(IO_ERROR_STRUCT);
        return BLangVMStructs.createBStruct(error, message);
    }

    /**
     * Asynchronously writes bytes to a channel.
     *
     * @param content content which should be written.
     * @param channel the channel the bytes should be written.
     * @return the number of bytes written to the channel.
     * @throws ExecutionException   errors which occur during execution.
     * @throws InterruptedException during interrupt error.
     */
    public static int writeFull(Channel channel, byte[] content, int size, EventContext context)
            throws ExecutionException, InterruptedException {
        int offset = 0;
        do {
            offset = offset + write(channel, content, offset, size, context);
        } while (offset < content.length);
        return offset;
    }

    private static int write(Channel channel, byte[] content, int offset, int numberOfBytes, EventContext context)
            throws InterruptedException, ExecutionException {
        WriteBytesEvent writeBytesEvent = new WriteBytesEvent(channel, content, offset, numberOfBytes, context);
        CompletableFuture<EventResult> future = EventManager.getInstance().publish(writeBytesEvent);
        future.thenApply(Write::writeResponse);
        EventResult eventResponse = future.get();
        offset = offset + (Integer) eventResponse.getResponse();
        return offset;
    }

    /**
     * Asynchronously reads bytes from the channel.
     *
     * @param content the initialized array which should be filled with the content.
     * @param channel the channel the content should be read into.
     * @param offset  if the array size should be read from an offset
     * @throws InterruptedException during interrupt error.
     * @throws ExecutionException   errors which occurs while execution.
     */
    public static int readFull(Channel channel, byte[] content, int offset, EventContext context)
            throws InterruptedException, ExecutionException {
        int numberOfBytesToRead = content.length - offset;
        do {
            offset = offset + read(channel, content, offset, context);
        } while (offset < numberOfBytesToRead && !channel.hasReachedEnd());
        return offset;
    }

    private static int read(Channel channel, byte[] content, int offset, EventContext context)
            throws InterruptedException, ExecutionException {
        ReadBytesEvent event = new ReadBytesEvent(channel, content, offset, context);
        CompletableFuture<EventResult> future = EventManager.getInstance().publish(event);
        //We call the trigger function here
        future.thenApply(Read::readResponse);
        EventResult eventResponse = future.get();
        offset = (Integer) eventResponse.getResponse();
        return offset;
    }

}
