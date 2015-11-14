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

package com.boundlessgeo.spatialconnect.dataAdapter;


import com.boundlessgeo.spatialconnect.config.SCStoreConfig;

public abstract class SCDataAdapter<T>
{
    private String name;
    private String type;
    private int version;
    private int adapterVersion = 0;
    private SCDataAdapterStatus status = SCDataAdapterStatus.DATA_ADAPTER_DISCONNECTED;
    protected SCStoreConfig scStoreConfig;

    public SCDataAdapter() {}
    
    public SCDataAdapter(String name, String type, int version)
    {
        this.name = name;
        this.type = type;
        this.version = version;

    }

    public void connect()
    {
        this.status = SCDataAdapterStatus.DATA_ADAPTER_CONNECTING;
    }

    public void connected()
    {
        this.status = SCDataAdapterStatus.DATA_ADAPTER_CONNECTED;
    }

    public void disconnect()
    {
        this.status = SCDataAdapterStatus.DATA_ADAPTER_DISCONNECTED;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public int getAdapterVersion() { return adapterVersion; }

    public void setAdapterVersion(int adapterVersion) { this.adapterVersion = adapterVersion; }

    public SCDataAdapterStatus getStatus()
    {
        return status;
    }

    public void setStatus(SCDataAdapterStatus status)
    {
        this.status = status;
    }

    public String getDataStoreName() {
        return scStoreConfig.getName();
    }
}
