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
package org.opennms.gizmo.k8s.portforward;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import com.google.common.base.Throwables;

public class KubeCtlPortForwardingStrategy implements PortForwardingStrategy {

    protected static Integer getLocalPortFromOutput(String out) {
        Pattern p = Pattern.compile(" (.*):(\\d+) -> (\\d+)", Pattern.MULTILINE);
        Matcher m = p.matcher(out);
        if (m.find()) {
            return Integer.valueOf(m.group(2));
        }
        return null;
    }

    protected static int waitForLocalPort(ExecuteWatchdog wd, ByteArrayOutputStream out, ByteArrayOutputStream err) {
        while (wd.isWatching()) {
            Integer port = getLocalPortFromOutput(out.toString());
            if (port != null) {
                return port.intValue();
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException(String.format("Process execution failed. Stdout: %s Stderr: %s", out, err));
    }

    @Override
    public ForwardedPort portForward(String namespace, String pod, int remotePort) {
        CommandLine cmdLine = new CommandLine("kubectl");
        cmdLine.addArgument("--namespace=${namespace}");
        cmdLine.addArgument("port-forward");
        cmdLine.addArgument("${pod}");
        cmdLine.addArgument(":${remotePort}");

        HashMap<String, String> map = new HashMap<>();
        map.put("namespace", namespace);
        map.put("pod", pod);
        map.put("remotePort", Integer.toString(remotePort));
        cmdLine.setSubstitutionMap(map);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PumpStreamHandler psh = new PumpStreamHandler(out, err);

        DefaultExecutor executor = new DefaultExecutor();
        final ExecuteWatchdog wd = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
        executor.setWatchdog(wd);
        executor.setStreamHandler(psh);
        
        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        try {
            executor.execute(cmdLine, resultHandler);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        final int localPort = waitForLocalPort(wd, out, err);
        return new ForwardedPort() {
            @Override
            public InetSocketAddress getAddress() {
                return new InetSocketAddress(InetAddress.getLoopbackAddress(), localPort);
            }

            @Override
            public void close() throws IOException {
                wd.destroyProcess();
            }

            @Override
            public String toString() {
                return String.format("ForwardedPort[localPort=%d]", localPort);
            }
        };
    }

}
