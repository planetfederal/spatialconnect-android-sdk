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
     * @return
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
     * @return
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
