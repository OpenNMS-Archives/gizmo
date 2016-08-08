/*
 * Copyright 2016, The OpenNMS Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opennms.gizmo.utils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Callable;

/**
 * Utilities for testing network connectivity.
 *
 * @author jwhite
 */
public class NetUtils {

    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 100;

    public static boolean isTcpPortOpen(int port) {
        return isTcpPortOpen(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), DEFAULT_CONNECT_TIMEOUT_MS);
    }

    public static boolean isTcpPortOpen(int port, int connectTimeoutMs) {
        return isTcpPortOpen(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), connectTimeoutMs);
    }

    public static boolean isTcpPortOpen(InetSocketAddress addr) {
        return isTcpPortOpen(addr, DEFAULT_CONNECT_TIMEOUT_MS);
    }

    public static boolean isTcpPortOpen(InetSocketAddress addr, int connectTimeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(addr, connectTimeoutMs);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static Callable<Boolean> isTcpPortOpenCallable(final int port) {
        return isTcpPortOpenCallable(port, DEFAULT_CONNECT_TIMEOUT_MS);
    }

    public static Callable<Boolean> isTcpPortOpenCallable(final int port, final int connectTimeoutMs) {
        return new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return isTcpPortOpen(port, connectTimeoutMs);
            }
        };
    }
}
