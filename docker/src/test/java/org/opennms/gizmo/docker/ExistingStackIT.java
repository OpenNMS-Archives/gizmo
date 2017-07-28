/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.gizmo.docker;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.ContainerInfo;
import org.junit.Test;
import org.opennms.gizmo.utils.HttpUtils;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;

public class ExistingStackIT {

    @Test
    public void canReferenceExistingStacks() throws Exception {
        // Stack it
        GizmoDockerRule initialGizmoRule = GizmoDockerRule.builder()
                .withStack(new NginxStack())
                .skipTearDown(true)
                .build();
        initialGizmoRule.before();

        // Verify
        final InetSocketAddress httpAddr = initialGizmoRule.getStacker().getServiceAddress(NginxStack.NGINX, 80);
        verifyNginxContainer(httpAddr);

        // Save this for later
        ContainerInfo containerInfo = initialGizmoRule.getStacker()
                .getContainerInfo(NginxStack.NGINX);

        // Complete the rule
        initialGizmoRule.after(false);

        // The container should still be up since we skipped the tear down
        verifyNginxContainer(httpAddr);

        // Now create a new rule that will reference the existing stack(s)
        GizmoDockerRule anotherGizmoRule = GizmoDockerRule.builder()
                .withStack(new NginxStack())
                .useExistingStacks(true)
                .build();
        anotherGizmoRule.before();

        // The HTTP endpoints should be the same
        final InetSocketAddress anotherHttpAddr = initialGizmoRule.getStacker()
                .getServiceAddress(NginxStack.NGINX, 80);
        assertThat(anotherHttpAddr, equalTo(httpAddr));

        // The container should still be reachable
        verifyNginxContainer(anotherHttpAddr);

        // Complete the rule
        anotherGizmoRule.after(false);

        // We don't remove containers when referencing an existing stack, so
        // The container should still be reachable
        verifyNginxContainer(anotherHttpAddr);

        // Kill the container
        try (DockerClient docker = DefaultDockerClient.fromEnv().build()) {
            docker.killContainer(containerInfo.id());
        }
    }

    private void verifyNginxContainer(InetSocketAddress httpAddr) throws IOException {
        assertThat(HttpUtils.get(httpAddr, "/"), containsString("Welcome to nginx!"));
    }
}
