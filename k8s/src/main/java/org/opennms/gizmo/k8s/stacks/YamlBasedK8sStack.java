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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
import org.opennms.gizmo.k8s.GizmoK8sStack;
import org.opennms.gizmo.k8s.GizmoK8sStacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.google.common.base.Throwables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.hubspot.jinjava.Jinjava;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;

public class YamlBasedK8sStack extends ComponentBasedK8sStack {
    private static final Logger LOG = LoggerFactory.getLogger(YamlBasedK8sStack.class);

    private final Multimap<String, String> documentsByKind = LinkedListMultimap.create();
    private final Jinjava jinjava = new Jinjava();
    private boolean useTemplating;

    public YamlBasedK8sStack(URL... yaml) {
        Yaml yamlHandler = new Yaml();
        try {
            for (URL url : yaml) {
                for (Object doc : yamlHandler.loadAll(url.openStream())) {
                    documentsByKind.put(getKind(doc).toLowerCase(), yamlHandler.dump(doc));
                }
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public void useTemplating(boolean useTemplating) {
        this.useTemplating = useTemplating;
    }

    public Map<String, Object> getTemplateContext() {
        return Collections.emptyMap();
    }

    @Override
    public List<GizmoK8sStack> getDependencies() {
        return Collections.emptyList();
    }

    private InputStream maybeApplyTemplating(GizmoK8sStacker stacker, String doc) {
        String renderedDoc;
        if (!useTemplating) {
            renderedDoc = doc;
        } else {
            Map<String, Object> context = new HashMap<>();
            context.put("namespace", stacker.getNamespace());
            context.putAll(getTemplateContext());
            renderedDoc = jinjava.render(doc, context);
            LOG.debug("Rendered document: {}", renderedDoc);
        }
        return new ByteArrayInputStream(renderedDoc.getBytes());
    }

    @Override
    public List<Secret> getSecrets(GizmoK8sStacker stacker) {
        final KubernetesClient client = stacker.getClient();
        return documentsByKind.get("secret").stream()
            .map(doc -> client.secrets().load(maybeApplyTemplating(stacker, doc)).get())
            .collect(Collectors.toList());
    }

    @Override
    public List<ConfigMap> getConfigMaps(GizmoK8sStacker stacker) {
        final KubernetesClient client = stacker.getClient();
        return documentsByKind.get("configmap").stream()
                .map(doc -> client.configMaps().load(maybeApplyTemplating(stacker, doc)).get())
                .collect(Collectors.toList());
    }

    @Override
    public List<Service> getServices(GizmoK8sStacker stacker) {
        final KubernetesClient client = stacker.getClient();
        return documentsByKind.get("service").stream()
                .map(doc -> client.services().load(maybeApplyTemplating(stacker, doc)).get())
                .collect(Collectors.toList());
    }

    @Override
    public List<StatefulSet> getStatefulSets(GizmoK8sStacker stacker) {
        final KubernetesClient client = stacker.getClient();
        return documentsByKind.get("statefulset").stream()
                .map(doc -> client.apps().statefulSets().load(maybeApplyTemplating(stacker, doc)).get())
                .collect(Collectors.toList());
    }

    @Override
    public List<Deployment> getDeployments(GizmoK8sStacker stacker) {
        final KubernetesClient client = stacker.getClient();
        return documentsByKind.get("deployment").stream()
                .map(doc -> client.extensions().deployments().load(maybeApplyTemplating(stacker, doc)).get())
                .collect(Collectors.toList());
    }

    @Override
    public List<ReplicationController> getReplicationControllers(GizmoK8sStacker stacker) {
        final KubernetesClient client = stacker.getClient();
        return documentsByKind.get("replicationcontroller").stream()
            .map(doc -> client.replicationControllers().load(maybeApplyTemplating(stacker, doc)).get())
            .collect(Collectors.toList());
    }

    @Override
    public List<Pod> getPods(GizmoK8sStacker stacker) {
        final KubernetesClient client = stacker.getClient();
        return documentsByKind.get("pod").stream()
            .map(doc -> client.pods().load(maybeApplyTemplating(stacker, doc)).get())
            .collect(Collectors.toList());
    }

    private static String getKind(Object document) {
        if (document instanceof Map) {
            Map<?,?> documentAsMap = (Map<?,?>)document;
            Object type = documentAsMap.get("kind");
            if (type instanceof String) {
                return (String)type;
            }
        }
        throw new RuntimeException("Unable to determine kind in " + document);
    }

    @Override
    public List<Consumer<GizmoK8sStacker>> getWaitingRules() {
        return Collections.emptyList();
    }

}
