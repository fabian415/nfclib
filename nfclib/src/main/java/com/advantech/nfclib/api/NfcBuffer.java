package com.advantech.nfclib.api;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by david on 2018/1/15.
 */

public class NfcBuffer {
    public static final int DEFAULT_READ_BUFFER_SIZE = 512;
    public static final int DEFAULT_WRITE_BUFFER_SIZE = 512;

    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;

    public NfcBuffer() {
        writeBuffer = ByteBuffer.allocate(DEFAULT_WRITE_BUFFER_SIZE);
        readBuffer = ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);
    }

    public boolean putReadBuffer(ByteBuffer data) {
        synchronized (this) {
            try {
                readBuffer.put(data);
            } catch (BufferOverflowException e) {
                return false;
            }
        }
        return true;
    }

    public int getReadBufferLength() {
        synchronized (this) {
            return readBuffer.position();
        }
    }

    public byte[] getDataReceived() {
        synchronized (this) {
            byte[] dst = new byte[readBuffer.position()];
            readBuffer.position(0);
            readBuffer.get(dst, 0, dst.length);
            clearReadBuffer();
            return dst;
        }
    }

    public void clearReadBuffer() {
        synchronized (this) {
            readBuffer.clear();
        }
    }


    public boolean putWriteBuffer(ByteBuffer data) {
        synchronized (this) {
            try {
                writeBuffer.put(data);
            } catch (BufferOverflowException e) {
                return false;
            }
        }
        return true;
    }

    public int getWriteBufferLength() {
        synchronized (this) {
            return writeBuffer.position();
        }
    }

    public byte[] getDataTransmitted() {
        synchronized (this) {
            byte[] dst = new byte[writeBuffer.position()];
            writeBuffer.position(0);
            writeBuffer.get(dst, 0, dst.length);
            clearWriteBuffer(); // empty
            return dst;
        }
    }

    public void clearWriteBuffer() {
        synchronized (this) {
            writeBuffer.clear();
        }
    }
}
