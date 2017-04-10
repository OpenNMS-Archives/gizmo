package org.opennms.gizmo.k8s.stacks;

import java.util.Objects;

import org.opennms.gizmo.k8s.GizmoK8sStacker;
import org.opennms.gizmo.k8s.helm.HelmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClient;

public class HelmChartBasedStack extends ComponentBasedK8sStack {
    private static final Logger LOG = LoggerFactory.getLogger(HelmChartBasedStack.class);

    private final String chart;

    public HelmChartBasedStack(String chart) {
        this.chart = Objects.requireNonNull(chart);
    }

    @Override
    public void create(GizmoK8sStacker stacker, KubernetesClient kubernetes) {
        HelmUtils.init(stacker);

        LOG.info("Installing chart: {}", chart);
        HelmUtils.install(stacker.getNamespace(), chart);
    }

}
