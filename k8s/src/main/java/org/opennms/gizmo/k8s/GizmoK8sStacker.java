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
package org.opennms.gizmo.k8s;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.opennms.gizmo.GizmoStacker;
import org.opennms.gizmo.k8s.portforward.ForwardedPort;
import org.opennms.gizmo.k8s.portforward.KubeCtlPortForwardingStrategy;
import org.opennms.gizmo.k8s.portforward.PortForwardingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.URLUtils;
import okhttp3.OkHttpClient;

public class GizmoK8sStacker implements GizmoStacker<GizmoK8sStack> {
    private static final Logger LOG = LoggerFactory.getLogger(GizmoK8sStacker.class);

    private KubernetesClient kubernetes;
    private String namespace;
    private boolean deleteNamespace = false;

    private final PortForwardingStrategy portFwdStrategy = new KubeCtlPortForwardingStrategy();
    private final List<ForwardedPort> fwdedPorts = new LinkedList<>();

    public GizmoK8sStacker(GizmoK8sRuleBuilder builder) {
        kubernetes = builder.kubernetes;
        namespace = builder.namespace;
    }

    @Override
    public void init() {
        if (kubernetes == null) {
            // Initialize the default client
            kubernetes = new DefaultKubernetesClient();
        }

        if (namespace == null) {
            // Generate a new namespace with a random UUID
            namespace = "gizmo-" + UUID.randomUUID().toString();

            LOG.info("Creating namespace: {}", namespace);
            kubernetes.namespaces().create(new NamespaceBuilder().withNewMetadata()
                    .withName(namespace)
                    .addToLabels("scope", "test").endMetadata().build());

            // Only delete the namespace if we created it.
            deleteNamespace = true;
        }
    }

    @Override
    public void stack(GizmoK8sStack stack) {
        for (GizmoK8sStack dependency : stack.getDependencies()) {
            LOG.info("Stacking dependency: {}", dependency);
            stack(dependency);
        }

        for (Secret secret : stack.getSecrets(this)) {
            LOG.info("Creating secret: {}", secret);
            kubernetes.secrets().inNamespace(namespace).create(secret);
        }

        for (Service svc : stack.getServices(this)) {
            LOG.info("Creating service: {}", svc);
            kubernetes.services().inNamespace(namespace).create(svc);
        }

        for (ReplicationController rc : stack.getReplicationControllers(this)) {
            LOG.info("Creating replication controller: {}", rc);
            kubernetes.replicationControllers().inNamespace(namespace).create(rc);
        }

        for (Pod pod : stack.getPods(this)) {
            LOG.info("Creating pod: {}", pod);
            kubernetes.pods().inNamespace(namespace).create(pod);
        }

        // TODO: Verify service and pod status'es before firing off the waiting rules
        // i.e. avoid waiting for L7 check if application image cannot be found

        for (Consumer<GizmoK8sStacker> waitingRule : stack.getWaitingRules()) {
            try {
                waitingRule.accept(this);
            } catch (Throwable t) {
                LOG.error("waitFor() rule failed. Tearing down.", t);
                throw Throwables.propagate(t);
            }
        }
    }

    @Override
    public void tearDown() {
        if (kubernetes == null) {
            LOG.warn("Kubernetes client instance is null. Skipping tear down.");
            return;
        }

        if (namespace != null && deleteNamespace) {
            LOG.info("Deleting namespace: {}", namespace);
            kubernetes.namespaces().withName(namespace).delete();
        }

        for (ForwardedPort fwdedPort : fwdedPorts) {
            try {
                LOG.info("Closing forwarded port: {}", fwdedPort);
                fwdedPort.close();
            } catch (IOException e) {
                LOG.warn("Failed to close forwarded port: {}", fwdedPort, e);
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (kubernetes == null) {
            LOG.warn("Kubernetes client instance is null. Skipping tear down.");
            return;
        }
        kubernetes.close();
    }

    public KubernetesClient getClient() {
        return kubernetes;
    }

    public String getNamespace() {
        return namespace;
    }

    public List<Pod> getPodsWithLabel(String key, String value) {
        return kubernetes.pods().inNamespace(namespace).list().getItems().stream()
                .filter(pod -> value.equals(pod.getMetadata().getLabels().get(key)))
                .collect(Collectors.toList());
    }

    public InetSocketAddress portForward(String pod, int remotePort) {
        ForwardedPort fwdedPort = portFwdStrategy.portForward(namespace, pod, remotePort);
        fwdedPorts.add(fwdedPort);
        return fwdedPort.getAddress();
    }

    public OkHttpClient getHttpClient() {
        if (kubernetes instanceof DefaultKubernetesClient) {
            return ((DefaultKubernetesClient)kubernetes).getHttpClient();
        } else {
            return null;
        }
    }

    public URL getProxyUrl(String service, String... parts) {
        try {
            URL baseUrl = new URL(URLUtils.join(kubernetes.getMasterUrl().toString(),
                        "api", "v1", "proxy", "namespaces", namespace, "services", service));
            return new URL(URLUtils.join(Lists.asList(baseUrl.toString(), parts).toArray(new String[0])));
        } catch (MalformedURLException e) {
            throw Throwables.propagate(e);
        }
    }
}
