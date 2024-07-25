package com.advantech.nfclib.api.cmd;

import android.nfc.tech.NfcV;
import android.util.Log;

import java.io.IOException;

/**
 * Created by david on 2018/1/3.
 */

public class NfcVCommand
{
    private static final boolean LOG_ENABLED = false;
    private static String TAG = "NfcV_CMD";
    private static boolean dump = LOG_ENABLED;
    protected NfcV tag;

    public NfcVCommand(NfcV nfcvTag) {
        tag = nfcvTag;
    }

    public byte[] transferRF(byte[] request) {
        byte[] response = {};

        String s = "";
        // request
        if(dump) {
            s = "(->) ";
            for (int i = 0; i < request.length; i++) {
                s += String.format("%02X ", request[i]);
            }

            s += " (<-) ";
        }

        // test RF command
        try {
            tag.close();
            tag.connect();

            // request_flags command_code parameters data
            synchronized (tag) {
                int count = 10;

                while(count>0) {
                    try {
                        response = tag.transceive(request);
                        break;
                    } catch(IOException e) {
                        count--;
                        if(count==0)
                            return null;
                    }
                }
            }

            // dump log
            if(dump) {
                for(int i=0;i<response.length;i++) { s += String.format( "%02X ", response[i]); }
                Log.v(TAG, s);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return response;
    }

    public boolean isResponseOK(byte[] response) {

        // check RF response code
        if(response==null)
            return false;
        if((response[0]&1)!=0) {
            return false;
        }
        return true;
    }

    public boolean isValid() {
        return tag.isConnected();
    }

}
