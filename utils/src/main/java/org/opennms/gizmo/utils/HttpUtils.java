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
package org.opennms.gizmo.utils;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpUtils {
    private static final Logger LOG = LoggerFactory.getLogger(HttpUtils.class);

    public static String get(InetSocketAddress httpAddr, String url) throws IOException {
        return get(httpAddr, null, null, url);
    }

    public static String get(InetSocketAddress httpAddr, String username, String password, String url) throws IOException {
        try {
            final OkHttpClient client = new OkHttpClient();
            Request.Builder builder = new Request.Builder();
            if (username != null && password != null) {
                builder.header("Authorization", Credentials.basic(username, password));
            }
            Request request = builder.url(String.format("http://%s:%d%s",
                    httpAddr.getHostString(), httpAddr.getPort(), url))
                .build();
            LOG.info("Calling URL: {}", request.url());
            Response response = client.newCall(request).execute();
            return response.isSuccessful() ? response.body().string() : null;
        } catch (IOException e) {
            LOG.info("Calling URL failed: {}", e.getMessage());
            return null;
        }
    }
}
