/*
 *
 *  * ****************************************************************************
 *  *  Licensed to the Apache Software Foundation (ASF) under one
 *  *  or more contributor license agreements.  See the NOTICE file
 *  *  distributed with this work for additional information
 *  *  regarding copyright ownership.  The ASF licenses this file
 *  *  to you under the Apache License, Version 2.0 (the
 *  *  "License"); you may not use this file except in compliance
 *  *  with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing,
 *  *  software distributed under the License is distributed on an
 *  *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  *  KIND, either express or implied.  See the License for the
 *  *  specific language governing permissions and limitations
 *  *  under the License.
 *  * ****************************************************************************
 *
 */

package com.boundlessgeo.spatialconnect.stores;

public enum SCDataStoreStatus
{
    SC_DATA_STORE_STARTED,  // when store is initialized
    SC_DATA_STORE_DOWNLOADING, // when the store needs to download data from a remote location
    SC_DATA_STORE_RUNNING, // when the store is running an ready to be used
    SC_DATA_STORE_PAUSED,
    SC_DATA_STORE_STOPPED, // before the store is initialized OR if was unsuccessful trying to get to the running state
    SC_DATA_SERVICE_ALLSTORESSTARTED // when all stores are started
}
