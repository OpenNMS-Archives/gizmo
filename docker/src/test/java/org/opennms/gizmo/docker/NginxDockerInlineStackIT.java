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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.junit.Rule;
import org.junit.Test;
import org.opennms.gizmo.utils.HttpUtils;

import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.HostConfig;

/**
 * In this example, we define a single container container in-line with the
 * Docker rule and wait until the server successfully to an HTTP request
 * before passing control to the test.
 *
 * @author jwhite
 */
public class NginxDockerInlineStackIT {

    /**
     * Within Gizmo, containers are referred to using an alias, which
     * can be different from the image name and container id.
     *
     * Every container used in a rule should be a unique alias.
     */
    private static final String NGINX_ALIAS = "NGINX";

    @Rule
    public GizmoDockerRule gizmo = GizmoDockerRule.builder()
        .withContainer(NGINX_ALIAS, (stacker) -> ContainerConfig.builder()
                .image("nginx:1.11.1-alpine")
                .hostConfig(HostConfig.builder()
                        .publishAllPorts(true)
                        .autoRemove(true)
                        .build())
                .build())
        .withWaitingRule((stacker) -> {
            // When the container ports are bound to random host ports
            // you can use this call to determine the effective address of the service
            final InetSocketAddress httpAddr = stacker.getServiceAddress(NGINX_ALIAS, 80);
            await().atMost(2, MINUTES)
                    .pollInterval(5, SECONDS).pollDelay(0, SECONDS)
                    .until(() -> HttpUtils.get(httpAddr, "/") != null);
        }).build();

    @Test
    public void canSpawnContainerAndDoGet() throws IOException {
        // At this point, our container should is up and running and ready to answer requests
        final GizmoDockerStacker stacker = gizmo.getStacker();
        final InetSocketAddress httpAddr = stacker.getServiceAddress(NGINX_ALIAS, 80);
        assertThat(HttpUtils.get(httpAddr, "/"), containsString("Welcome to nginx!"));
    }
}
