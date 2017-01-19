/**
 * Copyright 2015-2017 Boundless, http://boundlessgeo.com
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
package com.boundlessgeo.spatialconnect.stores;

import android.util.Base64;

import java.io.UnsupportedEncodingException;

public class SCKeyTuple {
    private String storeId;
    private String layerId;
    private String featureId;

    public SCKeyTuple(String storeId, String layerId, String featureId) {
        this.storeId = storeId;
        this.layerId = layerId;
        this.featureId = featureId;
    }

    public SCKeyTuple(String compositeKey) throws UnsupportedEncodingException {
        String[] strs = compositeKey.split("\\.");
        this.storeId = this.decodeString(strs[0]);
        this.layerId = this.decodeString(strs[1]);
        this.featureId = this.decodeString(strs[2]);
    }

    public String getStoreId() {
        return storeId;
    }

    public String getLayerId() {
        return layerId;
    }

    public String getFeatureId() {
        return featureId;
    }

    public String encodedCompositeKey () throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append(this.encodeString(this.storeId));
        sb.append('.');
        sb.append(this.encodeString(this.layerId));
        sb.append('.');
        sb.append(this.encodeString(this.featureId));
        return sb.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.storeId);
        sb.append('.');
        sb.append(this.layerId);
        sb.append('.');
        sb.append(this.featureId);
        return sb.toString();
    }

    private String encodeString(String s) throws UnsupportedEncodingException {
        byte[] data = s.getBytes("UTF-8");
        return Base64.encodeToString(data,Base64.DEFAULT);
    }

    private String decodeString(String s) throws UnsupportedEncodingException {
        byte[] enc = Base64.decode(s,Base64.DEFAULT);
        return new String(enc,"UTF-8");
    }
}
