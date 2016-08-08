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
package org.opennms.gizmo;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.opennms.gizmo.junit.ExternalResourceRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses the provided {@link GizmoStacker} to setup and optionally tear down
 * {@link GizmoStack}s before/after test execution.
 *
 * @author jwhite
 *
 * @param <T> type of stack
 * @param <U> type of stacker
 */
public class GizmoRule<T extends GizmoStack<T, U>, U extends GizmoStacker<T>> extends ExternalResourceRule {
    private static final Logger LOG = LoggerFactory.getLogger(GizmoRule.class);

    private final U stacker;
    private final List<T> stacks;
    private final boolean skipTearDown;
    private final boolean skipTearDownOnFailure;

    public GizmoRule(U stacker, List<T> stacks, boolean skipTearDown, boolean skipTearDownOnFailure) {
        this.stacker = Objects.requireNonNull(stacker);
        this.stacks = Objects.requireNonNull(stacks);
        this.skipTearDown = skipTearDown;
        this.skipTearDownOnFailure = skipTearDownOnFailure;
    }

    @Override
    public void before() throws Exception {        
        stacker.init();

        for (T stack : stacks) {
            stacker.stack(stack);
        }
    };

    @Override
    public void after(boolean didFail) {
        if (didFail) {
            LOG.warn("One or more tests failed.");
        }

        if (skipTearDown || (didFail && skipTearDownOnFailure)) {
            LOG.info("Skipping tear down.");
        } else {
            LOG.info("Tearing down...");
            stacker.tearDown();
        }

        try {
            stacker.close();
        } catch (IOException e) {
            LOG.error("An error occured while closting the stacker.", e);
        }
    };

    public U getStacker() {
        return stacker;
    }
}
