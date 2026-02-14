package com.kalynx.simplyabinaryserializer.serializer;

/**
 * Fast byte array writer that writes directly without stream overhead.
 * Avoids DataOutputStream method call overhead for hot paths.
 */
public class FastByteWriter {
    private byte[] buf;
    private int pos;

    public void setBuffer(byte[] buffer) {
        this.buf = buffer;
        this.pos = 0;
    }

    public void reset(int capacity) {
        if (buf == null || buf.length < capacity) {
            buf = new byte[capacity];
        }
        pos = 0;
    }

    public byte[] toByteArray() {
        byte[] result = new byte[pos];
        System.arraycopy(buf, 0, result, 0, pos);
        return result;
    }

    public void setPosition(int position) {
        this.pos = position;
    }

    public int getPosition() {
        return pos;
    }

    public byte[] getBuffer() {
        return buf;
    }

    public void writeByte(int v) {
        ensureCapacity(1);
        buf[pos++] = (byte) v;
    }

    // Inline varint writing to avoid method call overhead
    public final void writeVarint(int value) {
        ensureCapacity(5); // Worst case: 5 bytes for int32
        if (value < 128) {
            buf[pos++] = (byte) value;
        } else if (value < 16384) {
            buf[pos++] = (byte) ((value & 0x7F) | 0x80);
            buf[pos++] = (byte) (value >>> 7);
        } else {
            while (value > 127) {
                buf[pos++] = (byte) ((value & 0x7F) | 0x80);
                value >>>= 7;
            }
            buf[pos++] = (byte) value;
        }
    }

    public final void writeShort(int v) {
        ensureCapacity(2);
        buf[pos++] = (byte) (v >>> 8);
        buf[pos++] = (byte) v;
    }

    public final void writeShort(short v) {
        writeShort((int) v);
    }

    public final void writeInt(int v) {
        ensureCapacity(4);
        buf[pos++] = (byte) (v >>> 24);
        buf[pos++] = (byte) (v >>> 16);
        buf[pos++] = (byte) (v >>> 8);
        buf[pos++] = (byte) v;
    }

    public final void writeLong(long v) {
        ensureCapacity(8);
        buf[pos++] = (byte) (v >>> 56);
        buf[pos++] = (byte) (v >>> 48);
        buf[pos++] = (byte) (v >>> 40);
        buf[pos++] = (byte) (v >>> 32);
        buf[pos++] = (byte) (v >>> 24);
        buf[pos++] = (byte) (v >>> 16);
        buf[pos++] = (byte) (v >>> 8);
        buf[pos++] = (byte) v;
    }

    public final void writeBoolean(boolean v) {
        ensureCapacity(1);
        buf[pos++] = (byte) (v ? 1 : 0);
    }

    public final void writeDouble(double v) {
        writeLong(Double.doubleToLongBits(v));
    }

    public final void writeFloat(float v) {
        writeInt(Float.floatToIntBits(v));
    }

    public final void writeBytes(byte[] src) {
        ensureCapacity(src.length);
        System.arraycopy(src, 0, buf, pos, src.length);
        pos += src.length;
    }

    private void ensureCapacity(int additional) {
        if (pos + additional > buf.length) {
            byte[] newBuf = new byte[Math.max(buf.length * 2, pos + additional)];
            System.arraycopy(buf, 0, newBuf, 0, pos);
            buf = newBuf;
        }
    }

    public final void writeString(byte[] strBytes) {
        writeShort(strBytes.length);
        System.arraycopy(strBytes, 0, buf, pos, strBytes.length);
        pos += strBytes.length;
    }

    public final void writeBytes(byte[] bytes, int length) {
        ensureCapacity(length);
        System.arraycopy(bytes, 0, buf, pos, length);
        pos += length;
    }
}
