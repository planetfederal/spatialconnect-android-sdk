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

/**
 * The SCKeyTuple class uniquely identifies a feature in SpatialConnect using the store, layer,
 * and feature id.
 */
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SCKeyTuple that = (SCKeyTuple) o;

        if (storeId != null ? !storeId.equals(that.storeId) : that.storeId != null) return false;
        if (layerId != null ? !layerId.equals(that.layerId) : that.layerId != null) return false;
        return featureId != null ? featureId.equals(that.featureId) : that.featureId == null;
    }

    @Override
    public int hashCode() {
        int result = storeId != null ? storeId.hashCode() : 0;
        result = 31 * result + (layerId != null ? layerId.hashCode() : 0);
        result = 31 * result + (featureId != null ? featureId.hashCode() : 0);
        return result;
    }
}
