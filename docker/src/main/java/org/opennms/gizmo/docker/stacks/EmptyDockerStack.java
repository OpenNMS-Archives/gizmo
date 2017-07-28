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
package org.opennms.gizmo.docker.stacks;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.opennms.gizmo.docker.GizmoDockerStack;
import org.opennms.gizmo.docker.GizmoDockerStacker;

import com.spotify.docker.client.messages.ContainerConfig;

public class EmptyDockerStack implements GizmoDockerStack {

    @Override
    public List<GizmoDockerStack> getDependencies() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, Function<GizmoDockerStacker, ContainerConfig>> getContainersByAlias() {
        return Collections.emptyMap();
    }

    @Override
    public List<Consumer<GizmoDockerStacker>> getWaitingRules() {
        return Collections.emptyList();
    }

    @Override
    public void beforeStack(GizmoDockerStacker stacker) {
        // pass
    }

    @Override
    public void afterStack(GizmoDockerStacker stacker) {
        // pass
    }

}
