package com.advantech.nfclib.api;

import android.annotation.SuppressLint;
import android.util.Log;

import com.advantech.nfclib.EinkImage;
import com.advantech.nfclib.NFCException;
import com.advantech.nfclib.NFCManager;
import com.advantech.nfclib.NfcEPDAPI;
import com.advantech.nfclib.api.cmd.D30Command;

import java.util.Arrays;

import static com.advantech.nfclib.NFCExceptionType.NFC_EXCEPTION_TYPE_BUSY;
import static com.advantech.nfclib.NFCExceptionType.NFC_EXCEPTION_TYPE_NO_CALLBACK_ERROR;
import static com.advantech.nfclib.NFCExceptionType.NFC_EXCEPTION_TYPE_NO_TAG;

/**
 * Created by david on 2018/1/6.
 */


public class LeoD30EPDAPI implements NfcEPDAPI, NFCState.NFCSTATEChangeCallback {

    private static final int DEFAULT_TIMEOUT = 1000;
    private static final String INVALID_VERSION = "??";
    private final String TAG = "LEOAPI";
    private NFCState nfcState;

    private final int ACK_SUCCESS = 1;
    private final int ACK_ERROR = 0;

    private final int CMD_VERSION = 0xF0;
    private final int CMD_PLATFORM_NAME = 0xF1;
    private final int CMD_BOOT_TO_LOADER = 0xF4;
    private final int CMD_GET_SN = 0xF6;

    private final int CMD_ERASE_IMAGE_FLASH = 0x80;
    private final int CMD_WRITE_IMAGE_FLASH = 0x81;
    private final int CMD_CHECK_IMAGE_FLASH = 0x82;
    private final int CMD_WRITE_IMAGE_FLASH_NOACK = 0x8E;
    private final int CMD_START_WRITE_FLASH_AND_EPD = 0x83;
    private final int CMD_WRITE_IMAGE_FLASH_AND_EPD = 0x84;
    private final int CMD_END_WRITE_FLASH_AND_EPD = 0x85;
    private final int CMD_GET_EPD_STATUS = 0x88;


    private final int CMD_WRITE_EPD = 0x90;

    private final int CMD_PINGCODE_STATUS = 0xA0;
    private final int CMD_PINCODE_UNLOCK = 0xA1;
    private final int CMD_PINCODE_RESET = 0xA2;
    private final int CMD_PINCODE_SET = 0xA3;
    private final int CMD_SYSTEM_RESET = 0xA4;

    private boolean drawing = false;
    private boolean busy = false;
    private boolean check_epd;

    private final int CMD_SHORT_TIMEOUT = 1000;
    private final int CMD_LONG_TIMEOUT = 40000;

    public LeoD30EPDAPI(NFCState nfcState) {

        this.nfcState = nfcState;
        nfcState.setStateChangeCallBack(this);
    }

    private boolean waitTxReady(int count) {

        while(count>0)
        {
            if(nfcState.readyToTx())
                return true;

            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {
            }
            count--;
        }
        return false;
    }

    private boolean checkChecksum(byte[] recv) {
        int chk = 0;
        for (byte aRecv : recv) {
            chk += (int) aRecv & 0xFF;
        }
        return (chk & 0xFF) == 0;
    }

    private void clearRx() {
        while(true) {
            byte[] recv = nfcState.getRx();
            if(recv==null)
                return;
        }
    }

    private void TxCommand(int command, byte[] data, int timeout_ms)
    {
        clearRx();
        try
        {
            byte[] chunk = nfcState.buildNFCPacket((byte)command, data);

            if (!waitTxReady(timeout_ms))
            {
                Log.v(TAG, "wait tx ready timeout");
                return;
            }

            // send command
            nfcState.addEvent(NFCState.FTMEventType.FTMEVENT_TX_MESSAGE, chunk);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return;
        }
    }

    private byte[] TranceiveCommand(int command, byte [] data, int timeout_ms)
    {
        clearRx();
        try {
            byte[] chunk = nfcState.buildNFCPacket((byte) command, data);

            if(!waitTxReady(timeout_ms)) {
                Log.v(TAG, "wait tx ready timeout");
                return null;
            }

            // send command
            nfcState.addEvent(NFCState.FTMEventType.FTMEVENT_TX_MESSAGE, chunk);

            Log.v(TAG,"start");
            while(timeout_ms>0)
            {
                byte[] recv = nfcState.getRx();
                if(recv!=null) {
                    if(!checkChecksum(recv)) {
                        Log.v(TAG, "response checksum error");
                        return null;
                    }

                    return recv;
                }

                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {
                }
                timeout_ms--;
            }

            Log.v(TAG,"end");
            return null;

        } catch (NFCException e) {
            e.printStackTrace();
            return null;
        }
    }

    boolean CheckResponse(byte[] recv) {
        if(recv!=null && recv.length==2) {
            return recv[0] == ACK_SUCCESS;
        }
        return false;
    }

    private boolean EraseImageFlash(byte lz4flag) {
        byte[] data = { (byte)lz4flag };
        byte[] recv = TranceiveCommand(CMD_ERASE_IMAGE_FLASH, data , CMD_LONG_TIMEOUT);
        return CheckResponse(recv);
    }

    private boolean WriteImageFlash(int address, byte [] data) {
        byte[] send = new byte[2 + data.length];
        send[0] = (byte) ((address >> 8) & 0xFF);
        send[1] = (byte) ((address) & 0xFF);

        System.arraycopy(data, 0, send, 2, data.length);

        for(int i=0;i<5;i++) {
            byte[] recv = TranceiveCommand(CMD_WRITE_IMAGE_FLASH, send, CMD_LONG_TIMEOUT);
            if(CheckResponse(recv))
                return true;
            if(recv==null)
                return false;
        }
        return false;
    }

    private boolean CheckImageFlash() {
        byte[] recv = TranceiveCommand(CMD_CHECK_IMAGE_FLASH, null , CMD_LONG_TIMEOUT);
        return CheckResponse(recv);
    }

    private boolean WriteImageFlashNoAck(int address, byte [] data) {
        byte[] send = new byte[2 + data.length];
        send[0] = (byte) ((address >> 8) & 0xFF);
        send[1] = (byte) ((address) & 0xFF);

        System.arraycopy(data, 0, send, 2, data.length);

        TxCommand(CMD_WRITE_IMAGE_FLASH_NOACK, send, CMD_LONG_TIMEOUT);
        return true;
    }

    private boolean StartWriteFlashAndEPD() {
        byte[] recv = TranceiveCommand(CMD_START_WRITE_FLASH_AND_EPD, null , CMD_LONG_TIMEOUT);
        return CheckResponse(recv);
    }

    private boolean WriteImageToFlashAndEPD(int address, byte [] data) {
        byte[] send = new byte[2 + data.length];
        send[0] = (byte) ((address >> 8) & 0xFF);
        send[1] = (byte) ((address) & 0xFF);
        System.arraycopy(data, 0, send, 2, data.length);
        byte[] recv = TranceiveCommand(CMD_WRITE_IMAGE_FLASH_AND_EPD, send, CMD_LONG_TIMEOUT);
        return CheckResponse(recv);
    }

    private boolean EndWriteFlashAndEPD(byte page) {
        byte [] data = { (byte) 0x01, (byte) 0x28, (byte) 0x00, (byte) 0x80, (byte) page };
        byte[] recv = TranceiveCommand(CMD_END_WRITE_FLASH_AND_EPD, data , CMD_LONG_TIMEOUT);
        return CheckResponse(recv);
    }

    private boolean WriteFlashToEPD(byte page, int width, int height) {

        Log.d(TAG, "WriteFlashToEPD: " + width + "/" + height );
        byte[] data = { (byte)(width>>8), (byte)(width), (byte)(height>>8), (byte)(height), (byte)page };
        byte[] recv = TranceiveCommand(CMD_WRITE_EPD, data , CMD_LONG_TIMEOUT);
        return CheckResponse(recv);
    }

    private boolean WriteFlashToEPD(byte page) {
        byte [] data = { (byte) 0x01, (byte) 0x28, (byte) 0x00, (byte) 0x80, (byte) page };
        byte[] recv = TranceiveCommand(CMD_WRITE_EPD, data , 10000);
        return CheckResponse(recv);
    }

    /////////////////////////////////////////////////////////////////////////////////////
    int testCount = 0;
    public void DrawImage(final EinkImage image, final DrawImageMethod method, final DrawImageCallback cb) throws NFCException {

        if(drawing) {
            throw new NFCException(NFC_EXCEPTION_TYPE_BUSY);
        }

        NFCManager m = NFCManager.getInstance();

        D30Command command = nfcState.command;
        if(command==null)
            throw new NFCException(NFC_EXCEPTION_TYPE_NO_TAG);

        if(cb==null)
            throw new NFCException(NFC_EXCEPTION_TYPE_NO_CALLBACK_ERROR);

        if(nfcState.getNFCState()!= NFCState.NFCSTATE.NFCSTATE_READY) {
            throw new NFCException(NFC_EXCEPTION_TYPE_BUSY);
        }

        final int max_leng = m.getMaxNfcLength();
        Log.e(TAG, "DrawImage: max_leng : "+max_leng );
        byte[] tempData = image.getData();
        final int pages = image.getPages();
        final DrawImageCallback draw_callback = cb;

        final byte lz4flag;
        final int img_high = image.getHeight();
        final int img_width = image.getWidth();

        Log.e(TAG, "DrawImage: img_high ["+img_high+"] img_width ["+img_width+"]" );
        int img_size = (img_high * img_width) / 8;
        int Lz4Size = image.getLz4size();

        if(Lz4Size != 0 && Lz4Size < img_size) {
            Lz4Size = Lz4Size + (Lz4Size % 4);
            tempData = image.getLz4data();
            lz4flag = 1;
        } else {
            tempData = image.getData();
            Lz4Size = tempData.length;
            lz4flag = 0;
        }
        final byte[] data = tempData;
        final int size = Lz4Size;

        final EinkImage reimage = image;
        final DrawImageMethod remethod = method;
        final DrawImageCallback recb = cb;

        if(method==DrawImageMethod.DIMethod_Normal) {
            final Thread thread = new Thread() {
                @Override
                public void run() {
                    // step 1. Erase Flash
                    draw_callback.onProgress(DrawImageState.DIState_Erase, 0);
                    if(!EraseImageFlash(lz4flag)) {
                        Log.v(TAG, "Erase Image Flash Fail");
                        drawing = false;
                        draw_callback.onProgress(DrawImageState.DIState_Error, this);
                        return;
                    }

                    // step 2. send image
                    int pos = 0;
                    // int size = data.length;
                    int chunk_size = max_leng - 5; // NFC Packet need 3 bytes, address 2 bytes



                    long tt = System.currentTimeMillis();

                    while(pos < size) {
                        if(interrupted()) {
                            Log.v(TAG, "Write Image Interrupted");
                            drawing = false;
                            draw_callback.onProgress(DrawImageState.DIState_Error, 0);
                            return;
                        }

                        draw_callback.onProgress(DrawImageState.DIState_SendData, (int)(pos*100/size) );

                        int to;
                        if(pos+chunk_size>size) {
                            to = size;
                        } else {
                            to = pos+chunk_size;
                        }

                        byte [] chunk = Arrays.copyOfRange(data, pos, to);

//                        Log.v(TAG, "======================");
//                        Log.v(TAG, String.format("%d/%d", pos, size));
//                        Log.v(TAG, "======================");

//                        if(!WriteImageFlash(pos, chunk)) {
//                            Log.v(TAG, "Write Image Fail");
//                            drawing = false;
//                            draw_callback.onProgress(DrawImageState.DIState_Error, 0);
//                            return;
//                        }

                        WriteImageFlashNoAck(pos, chunk);
//                        try {
//                            Thread.sleep(100);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
                        pos += chunk_size;
                    }

                    draw_callback.onProgress(DrawImageState.DIState_SendData, (int)(100) );

                    Log.d(TAG, "run: wait to display...");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Log.d(TAG, "run: CheckImageFlash...");

                    if(!CheckImageFlash()) {
                        Log.v(TAG, "Check Image Flash Fail");
                        drawing = false;
                        draw_callback.onProgress(DrawImageState.DIState_Error, 0);
                        return;
                    }

                    Log.v(TAG,"eplased="+String.format("%d ms", System.currentTimeMillis()-tt));

                    // step 2. write epaper
                    draw_callback.onProgress(DrawImageState.DIState_WriteToEPD, (int)(100) );


                    Log.d(TAG, "run: WriteFlashToEPD...");
                    if(WriteFlashToEPD((byte) pages, img_width, img_high)==false) {
//                    if(WriteFlashToEPD((byte) pages)==false) {
                        Log.v(TAG, "Write Flash to EPD fail");
                        drawing = false;
                        draw_callback.onProgress(DrawImageState.DIState_Error, 0);
                        return;
                    }

                    drawing = false;

                    //test
                    /*
                    if (testCount < 100) {
                        Log.v(TAG, "Finish test ["+testCount+"]");
                        drawing = false;
                        try {
                            DrawImage( reimage,  remethod,  recb);
                        } catch (NFCException e) {
                            e.printStackTrace();
                        }
                        testCount++;
                        return;
                    }
                    * * */

                    draw_callback.onProgress(DrawImageState.DIState_Finish, (int)(100) );
                }

            };

            thread.start();

        } else {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    // step 1. Erase Flash
                    draw_callback.onProgress(DrawImageState.DIState_Erase, 0);
                    if(EraseImageFlash(lz4flag)==false) {
                        Log.v(TAG, "Erase Image Flash Fail");
                        drawing = false;
                        draw_callback.onProgress(DrawImageState.DIState_Error, this);
                        return;
                    }

                    // step 2. send start write to flash and epd
                    if(StartWriteFlashAndEPD()==false) {
                        Log.v(TAG, "Start Write Flash and EPD Fail");
                        drawing = false;
                        draw_callback.onProgress(DrawImageState.DIState_Error, this);
                        return;
                    }

                    int pos = 0;
                    int size = data.length;
                    int chunk_size = max_leng - 5; // NFC Packet need 3 bytes, address 2 bytes

                    long tt = System.currentTimeMillis();

                    while(pos < size) {
                        if(interrupted()) {
                            Log.v(TAG, "Write Image Interrupted");
                            drawing = false;
                            draw_callback.onProgress(DrawImageState.DIState_Error, 0);
                            return;
                        }

                        draw_callback.onProgress(DrawImageState.DIState_SendData, (int)(pos*100/size) );

                        int to;
                        if(pos+chunk_size>size) {
                            to = size;
                        } else {
                            to = pos+chunk_size;
                        }

                        byte [] chunk = Arrays.copyOfRange(data, pos, to);

                        Log.v(TAG, String.format("%d/%d", pos, size));

                        if(!WriteImageToFlashAndEPD(pos, chunk)) {
                            Log.v(TAG, "Write Image Fail");
                            drawing = false;
                            draw_callback.onProgress(DrawImageState.DIState_Error, 0);
                            return;
                        }

                        pos += chunk_size;
                    }

                    draw_callback.onProgress(DrawImageState.DIState_SendData, (int)(100) );

                    if(CheckImageFlash()==false) {
                        Log.v(TAG, "Check Image Flash Fail");
                        drawing = false;
                        draw_callback.onProgress(DrawImageState.DIState_Error, 0);
                        return;
                    }

                    Log.v(TAG,"eplased="+String.format("%d ms", System.currentTimeMillis()-tt));

                    // step 2. write epaper
                    draw_callback.onProgress(DrawImageState.DIState_WriteToEPD, (int)(100) );
                    if(EndWriteFlashAndEPD((byte) pages)==false) {
                        Log.v(TAG, "Write Flash to EPD fail");
                        drawing = false;
                        draw_callback.onProgress(DrawImageState.DIState_Error, 0);
                        return;
                    }


                    drawing = false;
                    draw_callback.onProgress(DrawImageState.DIState_Finish, (int)(100) );
                }

            };

            thread.start();
        }
        drawing = true;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String GetVersion() {
        busy = true;

        byte[] recv = TranceiveCommand(CMD_VERSION, null , 5000);

        if (recv!=null && recv.length == 3) {
            busy = false;
            return Integer.toString(recv[0])+"."+Integer.toString(recv[1]);
        }
        if(recv!=null) {
            Log.v(TAG, recv.toString());

            if (recv.length == 4) {
                return String.format("%d.%d.%d", recv[0],recv[1], recv[2]);
            }else if (recv.length == 5) {
                return String.format("%d.%d.%d", recv[0],recv[1], recv[2]);
            }
        }
        busy = false;
        return INVALID_VERSION;
    }

    @Override
    public String GetPlatformName() {
        busy = true;
        byte[] recv = TranceiveCommand(CMD_PLATFORM_NAME, null , 5000);

        if (recv!=null && recv.length == 13) {
            recv[12] = 0;
            busy = false;
            return new String(recv);
        }
        busy = false;
        return "Unkown";
    }

    @Override
    public String GetSN() {
        busy = true;
        byte[] recv = TranceiveCommand(CMD_GET_SN, null , 5000);

        if (recv!=null && recv.length == 13) {
            String s = "";

            for(int i=0;i<recv.length;i++) {
                s += String.format("%02X", (int)(recv[i]&0xFF));
            }
            busy = false;
            return s;
        }
        busy = false;
        return "??";
    }


    public boolean isValid() {
        D30Command command = nfcState.command;
        if (command != null)
            if (command.isValid()) return true;
        return false;
    }

    @Override
    public boolean isBusy() {
        return busy || drawing;
    }

    @Override
    public byte[] getTagID() throws NFCException {
        return NFCManager.getInstance().getTagId();
    }

    @Override
    public void TestAPI() {
        TranceiveCommand(CMD_VERSION, null , 1000);
    }

    @Override
    public boolean CheckEPDStatus()
    {
        return CheckResponse(TranceiveCommand(CMD_GET_EPD_STATUS, null , 1000));
    }

    @Override
    public byte[] RxData() {
        return nfcState.getRx();
    }

    @Override
    public void TxData(byte[] data) {
        nfcState.addEvent(NFCState.FTMEventType.FTMEVENT_TX_MESSAGE, data);
    }

    @Override
    public void onNFCStateChange(NFCState.NFCSTATE new_state) {
        check_epd = (new_state== NFCState.NFCSTATE.NFCSTATE_READY||new_state== NFCState.NFCSTATE.NFCSTATE_BUSY);
    }

    @Override
    public byte GetPinCodeStatus() {
        busy = true;
        byte[] recv = TranceiveCommand(CMD_PINGCODE_STATUS, null , 5000);

        if (recv!=null && recv.length == 2) {
            return recv[0];
        }
        busy = false;

        return 0x10; // locked
    }

    @Override
    public boolean UnlockPinCode(byte[] data) {
        if(data.length!=4) {
            return false;
        }

        busy = true;
        byte[] recv = TranceiveCommand(CMD_PINCODE_UNLOCK, data , 5000);
        busy = false;

        return CheckResponse(recv);
    }

    // Change pin code, data (4 bytes), only on unlocked mode
    @Override
    public boolean SetPinCode(byte[] data)
    {
        if(data.length!=4) {
            return false;
        }

        busy = true;
        byte[] recv = TranceiveCommand(CMD_PINCODE_SET, data , 5000);
        busy = false;

        return CheckResponse(recv);
    }

    // Reset pin code
    // data: 8 bytes (custom CCITT checksum)
    @Override
    public boolean ResetPinCode(byte[] data)
    {
        if(data.length!=8) {
            return false;
        }

        busy = true;
        byte[] recv = TranceiveCommand(CMD_PINCODE_RESET, data , 5000);
        busy = false;

        return CheckResponse(recv);
    }

    @Override
    public byte[] SystemReset() {
        byte[] data = new byte[]{ (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x30 };
        busy = true;
        byte[] recv = TranceiveCommand(CMD_SYSTEM_RESET, data, 2000);
        busy = false;

        return recv;
    }
}
