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

import org.opennms.gizmo.GizmoRule;

public class GizmoK8sRule extends GizmoRule<GizmoK8sStack, GizmoK8sStacker> {

    public GizmoK8sRule(GizmoK8sStacker stacker, GizmoK8sRuleBuilder builder) {
        super(stacker, builder.stacks, builder.skipTearDown, builder.skipTearDownOnFailure);
    }

    public static GizmoK8sRuleBuilder builder() {
        return new GizmoK8sRuleBuilder();
    }

}
