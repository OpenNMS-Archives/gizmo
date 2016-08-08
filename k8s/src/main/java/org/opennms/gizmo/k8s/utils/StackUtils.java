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
package org.opennms.gizmo.k8s.utils;

import java.util.List;

import org.opennms.gizmo.k8s.GizmoK8sStacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Pod;

public class StackUtils {
    private static final Logger LOG = LoggerFactory.getLogger(StackUtils.class);

    public static Pod getFirstRunningPod(final GizmoK8sStacker stacker, final String labelKey, final String labelValue) {
        LOG.info("Retrieving pods using selector {}={}", labelKey, labelValue);
        final List<Pod> pods = stacker.getPodsWithLabel(labelKey, labelValue);
        LOG.info("Found {} pods.", pods.size());
        if (pods.size() > 0) {
            for (Pod pod : pods) {
                if (KubernetesHelper.isPodRunning(pod)) {
                    LOG.info("{} is running.", pod.getMetadata().getName());
                    return pod;
                }
            }
            LOG.info("None of the pods are running.");
        }
        return null;
    }
}
