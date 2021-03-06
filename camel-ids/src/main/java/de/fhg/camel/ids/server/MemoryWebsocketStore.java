/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fhg.camel.ids.server;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryWebsocketStore extends ConcurrentHashMap<String, DefaultWebsocket> implements WebsocketStore {

    private static final long serialVersionUID = -2826843758230613922L;

    @Override
    public void add(DefaultWebsocket ws) {
        super.put(ws.getConnectionKey(), ws);
    }

    @Override
    public void remove(DefaultWebsocket ws) {
        super.remove(ws.getConnectionKey());
    }

    @Override
    public void remove(String key) {
        super.remove(key);
    }

    @Override
    public DefaultWebsocket get(String key) {
        return super.get(key);
    }

    @Override
    public Collection<DefaultWebsocket> getAll() {
        return super.values();
    }

    @Override
    public void start() throws Exception {
        // noop
    }

    @Override
    public void stop() throws Exception {
        clear();
    }
}