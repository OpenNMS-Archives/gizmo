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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.ContainerConfig;

import jersey.repackaged.com.google.common.collect.ImmutableList;

public class GizmoDockerRuleBuilder {
    protected DockerClient docker;
    protected final Map<String, Function<GizmoDockerStacker, ContainerConfig>> containersByAlias = new LinkedHashMap<>();
    protected final List<Consumer<GizmoDockerStacker>> waitingRules = new LinkedList<>();
    protected boolean skipPull = false;
    protected boolean skipTearDown = false;
    protected boolean skipTearDownOnFailure = false;
    protected List<GizmoDockerStack> stacks = new LinkedList<>();

    public GizmoDockerRuleBuilder withDockerClient(DockerClient docker) {
        this.docker = docker;
        return this;
    }

    public GizmoDockerRuleBuilder withContainer(String alias, Function<GizmoDockerStacker, ContainerConfig> container) {
        containersByAlias.put(alias, container);
        return this;
    }

    public GizmoDockerRuleBuilder withWaitingRule(Consumer<GizmoDockerStacker> stacker) {
        waitingRules.add(stacker);
        return this;
    }

    public GizmoDockerRuleBuilder withStack(GizmoDockerStack stack) {
        stacks.add(stack);
        return this;
    }

    public GizmoDockerRuleBuilder skipPull(boolean skipPull) {
        this.skipPull = skipPull;
        return this;
    }

    public GizmoDockerRuleBuilder skipTearDown(boolean skipTearDown) {
        this.skipTearDown = skipTearDown;
        return this;
    }

    public GizmoDockerRule build() {
        if (containersByAlias.size() > 0) {
            final Map<String, Function<GizmoDockerStacker, ContainerConfig>> containers = ImmutableMap.copyOf(containersByAlias);
            final List<Consumer<GizmoDockerStacker>> rules = ImmutableList.copyOf(waitingRules);
            stacks.add(new GizmoDockerStack() {
                @Override
                public List<GizmoDockerStack> getDependencies() {
                    return Collections.emptyList();
                }

                @Override
                public Map<String, Function<GizmoDockerStacker, ContainerConfig>> getContainersByAlias() {
                    return containers;
                }

                @Override
                public List<Consumer<GizmoDockerStacker>> getWaitingRules() {
                    return rules;
                }
            });
        }

        if (stacks.size() < 1) {
            throw new IllegalStateException("One or more stacks and/or containers are required.");
        }
        return new GizmoDockerRule(new GizmoDockerStacker(this), this);
    }
}
