package org.joemonti.util;

import java.io.UnsupportedEncodingException;

/**
 * Created by joe on 6/12/16.
 */
public class ByteUtil {
    public static final String UTF8_ENCODING = "UTF-8";

    public static final int SIZEOF_BYTE = 1;
    public static final int SIZEOF_LONG = Long.SIZE / Byte.SIZE;
    public static final int SIZEOF_INT = Integer.SIZE / Byte.SIZE;
    public static final int SIZEOF_SHORT = Short.SIZE / Byte.SIZE;

    public static double readDouble( byte[] bytes, int offset ) {
        if ( offset < 0 || ( offset + SIZEOF_LONG ) > bytes.length ) {
            throw new IllegalArgumentException( "Invalid offset " + offset + " for bytes length " + bytes.length );
        }
        long value = 0l;
        for ( int i = offset; i < ( offset + SIZEOF_LONG ); i++ ) {
            value <<= 8;
            value ^= bytes[i] & 0xFF;
        }
        return Double.longBitsToDouble( value );
    }

    public static int writeDouble( byte[] bytes, int offset, double value ) {
        if ( offset < 0 || ( offset + SIZEOF_LONG ) > bytes.length ) {
            throw new IllegalArgumentException( "Invalid offset " + offset + " for bytes length " + bytes.length );
        }
        long lvalue = Double.doubleToLongBits( value );
        for ( int i = offset + ( SIZEOF_LONG - 1 ); i > offset; i-- ) {
            bytes[i] = (byte) lvalue;
            lvalue >>>= 8;
        }
        bytes[offset] = (byte) lvalue;
        return offset + SIZEOF_LONG;
    }

    public static long readLong( byte[] bytes, int offset ) {
        if ( offset < 0 || ( offset + SIZEOF_LONG ) > bytes.length ) {
            throw new IllegalArgumentException( "Invalid offset " + offset + " for bytes length " + bytes.length );
        }
        long value = 0l;
        for ( int i = offset; i < ( offset + SIZEOF_LONG ); i++ ) {
            value <<= 8;
            value ^= bytes[i] & 0xFF;
        }
        return value;
    }

    public static int writeLong( byte[] bytes, int offset, long value ) {
        if ( offset < 0 || ( offset + SIZEOF_LONG ) > bytes.length ) {
            throw new IllegalArgumentException( "Invalid offset " + offset + " for bytes length " + bytes.length );
        }
        for ( int i = offset + ( SIZEOF_LONG - 1 ); i > offset; i-- ) {
            bytes[i] = (byte) value;
            value >>>= 8;
        }
        bytes[offset] = (byte) value;
        return offset + SIZEOF_LONG;
    }

    public static int readInt( byte[] bytes, int offset ) {
        if ( offset < 0 || ( offset + SIZEOF_INT ) > bytes.length ) {
            throw new IllegalArgumentException( "Invalid offset " + offset + " for bytes length " + bytes.length );
        }
        int value = 0;
        for ( int i = offset; i < ( offset + SIZEOF_INT ); i++ ) {
            value <<= 8;
            value ^= bytes[i] & 0xFF;
        }
        return value;
    }

    public static int writeInt( byte[] bytes, int offset, int value ) {
        if ( offset < 0 || ( offset + SIZEOF_INT ) > bytes.length ) {
            throw new IllegalArgumentException( "Invalid offset " + offset + " for bytes length " + bytes.length );
        }
        for ( int i = offset + ( SIZEOF_INT - 1 ); i > offset; i-- ) {
            bytes[i] = (byte) value;
            value >>>= 8;
        }
        bytes[offset] = (byte) value;
        return offset + SIZEOF_INT;
    }

    public static short readShort( byte[] bytes, int offset ) {
        if ( offset < 0 || ( offset + SIZEOF_SHORT ) > bytes.length ) {
            throw new IllegalArgumentException( "Invalid offset " + offset + " for bytes length " + bytes.length );
        }
        short value = 0;
        for ( int i = offset; i < ( offset + SIZEOF_INT ); i++ ) {
            value <<= 8;
            value ^= bytes[i] & 0xFF;
        }
        return value;
    }

    public static int writeShort( byte[] bytes, int offset, short value ) {
        if ( offset < 0 || ( offset + SIZEOF_SHORT ) > bytes.length ) {
            throw new IllegalArgumentException( "Invalid offset " + offset + " for bytes length " + bytes.length );
        }
        for ( int i = offset + ( SIZEOF_SHORT - 1 ); i > offset; i-- ) {
            bytes[i] = (byte) value;
            value >>>= 8;
        }
        bytes[offset] = (byte) value;
        return offset + SIZEOF_SHORT;
    }

    public static int writeByte( byte[] bytes, int offset, byte value ) {
        bytes[offset] = value;
        return offset + 1;
    }

    public static void readBytes( byte[] bytes, int offset, byte[] dst, int dstOffset, int length ) {
        System.arraycopy( bytes, offset, dst, dstOffset, length );
    }

    public static int writeBytes( byte[] bytes, int offset, byte[] value, int valueOffset, int length ) {
        System.arraycopy( value, valueOffset, bytes, offset, length );
        return offset + length;
    }

    public static String readString( byte[] bytes, int offset, int length ) {
        if ( bytes == null ) return null;
        if ( length == 0 ) return "";

        try {
            return new String( bytes, offset, length, UTF8_ENCODING );
        } catch ( UnsupportedEncodingException ex ) {
            throw new IllegalArgumentException( "Unable to encode " + UTF8_ENCODING, ex );
        }
    }

    public static int writeString( byte[] bytes, int offset, String value ) {
        byte[] valueBytes;

        try {
            valueBytes = value.getBytes( UTF8_ENCODING );
        } catch ( UnsupportedEncodingException ex ) {
            throw new IllegalArgumentException( "Unable to encode " + UTF8_ENCODING, ex );
        }

        return writeBytes( bytes, offset, valueBytes, 0, valueBytes.length );
    }
}
