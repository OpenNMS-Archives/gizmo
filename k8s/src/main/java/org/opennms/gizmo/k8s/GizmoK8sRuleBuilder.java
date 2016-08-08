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

import java.util.LinkedList;
import java.util.List;

import io.fabric8.kubernetes.client.KubernetesClient;

public class GizmoK8sRuleBuilder {
    protected KubernetesClient kubernetes;
    protected String namespace = null;
    protected final List<GizmoK8sStack> stacks = new LinkedList<>();
    protected boolean skipTearDown = false;
    protected boolean skipTearDownOnFailure = false;

    public GizmoK8sRuleBuilder withKubernetesClient(KubernetesClient kubernetes) {
        this.kubernetes = kubernetes;
        return this;
    }

    public GizmoK8sRuleBuilder withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public GizmoK8sRuleBuilder withStack(GizmoK8sStack stack) {
        stacks.add(stack);
        return this;
    }

    public GizmoK8sRuleBuilder skipTearDown(boolean skipTearDown) {
        this.skipTearDown = skipTearDown;
        return this;
    }

    public GizmoK8sRuleBuilder skipTearDownOnFailure(boolean skipTearDownOnFailure) {
        this.skipTearDownOnFailure = skipTearDownOnFailure;
        return this;
    }

    public GizmoK8sRule build() {
        if (stacks.size() < 1) {
            throw new IllegalStateException("One or more stacks are required.");
        }
        return new GizmoK8sRule(new GizmoK8sStacker(this), this);
    }
}
