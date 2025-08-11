package com.ohmyimage.util;

import org.apache.tomcat.util.codec.binary.Base64;

public class Base64Util {
    public static String encodeBase64(byte[] bytes) {
        if (bytes == null) return "";
        return Base64.encodeBase64String(bytes);
    }
}