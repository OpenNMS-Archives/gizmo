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
package org.opennms.gizmo.junit;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Inspired by org.junit.rules.ExternalResource with the following additions:
 *  - A boolean is passed to after() indicating if any exceptions were thrown
 *    (did any tests fail?), allowing it to alter the way it tears down resources.
 *  - after() is always called, even when before() fails
 *
 * @author jwhite
 */
public abstract class ExternalResourceRule implements TestRule {
    public Statement apply(Statement base, Description description) {
        return statement(base);
    }

    private Statement statement(final Statement base) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                boolean didFail = true;
                try {
                    before();
                    base.evaluate();
                    didFail = false;
                } catch (Throwable t) {
                    throw t;
                } finally {
                    after(didFail);
                }
            }
        };
    }

    /**
     * Override to set up your specific external resource.
     *
     * @throws Throwable if setup fails (which will disable {@code after}
     */
    protected void before() throws Throwable {
        // do nothing
    }

    /**
     * Override to tear down your specific external resource.
     */
    protected void after(boolean didFail) {
        // do nothing
    }
}
