package org.opennms.gizmo.k8s.helm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.opennms.gizmo.k8s.GizmoK8sStacker;
import org.opennms.gizmo.k8s.utils.StackUtils;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

public class HelmUtils {

    private static boolean isTillerRunning(final GizmoK8sStacker stacker) {
        return StackUtils.getFirstReadyPod(
                stacker.getPodsWithLabels(ImmutableMap.of(
                        "app", "helm",
                        "name", "tiller"))) != null;
    }

    public static void init(final GizmoK8sStacker stacker) {
        if (isTillerRunning(stacker)) {
            return;
        }

        CommandLine cmdLine = new CommandLine("helm");
        cmdLine.addArgument("init");
        cmdLine.addArgument("--upgrade");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PumpStreamHandler psh = new PumpStreamHandler(out, err);

        DefaultExecutor executor = new DefaultExecutor();
        final ExecuteWatchdog wd = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
        executor.setWatchdog(wd);
        executor.setStreamHandler(psh);

        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        try {
            executor.execute(cmdLine, resultHandler);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        try {
            resultHandler.waitFor();
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }

        final int exitCode = resultHandler.getExitValue();
        if (exitCode != 0) {
            throw new RuntimeException(String.format("Helm initialization failed with exit code %d. Stdout: %s Stderr: %s",
                    exitCode, out, err));
        }

        int attempts = 0;
        while(!isTillerRunning(stacker) && attempts < 5) {
            attempts++;
            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public static void install(String namespace, String chart) {
        CommandLine cmdLine = new CommandLine("helm");
        cmdLine.addArgument("install");
        cmdLine.addArgument("--namespace=${namespace}");
        cmdLine.addArgument("--name=${namespace}");
        cmdLine.addArgument("${chart}");

        HashMap<String, String> map = new HashMap<>();
        map.put("namespace", namespace);
        map.put("chart", chart);
        cmdLine.setSubstitutionMap(map);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PumpStreamHandler psh = new PumpStreamHandler(out, err);

        DefaultExecutor executor = new DefaultExecutor();
        final ExecuteWatchdog wd = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
        executor.setWatchdog(wd);
        executor.setStreamHandler(psh);

        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        try {
            executor.execute(cmdLine, resultHandler);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        try {
            resultHandler.waitFor();
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }

        final int exitCode = resultHandler.getExitValue();
        if (exitCode != 0) {
            throw new RuntimeException(String.format("Chart installation failed with exit code %d. Stdout: %s Stderr: %s",
                    exitCode, out, err));
        }
    }
}
