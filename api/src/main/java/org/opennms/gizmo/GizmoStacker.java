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

import java.io.Closeable;

/**
 * Responsible for setting up and tearing down {@link GizmoStack}s.
 *
 * @author jwhite
 */
public interface GizmoStacker<T extends GizmoStack<?,?>> extends Closeable {

    /**
     * Performs any initialization required, such as setting up the required client
     * sessions, etc...
     *
     * If this method throws an exception, {@link #close()} will still be called.
     *
     * @throws Exception
     */
    void init() throws Exception;

    /**
     * Instantiates the stack.
     *
     * @param stack
     * @throws Exception
     */
    void stack(T stack) throws Exception;

    void tearDown();

}
