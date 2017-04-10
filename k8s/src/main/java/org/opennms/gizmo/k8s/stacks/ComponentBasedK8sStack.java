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
package org.opennms.gizmo.k8s.stacks;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.opennms.gizmo.k8s.GizmoK8sStack;
import org.opennms.gizmo.k8s.GizmoK8sStacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;

public class ComponentBasedK8sStack extends EmptyK8sStack {
    private static final Logger LOG = LoggerFactory.getLogger(YamlBasedK8sStack.class);

    @Override
    public void create(GizmoK8sStacker stacker, KubernetesClient kubernetes) {
        for (Secret secret : getSecrets(stacker)) {
            LOG.info("Creating secret: {}", secret);
            kubernetes.secrets().inNamespace(stacker.getNamespace()).create(secret);
        }

        for (Service svc : getServices(stacker)) {
            LOG.info("Creating service: {}", svc);
            kubernetes.services().inNamespace(stacker.getNamespace()).create(svc);
        }

        for (ReplicationController rc : getReplicationControllers(stacker)) {
            LOG.info("Creating replication controller: {}", rc);
            kubernetes.replicationControllers().inNamespace(stacker.getNamespace()).create(rc);
        }

        for (Pod pod : getPods(stacker)) {
            LOG.info("Creating pod: {}", pod);
            kubernetes.pods().inNamespace(stacker.getNamespace()).create(pod);
        }

        // TODO: Verify service and pod status'es before firing off the waiting rules
        // i.e. avoid waiting for L7 check if application image cannot be found
    }

    public List<Secret> getSecrets(GizmoK8sStacker stacker) {
        return Collections.emptyList();
    }

    public List<Service> getServices(GizmoK8sStacker stacker) {
        return Collections.emptyList();
    }

    public List<ReplicationController> getReplicationControllers(GizmoK8sStacker stacker) {
        return Collections.emptyList();
    }

    public List<Pod> getPods(GizmoK8sStacker stacker) {
        return Collections.emptyList();
    }

    @Override
    public List<GizmoK8sStack> getDependencies() {
        return Collections.emptyList();
    }

    @Override
    public List<Consumer<GizmoK8sStacker>> getWaitingRules() {
        return Collections.emptyList();
    }
}
