package com.advantech.nfclib;


import static com.advantech.nfclib.NFCExceptionType.NFC_EXCEPTION_TYPE_NO_CALLBACK_ERROR;

import android.graphics.Bitmap;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import com.advantech.nfclib.api.LeoD30EPDAPI;
import com.advantech.nfclib.api.NFCState;
import com.advantech.nfclib.image.BSCAdjuster;
import com.advantech.nfclib.image.Dithering;
import com.advantech.nfclib.image.RGBTriple;
import com.advantech.nfclib.utils.CommonUtil;
import com.advantech.nfclib.utils.ImageUtil;

/**
 * NFCManager is a singleton class for NFC communication with Advantech EPD devices. This class
 * provides basic functions for detecting NFC cards, display images, unlock PIN code, set PIN code,
 * and reset PIN code. Please git pull the sample project to explore these functions.
 *
 * @author Fabian Chung
 * @version 1.0.0
 */
public class NFCManager {
    static private String TAG = "NFCManager";
    static private boolean commEnable = false;
    private Bitmap currentBitmap = null;

    /**
     * NFC Tag State.
     *
     * Here are three NFC state:
     * 1. NFC_TAG_STATE_TAG_OFF: NFC card lose connection
     * 2. NFC_TAG_STATE_TAG_ON: NFC card is detected
     * 3. NFC_TAG_STATE_COMM_ON: NFC card is ready to communicate
     */
    public enum NFCTagState {
        NFC_TAG_STATE_TAG_OFF(0),
        NFC_TAG_STATE_TAG_ON(1),
        NFC_TAG_STATE_COMM_ON(2);

        private final int value;
        private NFCTagState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    };

    ////////////////////////////////////////////////////////////////////
    /**
     * NFC Tag Change Listener Interface.
     *
     * This listener contains an callback function which will return the NFC tag state when state
     * is changed.
     * @see NFCTagState
     */
    public interface NFCTagChangeListener {
        /**
         * NFC Tag State Change Callback Function.
         *
         * @param event NFC Tag State.
         */
        void onTagStateChange(NFCTagState event);
    }

    private List<NFCTagChangeListener> mChangeTagListeners = new ArrayList<NFCTagChangeListener>();
    private NfcEPDAPI.DrawImageCallback drawImageCallback = null;

    /**
     * Add a NFC Tag Change Listener
     *
     * @param listener NFC Tag Change Listener
     */
    public void addChangeTagListener(NFCTagChangeListener listener) {
        mChangeTagListeners.add(listener);
    }

    /**
     * Set a Draw Image Callback Listener
     *
     * @param drawImageCallback Draw Image Callback
     */
    public void setDrawImageListener(NfcEPDAPI.DrawImageCallback drawImageCallback) {
       this.drawImageCallback = drawImageCallback;
    }

    private void doChangeTabListener(NFCTagState event) {
        for(NFCTagChangeListener l: mChangeTagListeners)
            l.onTagStateChange(event);
    }

    ////////////////////////////////////////////////////////////////////
    private static NFCManager instance;
    private static Tag tag;
    private static NfcV nfcvTag;
    private static byte[] tagId;
    private int maxLeng;
    private NFCTagState currentTagState;

    protected static NFCState nfcState;
    protected static NfcEPDAPI nfcapi;

    private NFCManager() {
        // create FTM state machine
        nfcState = new NFCState();
        nfcState.setName("NFCState");
        nfcState.start();

        currentTagState = NFCTagState.NFC_TAG_STATE_TAG_OFF;
        doChangeTabListener(NFCTagState.NFC_TAG_STATE_TAG_OFF);
    }

    /**
     * Get the NFCManager singleton instance.
     *
     * @return NFCManager
     */
    public static NFCManager getInstance() {
        if(instance == null){
            synchronized(NFCManager.class){
                if(instance == null){
                    instance = new NFCManager();
                }
            }
        }
        return instance;
    }

    /**
     * Set an NFC Tag for NFC Manager.
     *
     * @param tag NFC Tag
     */
    public void setTag(Tag tag) {
        NFCManager.tag = tag;

        if(tag!=null) {
            Log.v(TAG, tag.toString());

            nfcvTag = NfcV.get(tag);
            tagId = tag.getId();
            if(nfcvTag==null) {
                nfcvTag = null;
                tagId = null;
                doChangeTabListener(NFCTagState.NFC_TAG_STATE_TAG_OFF);
                return;
            }
            maxLeng = nfcvTag.getMaxTransceiveLength()-4; // RF packet need 4 bytes

            String s = "";
            for(int i = 0; i< tagId.length; i++) { s += String.format( "%02X ", tagId[i]); }
            Log.v(TAG, s);

            // notify FTM state machine
            nfcState.addEvent(NFCState.FTMEventType.FTMEVENT_TAG_FOUND, nfcvTag);
            doChangeTabListener(NFCTagState.NFC_TAG_STATE_TAG_ON);
        } else {
            nfcvTag = null;
            tagId = null;
            doChangeTabListener(NFCTagState.NFC_TAG_STATE_TAG_OFF);
        }
    }

    /**
     * Set NFC Communication is ready.
     *
     * @param enable Enable or disable the NFC communication.
     */
    public void setCommEnable(boolean enable)
    {
        commEnable = enable;
        if(enable) {
            // notify FTM state machine
            doChangeTabListener(NFCTagState.NFC_TAG_STATE_COMM_ON);
        } else if (nfcvTag != null) {
            doChangeTabListener(NFCTagState.NFC_TAG_STATE_TAG_ON);
        } else {
            doChangeTabListener(NFCTagState.NFC_TAG_STATE_TAG_OFF);
        }
    }

    /**
     * Get NFC Communication.
     *
     * @return NFC communication is enabled.
     */
    public boolean getCommEnable() {
        return commEnable;
    }


    /**
     * Get an NFC API instance.
     *
     * @return NFC API instance
     */
    public NfcEPDAPI getNfcAPI() {
        if(nfcapi == null) {
            nfcapi = new LeoD30EPDAPI(nfcState);
        }
        return nfcapi;
    }

    /**
     * Get NFC Tag ID.
     *
     * @return NFC Tag ID
     * @throws NFCException
     */
    public byte[] getTagId() throws NFCException {
        if (nfcvTag == null) throw new NFCException();
        return tagId;
    }

    /**
     * Get Maximum NFC Length.
     *
     * @return the maximum NFC length
     */
    public int getMaxNfcLength() { return maxLeng; }

    /**
     * Reset NFC State.
     *
     * This action will drive MCU to reboot!
     */
    public void resetNFCState() {
        nfcState.addEvent(NFCState.FTMEventType.FTMEVENT_RESET, 0);
    }

    /**
     * Unlock NFC Card With a PIN code
     *
     * @param bytes PIN code data (4 bytes)
     * @return Unlock result
     */
    public boolean unlockPINCode(byte[] bytes) {
        NfcEPDAPI api = getNfcAPI();
        boolean result = api.UnlockPinCode(bytes);
        Log.e(TAG, "Unlock PIN Code result: " + result);
        return result;
    }

    /**
     * Change the PIN Code.
     *
     * This action only allowed on the unlocked mode.
     *
     * @param bytes new PIN code data (4 bytes)
     * @return Set PIN code result
     */
    public boolean setPINCode(byte[] bytes) {
        NfcEPDAPI api = getNfcAPI();
        boolean result = api.SetPinCode(bytes);
        Log.e(TAG, "Set PIN Code result: " + result);
        return result;
    }

    /**
     * Reset NFC Card PIN Code.
     *
     * This action will reset PIN code to the default value.
     * @return Reset PIN code result
     */
    public boolean resetPINCode() {
        NfcEPDAPI api = getNfcAPI();
        boolean result = false;
        byte[] tag = api.SystemReset();
        String Crc = "";
        String other = "";
        if (tag == null) {
            Crc="??";
        } else {
            if (tag.length == 6) {
                int ack = (int) tag[0];
                result = ack == 1;
                for (int i = 1; i <= 4; i++) {
                    Crc += String.format("%02X", tag[i] & 0xFF);
                }
                other =  String.format("%02X", tag[5] & 0xFF);
            }
            else {
                Crc = "Not support error";
            }

        }
        Log.e(TAG, "System Reset CRC: " + Crc);
        Log.e(TAG, "System Reset Other: " + other);
        return result;
    }

    /**
     * Draw Image on An EPD Card.
     *
     * @param image Source image
     * @param isDither Apply the dithering algorithm or not
     * @param isAdjust Apply the BSC color adjustment or not
     * @throws Exception
     */
    public void drawImage(Bitmap image, boolean isDither, boolean isAdjust) throws Exception {
        int cpflag = 1, pages = 1, lz4packsize = 1024;
        int width, height;
        EinkImage.PanelType type;
        RGBTriple[] palette = Dithering.bw;
        Handler handler = new Handler();
        NfcEPDAPI api = getNfcAPI();
        String epdName = api.GetPlatformName().trim();
        String version = api.GetVersion();
        Log.e(TAG, "GetPlatformName: "+ epdName);
        Log.e(TAG, "GetVersion: "+ version);

        if (drawImageCallback == null) {
            throw new NFCException(NFC_EXCEPTION_TYPE_NO_CALLBACK_ERROR);
        }

        if ("EPD-210--TC2".equals(epdName)) {
            type = EinkImage.PanelType.EPD210;
            width = EinkImage.PanelType.EPD210.getWidth();
            height = EinkImage.PanelType.EPD210.getHeight();
            lz4packsize = 1024;
            if (CommonUtil.isFWSupport(version, "3.0.0")) {
                cpflag = 1;
            }else{
                cpflag = 0;
            }
            pages = 1;
            palette = Dithering.bw;
        } else if ("EPD-302--TC2".equals(epdName)) {
            type = EinkImage.PanelType.EPD302;
            width = EinkImage.PanelType.EPD302.getWidth();
            height = EinkImage.PanelType.EPD302.getHeight();
            lz4packsize = 5120;
            cpflag = 1;
            pages = 1;
            palette = Dithering.bw;
        } else if ("EPD-303--TC2".equals(epdName)) {
            type = EinkImage.PanelType.EPD303;
            width = EinkImage.PanelType.EPD303.getWidth();
            height = EinkImage.PanelType.EPD303.getHeight();
            lz4packsize = 5120;
            cpflag = 1;
            pages = 2;
            palette = Dithering.bwr;
        } else if ("EPD-304--TC2".equals(epdName)) {
            type = EinkImage.PanelType.EPD304;
            width = EinkImage.PanelType.EPD304.getWidth();
            height = EinkImage.PanelType.EPD304.getHeight();
            lz4packsize = 5120;
            cpflag = 1;
            pages = 2;
            palette = Dithering.bwry;
        } else {
            //default is 2.9
            cpflag = 0;
            type = EinkImage.PanelType.EPD210;
            width = EinkImage.PanelType.EPD210.getWidth();
            height = EinkImage.PanelType.EPD210.getHeight();
        }

        // resize an image
        if (currentBitmap != null) {
            currentBitmap.recycle();
            currentBitmap = null;
        }
        currentBitmap = ImageUtil.resize(image, width, height);
        if (isDither) {
            Dithering.applyFloydSteinbergDithering(currentBitmap, palette);
        }
        if (isAdjust) {
            BSCAdjuster.transform(currentBitmap, 35, 15, 25);
        }

        EinkImage currentImage = new EinkImage(width, height, pages, currentBitmap, cpflag, lz4packsize, type);
        Log.d(TAG, "drawImage: currentImage size  " + currentImage.getLz4size());
        Log.d(TAG, "Unlock PIN code" + currentImage.getLz4size());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 註冊 drawImageCallback 的事件 - onProgress()
                try {
                    long sdate = System.currentTimeMillis();
                    api.DrawImage(currentImage, NfcEPDAPI.DrawImageMethod.DIMethod_Normal, drawImageCallback);
                    long edate = System.currentTimeMillis();
                    long range = (edate-sdate);
                    Log.e(TAG, "DrawImage ["+(range)+"ms] " );
                } catch (NFCException e) {
                    e.printStackTrace();
                }
            }
        }, 1000);
    }
}
