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
package com.boundlessgeo.spatialconnect.scutilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SerializationUtils {

    /**
     * Turn an array of bytes containing a serialized Object into the deserialized Object.
     *
     * @param bytes
     * @return Object
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            Object o = in.readObject();
            return o;
        }
        finally {
            try {
                bis.close();
            }
            catch (IOException ex) {
            }
            try {
                if (in != null) {
                    in.close();
                }
            }
            catch (IOException ex) {
            }
        }
    }

    /**
     * Transform an Object into an array of bytes.
     *
     * @param object
     * @return byte[] of serialized object
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static byte[] serialize(Object object) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(object);
            return bos.toByteArray();
        }
        finally {
            try {
                bos.close();
            }
            catch (IOException ex) {
            }
            try {
                if (out != null) {
                    out.close();
                }
            }
            catch (IOException ex) {
            }
        }
    }
}
