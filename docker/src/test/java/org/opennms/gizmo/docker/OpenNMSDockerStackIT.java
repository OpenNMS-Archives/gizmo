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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.junit.Rule;
import org.junit.Test;
import org.opennms.gizmo.utils.HttpUtils;

/**
 * In this example, we reference a stack defined in a another class.
 *
 * @author jwhite
 */
public class OpenNMSDockerStackIT {

    @Rule
    public GizmoDockerRule gizmo = GizmoDockerRule.builder()
        .withStack(new OpenNMSStack())
        .build();

    @Test
    public void canSpawnContainerAndDoGet() throws IOException {
        // At this point, our containers are up and running
        final GizmoDockerStacker stacker = gizmo.getStacker();
        final InetSocketAddress nmsHttpAddr = stacker.getServiceAddress(OpenNMSStack.OPENNMS, 8980);
        assertThat(HttpUtils.get(nmsHttpAddr, "admin", "admin", "/opennms/rest/info"),
                containsString("OpenNMS"));
    }
}
