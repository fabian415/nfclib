package com.advantech.nfclib.api.cmd;

import android.nfc.tech.NfcV;
import android.util.Log;

import com.advantech.nfclib.NFCException;
import com.advantech.nfclib.NFCManager;
import com.advantech.nfclib.NFCException;

import java.util.Arrays;

/**
 * Created by david on 2018/1/3.
 */

public class D30Command extends NfcVCommand
{
    private static String TAG = "D30__CMD";
    private static final boolean LOG_ENABLED = false;

    public static final byte CMD_INVENTORY = 0X01;
    public static final byte CMD_QUIET = 0x02;

    public static final byte CMD_READ_SINGLE_BLOCK = 0X20;
    public static final byte CMD_WRITE_SINGLE_BLOCK = 0x21;
    public static final byte CMD_LOCK_BLOCK = 0x022;
    public static final byte CMD_READ_MULTIPLE_BLOCKS = 0x23;
    public static final byte CMD_WRITE_MULTIPLE_BLOCKS = 0x24;

    public static final byte CMD_SELECT = 0x25;
    public static final byte CMD_RESET_TO_READY = 0x26;
    public static final byte CMD_WRITE_AFI = 0x27;
    public static final byte CMD_LOCK_AFI = 0x28;
    public static final byte CMD_WRITE_DSFID = 0x29;

    public static final byte CMD_EXT_READ_SINGLE_BLOCK = 0x30;
    public static final byte CMD_EXT_WRITE_SINGLE_BLOCK = 0x31;
    public static final byte CMD_EXT_LOCK_BLOCK = 0x32;
    public static final byte CMD_EXT_READ_MULTIPLE_BLOCKS = 0x33;
    public static final byte CMD_EXT_WRITE_MULTIPLE_BLOCKS = 0x34;

    public static final byte CMD_LOCK_DSFID = 0x2A;
    public static final byte CMD_GET_SYSTEM_INFO = 0x2B;
    public static final byte CMD_MULTIPLE_BLOCK_SECURITY_STATUS = 0x2C;

    public static final byte CMD_EXT_GET_SYSTEM_INFO = 0x3B;
    public static final byte CMD_EXT_MULTIPLE_BLOCK_SECURITY_STATUS = 0x3C;

    public static final byte CMD_READ_CONFIGURATION = (byte)0xA0;
    public static final byte CMD_WRITE_CONFIGURATION = (byte)0xA1;
    public static final byte CMD_MANGE_GPO = (byte)0xA2;
    public static final byte CMD_WRITE_MESSSAGE = (byte)0xAA;
    public static final byte CMD_READ_MESSAGE_LENGTH = (byte)0xAB;
    public static final byte CMD_READ_MESSAGE = (byte)0xAC;
    public static final byte CMD_READ_DYN_CONFIGURATION = (byte)0xAD;
    public static final byte CMD_WRITE_DYN_CONFIGURATION = (byte)0xAE;
    public static final byte CMD_WRITE_PASSWORD = (byte)0xB1;
    public static final byte CMD_PRESENT_PASSWORD = (byte)0xB3;
    public static final byte CMD_FAST_READ_SINGLE_BLOCK = (byte)0xC0;
    public static final byte CMD_FAST_READ_MULTIPLE_BLOCKS = (byte)0xC3;
    public static final byte CMD_FAST_EXT_READ_SINGLE_BLOCK = (byte)0xC4;
    public static final byte CMD_FAST_EXT_READ_MULTIPLE_BLOCKS = (byte)0xC5;

    public static final byte CMD_FAST_WRITE_MESSAGE = (byte)0xCA;
    public static final byte CMD_FAST_READ_MESSAGE_LENGTH = (byte)0xCB;
    public static final byte CMD_FAST_READ_MESSAGE = (byte)0xCC;
    public static final byte CMD_FAST_READ_DYN_CONFIGURATION = (byte)0xCD;
    public static final byte CMD_FAST_WRITE_DYN_CONFIGURATION = (byte)0xCE;

    public static final byte REQ_FLAG_SUBCARRIER = 0x1;
    public static final byte REQ_FLAG_HIGH_DATA_RATE = 0x02;
    public static final byte REQ_FLAG_INVENTORY = 0x04;
    public static final byte REQ_FLAG_EXT_PROTOCOL = 0x08;
    public static final byte REQ_FLAG_SELECT = 0x10;
    public static final byte REQ_FLAG_ADDRESS = 0x20;
    public static final byte REQ_FLAG_OPTION = 0x40;
    public static final byte REQ_FLAG_RFU = (byte)0x80;

    public static final byte DYN_ADDR_GPO = 0x00;
    public static final byte DYN_ADDR_EH_CTRL = 0x02;
    public static final byte DYN_ADDR_MB_CTRL = 0x0D;

    public static final byte CFG_ADDR_EH_MODE = 0x02;

    public static final byte MB_CTRL_BIT_MB_EN = 0x01;
    public static final byte MB_CTRL_BIT_HOST_PUT_MSG = 0x02;
    public static final byte MB_CTRL_BIT_RF_PUT_MSG = 0x04;
    public static final byte MB_CTRL_BIT_RFU = 0x08;
    public static final byte MB_CTRL_BIT_HOST_MISS_MSG = 0x10;
    public static final byte MB_CTRL_BIT_RF_MISS_MSG = 0x20;
    public static final byte MB_CTRL_BIT_HOST_CURRENT_MSG = 0x40;
    public static final byte MB_CTRL_BIT_RF_CURRENT_MSG = (byte) 0x80;

    public static final byte IC_MFG_CODE = 0x02;

    public static final byte RF_ADDR_EH_MODE = 0x02;

    public D30Command(NfcV nfcvTag) {
        super(nfcvTag);
    }

    private void log(String m) {
        if(LOG_ENABLED) {
            Log.v(TAG, m);
        }
    }

    public byte readDynConfig(byte address) throws NFCException {
        byte request_flag = REQ_FLAG_HIGH_DATA_RATE;

        // Dyn Register EH_CTRL_Dyn
        byte frame[] = new byte[] { request_flag, CMD_READ_DYN_CONFIGURATION, IC_MFG_CODE, address};

        int count = 10;

        while(count > 0) {
            byte response[] = this.transferRF(frame);
            if (response == null) {
                log(String.format("Dyn[%02X]=??", address));
                throw new NFCException();
            } else if (this.isResponseOK(response)) {
                if(response.length>=2) {
                    log(String.format("Dyn[%02X]=%02X", address, response[1]));
                    return response[1];
                } else
                    throw new NFCException();
            } else {
                if(response.length>=2)
                    log(String.format("Dyn[%02X]=%02X [error]", address, response[1]));
                else {
                    log(String.format("Dyn[%02X]=? [error]", address));
                    throw new NFCException();
                }
            }
            count --;
        }
        throw new NFCException();
    }

    public void writeDynConfig(byte address, int data) throws NFCException {
        byte request_flag = REQ_FLAG_HIGH_DATA_RATE;

        // Dyn Register EH_CTRL_Dyn
        byte frame[] = new byte[] { request_flag, CMD_WRITE_DYN_CONFIGURATION, IC_MFG_CODE, address, (byte) data };

        int count = 10;

        while(count > 0) {
            byte response[] = this.transferRF(frame);

            if (response == null) {
                log(String.format("Dyn[%02X]=??", address));
                throw new NFCException();
            } else if (this.isResponseOK(response)) {
                log(String.format("Dyn[%02X]=>%02X", address, (byte)data));
                return;
            } else {
                if(response.length>=2)
                    log(String.format("Dyn[%02X]=%02X [error]", address, response[1]));
                else {
                    log(String.format("Dyn[%02X]=?? [error]", address));
                    throw new NFCException();
                }
            }
            count --;
        }

        throw new NFCException();
    }
    public byte readMessageLength() throws NFCException {
        byte request_flag = REQ_FLAG_HIGH_DATA_RATE;

        byte frame[] = new byte[] { request_flag, CMD_READ_MESSAGE_LENGTH, IC_MFG_CODE };

        int count = 10;

        while(count > 0) {
            byte response[] = this.transferRF(frame);
            if (response == null) {
                log(String.format("MSG_LEN=??"));
                throw new NFCException();
            } else if (this.isResponseOK(response)) {
                log(String.format("MSG_LEN=%02X", response[1]));
                return response[1];
            } else {
                if(response.length>=2)
                    log(String.format("MSG_LEN=%02X [error]", response[1]));
                else {
                    log(String.format("MSG_LEN=? [error]"));
                    throw new NFCException();
                }
            }
            count --;
        }
        throw new NFCException();
    }

    public byte[] readMessage(int pointer, int len) throws NFCException {
        byte request_flag = REQ_FLAG_HIGH_DATA_RATE;

        byte frame[] = new byte[] { request_flag, CMD_READ_MESSAGE, IC_MFG_CODE, (byte) pointer, (byte) len };

        int count = 10;

        while(count > 0) {
            byte response[] = this.transferRF(frame);

            if (response == null) {
                log(String.format("READ_MSG=??"));
                throw new NFCException();
            } else if (this.isResponseOK(response)) {
                return Arrays.copyOfRange(response, 0, response.length);
            } else {
                if(response.length>=2)
                    log(String.format("READ_MSG=%02X [error]", response[1]));
                else {
                    log(String.format("READ_MSG=?? [error]"));
                    throw new NFCException();
                }
            }

            count--;
        }
        throw new NFCException();
    }

    public void writeMessage(byte [] data) throws NFCException {
        byte request_flag = REQ_FLAG_HIGH_DATA_RATE;

        int len = data.length;

        // check length
        if(len > NFCManager.getInstance().getMaxNfcLength() || len <=0) throw new NFCException();

        // build frame
        byte frame[] = new byte[4+len];
        frame[0] = request_flag;
        frame[1] = CMD_WRITE_MESSSAGE;
        frame[2] = IC_MFG_CODE;
        frame[3] = (byte) (len-1);
        for(int i=0;i<len;i++) frame[4+i] = data[i];

        int count = 10;

        while(count > 0) {
            byte response[] = this.transferRF(frame);

            if (response == null) {
                log(String.format("WRITE_MSG=??"));
                throw new NFCException();
            } else if (this.isResponseOK(response)) {
                return;
            } else {
                if(response.length>=2)
                    log(String.format("READ_MSG=%02X [error]", response[1]));
                else {
                    log(String.format("READ_MSG=?? [error]"));
                    throw new NFCException();
                }
            }
            count--;
        }

        throw new NFCException();
    }

    public void writeConfiguration(byte address, byte data)  throws NFCException
    {
        byte request_flag = REQ_FLAG_HIGH_DATA_RATE;

        // build frame
        byte frame[] = new byte[5];
        frame[0] = request_flag;
        frame[1] = CMD_WRITE_CONFIGURATION;
        frame[2] = IC_MFG_CODE;
        frame[3] = address;
        frame[4] = data;

        int count = 10;

        while(count > 0) {
            byte response[] = this.transferRF(frame);

            if (response == null) {
                log(String.format("WRITE_CONF=??"));
                throw new NFCException();
            } else if (this.isResponseOK(response)) {
                return;
            } else {
                if(response.length>=2)
                    log(String.format("WRITE_CONF=%02X [error]", response[1]));
                else {
                    log(String.format("WRITE_CONF=?? [error]"));
                    throw new NFCException();
                }
            }
            count--;
        }

        throw new NFCException();
    }


    public byte readConfiguration(byte address)  throws NFCException
    {
        byte request_flag = REQ_FLAG_HIGH_DATA_RATE;

        // build frame
        byte frame[] = new byte[4];
        frame[0] = request_flag;
        frame[1] = CMD_READ_CONFIGURATION;
        frame[2] = IC_MFG_CODE;
        frame[3] = address;

        int count = 10;

        while(count > 0) {
            byte response[] = this.transferRF(frame);

            if (response == null) {
                log(String.format("READ_CONF=??"));
                throw new NFCException();
            } else if (this.isResponseOK(response)) {
                if(response.length>=2)
                    return response[1];
            } else {
                if(response.length>=2)
                    log(String.format("READ_CONF=%02X [error]", response[1]));
                else {
                    log(String.format("READ_CONF=?? [error]"));
                    throw new NFCException();
                }
            }
            count--;
        }

        throw new NFCException();
    }
    
    public void EnableEH() throws  NFCException {

    }

    public void UnlockRFSecurity() throws NFCException {
        byte request_flag = REQ_FLAG_HIGH_DATA_RATE;

        // build frame
        byte frame[] = new byte[12];
        frame[0] = request_flag;
        frame[1] = CMD_PRESENT_PASSWORD;
        frame[2] = IC_MFG_CODE;
        frame[3] = 0; // password number

        frame[4] = 0;
        frame[5] = 0;
        frame[6] = 0;
        frame[7] = 0;

        frame[8] = 0;
        frame[9] = 0;
        frame[10] = 0;
        frame[11] = 0;

        int count = 10;

        while(count > 0) {
            byte response[] = this.transferRF(frame);

            if (response == null) {
                log(String.format("Unlock=??"));
                throw new NFCException();
            } else if (this.isResponseOK(response)) {
                log("password ok");
                return;
            } else {
                if(response.length>=2)
                    log(String.format("Unlock=%02X [error]", response[1]));
                else {
                    log(String.format("Unlock=?? [error]"));
                    throw new NFCException();
                }
            }
            count--;
        }

        throw new NFCException();
    }
}
