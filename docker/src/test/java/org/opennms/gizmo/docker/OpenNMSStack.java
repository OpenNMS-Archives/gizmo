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
package org.opennms.gizmo.docker;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.opennms.gizmo.docker.stacks.EmptyDockerStack;
import org.opennms.gizmo.utils.HttpUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.HostConfig;

public class OpenNMSStack extends EmptyDockerStack {

    public static String OPENNMS = "OPENNMS";
    public static String POSTGRES = "POSTGRES";

    @Override
    public Map<String, Function<GizmoDockerStacker, ContainerConfig>> getContainersByAlias() {
        return ImmutableMap.of(POSTGRES, (stacker) -> {
            return ContainerConfig.builder()
                    .image("postgres:9.5.4")
                    .hostConfig(HostConfig.builder()
                     .publishAllPorts(true)
                     .build())
                    .build();
        }, OPENNMS, (stacker) -> {
            return ContainerConfig.builder()
                    .image("nethinks/opennms:18")
                    .exposedPorts("8980/tcp")
                    .hostConfig(HostConfig.builder()
                     .publishAllPorts(true)
                     // Link the container with the effective container name
                     .links(String.format("%s:dbserver", stacker.getContainerInfo(POSTGRES).name()))
                     .build())
                    .build();
        });
    }

    @Override
    public List<Consumer<GizmoDockerStacker>> getWaitingRules() {
        return ImmutableList.of((stacker) -> {
            final InetSocketAddress httpAddr = stacker.getServiceAddress(OPENNMS, 8980);
            await().atMost(5, MINUTES).pollInterval(10, SECONDS).pollDelay(0, SECONDS)
                .until(() -> {
                    final String response = HttpUtils.get(httpAddr, "admin", "admin", "/opennms/rest/info");
                    return response != null ? response.contains("opennms") : false;
                });
        });
    }
}
