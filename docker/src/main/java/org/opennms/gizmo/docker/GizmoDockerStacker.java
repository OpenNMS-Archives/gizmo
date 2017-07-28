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

import com.google.common.base.Throwables;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.PortBinding;
import org.opennms.gizmo.GizmoStacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class GizmoDockerStacker implements GizmoStacker<GizmoDockerStack> {
    private static final Logger LOG = LoggerFactory.getLogger(GizmoDockerStacker.class);

    private DockerClient docker;
    private final boolean skipPull;
    private final boolean useExistingStacks;

    /**
     * Keeps track of the IDs for all the created containers so we can
     * (possibly) tear them down later.
     */
    private final Set<String> createdContainerIds = new LinkedHashSet<>();

    /**
     * Keeps track of container meta-data.
     */
    private final Map<String, ContainerInfo> containerInfoByAlias = new HashMap<>();

    public GizmoDockerStacker(GizmoDockerRuleBuilder builder) {
        docker = builder.docker;
        skipPull = builder.skipPull;
        useExistingStacks = builder.useExistingStacks;
    }

    @Override
    public void init() throws DockerCertificateException {
        if (docker == null) {
            // Initialize the default client
            docker = DefaultDockerClient.fromEnv().build();
        }
    }

    @Override
    public void stack(GizmoDockerStack stack) throws DockerException, InterruptedException {
        for (GizmoDockerStack dependency : stack.getDependencies()) {
            LOG.info("Stacking dependency: {}", dependency);
            stack(dependency);
        }

        stack.beforeStack(this);

        for (Entry<String, Function<GizmoDockerStacker, ContainerConfig>> entry : stack.getContainersByAlias().entrySet()) {
            final String alias = entry.getKey();
            final Function<GizmoDockerStacker, ContainerConfig> containerFunc = entry.getValue();

            ContainerConfig container = containerFunc.apply(this);

            if (!skipPull) {
                try {
                    docker.inspectImage(container.image());
                } catch (ImageNotFoundException infe) {
                    LOG.info("Pulling image for alias {}: {}", alias, container.image());
                    docker.pull(container.image());
                    LOG.info("Done pulling image.");
                }
            }

            final String containerId;
            if (!useExistingStacks) {
                final ContainerCreation containerCreation = docker.createContainer(container);
                containerId = containerCreation.id();
                createdContainerIds.add(containerId);

                docker.startContainer(containerId);
            } else {
                containerId = docker.listContainers(DockerClient.ListContainersParam.withStatusRunning()).stream()
                        .filter(c -> Objects.equals(container.image(), c.image()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Could not find runnign container with image: "
                                + container.image()))
                        .id();
            }

            final ContainerInfo containerInfo = docker.inspectContainer(containerId);
            LOG.info("{} has container id: {}", alias, containerId);
            if (!containerInfo.state().running()) {
                throw new IllegalStateException("Could not start the " + alias + " container");
            }

            containerInfoByAlias.put(alias, containerInfo);
        }

        // TODO: Verify container's status'es before firing off the waiting rules
        // i.e. avoid waiting for L7 check if application image cannot be found
        for (Consumer<GizmoDockerStacker> waitingRule : stack.getWaitingRules()) {
            try {
                waitingRule.accept(this);
            } catch (Throwable t) {
                LOG.error("waitFor() rule failed. Tearing down.", t);
                throw Throwables.propagate(t);
            }
        }

        stack.afterStack(this);
    }

    @Override
    public void tearDown() {
        // Kill and remove all of the containers we created
        for (String containerId : createdContainerIds) {
            try {
                LOG.info("Killing container with id: {}", containerId);
                docker.killContainer(containerId);
                try {
                    LOG.info("Removing container with id: {}", containerId);
                    docker.removeContainer(containerId);
                } catch (Throwable t) {
                    // If autoremove is set, removeContainer will throw an NPE
                    LOG.info("Failed to remove container with id: {}. The container is likely already be removed.", containerId);
                }
            } catch (DockerException | InterruptedException e) {
                LOG.error("Failed to kill and/or remove container with id: {}", containerId, e);
            }
        }
    }

    public Set<String> getAliases() {
        return containerInfoByAlias.keySet();
    }

    public ContainerInfo getContainerInfo(final String alias) {
        return containerInfoByAlias.get(alias);
    }

    public InetSocketAddress getServiceAddress(String alias, int port) {
        return getServiceAddress(alias, port, "tcp");
    }

    public InetSocketAddress getServiceAddress(String alias, int port, String type) {
        final ContainerInfo info = getContainerInfo(alias);
        if (info == null) {
            throw new IllegalArgumentException(String.format("No container found with alias: %s. Available containers include: ",
                    alias, containerInfoByAlias.keySet()));
        }
        final String portKey = port + "/" + type;
        final List<PortBinding> bindings = info.networkSettings().ports().get(portKey);
        if (bindings == null) {
            throw new IllegalArgumentException(String.format("No bindings found for port %s on alias: %s. Available ports include: %s",
                    portKey, alias, info.networkSettings().ports().keySet()));
        }
        final PortBinding binding = bindings.iterator().next();
        final String host = "0.0.0.0".equals(binding.hostIp()) ? docker.getHost() : binding.hostIp();
        return new InetSocketAddress(host, Integer.valueOf(binding.hostPort()));
    }

    @Override
    public void close() throws IOException {
        if (docker == null) {
            LOG.warn("Docker client instance is null. Skipping tear down.");
            return;
        }

        docker.close();
    }

    protected DockerClient getDocker() {
        return docker;
    }
}
