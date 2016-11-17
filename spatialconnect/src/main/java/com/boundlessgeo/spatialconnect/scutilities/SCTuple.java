/**
 * Copyright 2015-2016 Boundless, http://boundlessgeo.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License
 */
package com.boundlessgeo.spatialconnect.scutilities;

public class SCTuple<T, U, V> {

    private final T first;
    private final U second;
    private final V third;

    public SCTuple(T first, U second, V third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public T first() { return first; }
    public U second() { return second; }
    public V third() { return third; }
}
