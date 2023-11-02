package saml.parser;


import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.zip.Deflater.DEFLATED;

public class EncodingUtils {

    private static final Base64 UNCHUNKED_ENCODER = new Base64(0, new byte[]{'\n'});

    public static String encode(byte[] b) {
        return UNCHUNKED_ENCODER.encodeToString(b);
    }

    static byte[] decode(String s) {
        return UNCHUNKED_ENCODER.decode(s);
    }

    static byte[] deflate(String s) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DeflaterOutputStream deflater = new DeflaterOutputStream(b, new Deflater(DEFLATED, true));
            deflater.write(s.getBytes(UTF_8));
            deflater.finish();
            return b.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Unable to deflate string", e);
        }
    }

    static String inflate(byte[] b) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InflaterOutputStream iout = new InflaterOutputStream(out, new Inflater(true));
            iout.write(b);
            iout.finish();
            return new String(out.toByteArray(), UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Unable to inflate string", e);
        }
    }


    static String samlEncode(String s, boolean deflate) {
        byte[] b;
        if (deflate) {
            b = EncodingUtils.deflate(s);
        }
        else {
            b = s.getBytes(UTF_8);
        }
        return EncodingUtils.encode(b);
    }

    public static String samlDecode(String s, boolean inflate) {
        byte[] b = EncodingUtils.decode(s);
        if (inflate) {
            return EncodingUtils.inflate(b);
        }
        else {
            return new String(b, UTF_8);
        }
    }
}
