/*
 * Copyright 2017 HugeGraph Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.baidu.hugegraph.backend.store.memory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.baidu.hugegraph.backend.store.AbstractBackendStoreProvider;
import com.baidu.hugegraph.backend.store.BackendStore;
import com.baidu.hugegraph.backend.store.memory.InMemoryDBStore.InMemoryGraphStore;
import com.baidu.hugegraph.backend.store.memory.InMemoryDBStore.InMemorySchemaStore;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;

public class InMemoryDBStoreProvider extends AbstractBackendStoreProvider {

    private static final Logger LOG = Log.logger(InMemoryDBStore.class);

    private static Map<String, InMemoryDBStoreProvider> providers = null;

    public static synchronized InMemoryDBStoreProvider instance(String name) {
        if (providers == null) {
            providers = new ConcurrentHashMap<>();
        }
        if (!providers.containsKey(name)) {
            InMemoryDBStoreProvider p = new InMemoryDBStoreProvider(name);
            providers.putIfAbsent(name, p);
        }
        return providers.get(name);
    }

    public InMemoryDBStoreProvider(String name) {
        this.open(name);
    }

    @Override
    public BackendStore loadSchemaStore(String name) {
        LOG.debug("InMemoryDBStoreProvider load '{}'", name);

        this.checkOpened();
        if (!this.stores.containsKey(name)) {
            this.stores.putIfAbsent(name, new InMemorySchemaStore(this, name));
        }
        BackendStore store = this.stores.get(name);
        E.checkNotNull(store, "store");
        return store;
    }

    @Override
    public BackendStore loadGraphStore(String name) {
        LOG.debug("InMemoryDBStoreProvider load '{}'", name);

        this.checkOpened();
        if (!this.stores.containsKey(name)) {
            this.stores.putIfAbsent(name, new InMemoryGraphStore(this, name));
        }
        BackendStore store = this.stores.get(name);
        E.checkNotNull(store, "store");
        return store;
    }

    @Override
    public String type() {
        return "memory";
    }
}
