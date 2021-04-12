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

package com.baidu.hugegraph.traversal.algorithm.records;

import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;

public class Int2IntsMap {

    private static final int INIT_KEY_CAPACITY = 1000000;
    private static final int CHUNK_SIZE = 10;
    private static final int POSITION_OFFSET = 0;
    private static final int SIZE_OFFSET = 1;
    private static final int FIRST_CHUNK_DATA_OFFSET = 2;
    private static final int EXPANSION_FACTOR = 2;

    /*
     *  firstChunkMap         chunkTable
     *
     *   --------              ---------------
     *  | 1 | 0  |--------->0 |      33       | (nextPosition)
     *  | 2 | 10 |-----+    1 |      19       | (size)
     *  | 3 | 40 |---+ |    2 |   int data    |
     *  | . | .  |   | |    3 |   int data    |
     *  | x | y  |   | |    . |     ...       |
     *   --------    | |    9 |      20       |-----------------+
     *               | |       ---------------                  |
     *               | +-->10 |     13        | (nextPosition)  |
     *               |     11 |      1        | (size)          |
     *               |     12 |  int data     |                 | nextChunk
     *               |     13 |      0        |                 |
     *               |      . |     ...       |                 |
     *               |     19 |      0        |                 |
     *               |         ---------------                  |
     *               |     20 |   int data    |<----------------+
     *               |     21 |   int data    |
     *               |     22 |   int data    |
     *               |     23 |   int data    |
     *               |      . |     ...       |
     *               |     29 |      30       |-----------------+
     *               |         ---------------                  | nextChunk
     *               |     30 |   int data    |<----------------+
     *               |     31 |   int data    |
     *               |     32 |   int data    |
     *               |     33 |      0        |
     *               |      . |     ...       |
     *               |     39 |      0        |
     *               |         ---------------
     *               +---->40 |      48       | (nextPosition)
     *                     41 |       6       | (size)
     *                     42 |   int data    |
     *                     43 |   int data    |
     *                      . |     ...       |
     *                     47 |   int data    |
     *                     48 |      0        |
     *                     49 |      0        |
     *                         ---------------
     *                     50 |     ...       |
     *                        |     ...       |
     *                        |     ...       |
     *                        |     ...       |
     *
     *
     */

    private IntIntHashMap firstChunkMap;
    private int[] chunkTable;

    private int nextChunk;

    public Int2IntsMap() {
        this.firstChunkMap = new IntIntHashMap(INIT_KEY_CAPACITY);
        this.chunkTable = new int[INIT_KEY_CAPACITY * CHUNK_SIZE];
        this.nextChunk = 0;
    }

    public void add(int key, int value) {
        if (this.firstChunkMap.containsKey(key)) {
            int firstChunk = this.firstChunkMap.get(key);
            int nextPosition = this.chunkTable[firstChunk + POSITION_OFFSET];
            if (!this.endOfChunk(nextPosition)) {
                chunkTable[nextPosition] = value;
                this.chunkTable[firstChunk + POSITION_OFFSET]++;
            } else {
                this.ensureCapacity();

                this.chunkTable[nextPosition] = this.nextChunk;

                this.chunkTable[this.nextChunk] = value;
                this.chunkTable[firstChunk + POSITION_OFFSET] = this.nextChunk + 1;

                // Update next block
                this.nextChunk += CHUNK_SIZE;
            }
            this.chunkTable[firstChunk + SIZE_OFFSET]++;
        } else {
            // New key, allocate 1st chunk and init
            this.ensureCapacity();

            // Allocate 1st chunk
            this.firstChunkMap.put(key, this.nextChunk);

            // Init first chunk
            this.chunkTable[this.nextChunk] = this.nextChunk +
                                              FIRST_CHUNK_DATA_OFFSET + 1;
            this.chunkTable[this.nextChunk + SIZE_OFFSET] = 1;
            this.chunkTable[this.nextChunk + FIRST_CHUNK_DATA_OFFSET] = value;

            // Update next block
            this.nextChunk += CHUNK_SIZE;
        }
    }

    public boolean containsKey(int key) {
        return this.firstChunkMap.containsKey(key);
    }

    public int[] get(int key) {
        int firstChunk = this.firstChunkMap.get(key);
        int size = this.chunkTable[firstChunk + SIZE_OFFSET];
        int[] values = new int[size];
        int i = 0;
        int position = firstChunk + FIRST_CHUNK_DATA_OFFSET;
        while (i < size) {
            if (!this.endOfChunk(position)) {
                values[i++] = this.chunkTable[position++];
            } else {
                position = this.chunkTable[position];
            }
        }
        return values;
    }

    public IntIterator keyIterator() {
        return this.firstChunkMap.keySet().intIterator();
    }

    public int size() {
        return this.firstChunkMap.size();
    }

    private boolean endOfChunk(int position) {
        return (position + 1) % CHUNK_SIZE == 0;
    }

    private void ensureCapacity() {
        if (this.nextChunk >= this.chunkTable.length) {
            this.expansion();
        }
    }

    private void expansion() {
        int currentSize = this.chunkTable.length;
        int[] newTable = new int[currentSize * EXPANSION_FACTOR];
        System.arraycopy(this.chunkTable, 0, newTable, 0, currentSize);
        this.chunkTable = newTable;
    }
}
