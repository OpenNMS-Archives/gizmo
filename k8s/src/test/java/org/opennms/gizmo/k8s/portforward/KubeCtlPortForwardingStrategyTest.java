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
package org.opennms.gizmo.k8s.portforward;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class KubeCtlPortForwardingStrategyTest {

    @Test
    public void canParseLocalPortFromOutput() {
        String output = "Forwarding from 127.0.0.1:40743 -> 8980\n" +
                        "Forwarding from [::1]:40743 -> 8980\n";
        check(output, 40743);
        
        output = "Forwarding from 127.0.0.1:40744 -> 2222\n" +
                "Forwarding from [::1]:40744 -> 2222\n";
        check(output, 40744);
    }

    private void check(String out, Integer port) {
        assertEquals(port, KubeCtlPortForwardingStrategy.getLocalPortFromOutput(out));
    }
}
