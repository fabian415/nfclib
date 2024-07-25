package com.advantech.nfclib.api;

import android.nfc.tech.NfcV;
import android.os.SystemClock;
import android.util.Log;

import com.advantech.nfclib.EinkImage;
import com.advantech.nfclib.NFCException;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import com.advantech.nfclib.NFCExceptionType;
import com.advantech.nfclib.NFCManager;
import com.advantech.nfclib.NfcEPDAPI;
import com.advantech.nfclib.api.cmd.D30Command;

import static com.advantech.nfclib.api.NFCState.NFCSTATE.NFCSTATE_NONE;
import static com.advantech.nfclib.api.NFCState.NFCSTATE.NFCSTATE_READY;
import static com.advantech.nfclib.api.cmd.D30Command.MB_CTRL_BIT_HOST_CURRENT_MSG;
import static com.advantech.nfclib.api.cmd.D30Command.MB_CTRL_BIT_HOST_MISS_MSG;
import static com.advantech.nfclib.api.cmd.D30Command.MB_CTRL_BIT_HOST_PUT_MSG;
import static com.advantech.nfclib.api.cmd.D30Command.MB_CTRL_BIT_MB_EN;
import static com.advantech.nfclib.api.cmd.D30Command.MB_CTRL_BIT_RF_MISS_MSG;
import static com.advantech.nfclib.api.cmd.D30Command.MB_CTRL_BIT_RF_PUT_MSG;

/**
 * Created by User on 2018/1/2.
 */

public class NFCState extends Thread
{
    private static final boolean LOG_ENABLED = false;
    private static String TAG = "NFCState";

    void ClearTag() {
        NFCManager.getInstance().setTag(null);
    }

    /////////////////////////////////////////////////////////////////////////////
    // NFC State
    interface NFCSTATEChangeCallback {
        void onNFCStateChange(NFCSTATE new_state);
    }
    private NFCSTATEChangeCallback stateChangeCallBack = null;
    public void setStateChangeCallBack(NFCSTATEChangeCallback cb) { stateChangeCallBack = cb; }
    public void removeStateChangeCallBack(NFCSTATEChangeCallback cb) { stateChangeCallBack = null; }

    public enum NFCSTATE {
        NFCSTATE_NONE,
        NFCSTATE_INIT,
        NFCSTATE_TEST,
        NFCSTATE_READY,
        NFCSTATE_BUSY
    }

    private NFCSTATE current_state = NFCSTATE_NONE;

    public NFCSTATE getNFCState() {
        return current_state;
    }

    void SetNFCState(NFCSTATE s) {
        this.current_state = s;
        if(stateChangeCallBack!=null) {
            stateChangeCallBack.onNFCStateChange(s);
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    private void log(String m) {
        if(LOG_ENABLED) {
            Log.v(TAG, m);
        }
    }

    private void logv(String m) {
        Log.v(TAG, m);
    }
    
    /////////////////////////////////////////////////////////////////////////////
    // State Events

    private static final int WAIT_EVENT_COUNT = 3;
    private FTMEventRec current_fsm_event;
    private boolean[] wait_in_queue = new boolean[WAIT_EVENT_COUNT];
    //private boolean wait2_in_queue = false;
    private boolean tx_in_queue = false;
    private int waitIndex;

    public enum FTMEventType {
        FTMEVENT_WAIT,
        FTMEVENT_TAG_FOUND,
        FTMEVENT_RX_MESSAGE,
        FTMEVENT_TX_MESSAGE,
        FTMEVENT_EXCEPTION,
        FTMEVENT_RESET
    };

    public class FTMEventRec {
        FTMEventType    type;
        Object          data;
    }

    private BlockingDeque<FTMEventRec> event_queue = new LinkedBlockingDeque<>(20);

    public void addEvent(FTMEventRec rec) {
        synchronized (event_queue) {
            if(rec.type == FTMEventType.FTMEVENT_WAIT) {
                int index = (int) rec.data;
                if(wait_in_queue[index])
                    return; // already in queue
                wait_in_queue[index] = true;
            }
            if(rec.type == FTMEventType.FTMEVENT_TX_MESSAGE) {
                tx_in_queue = true;
            }
            event_queue.add(rec);
        }
    }

    public void addEvent(FTMEventType type, Object data) {
        if(type==FTMEventType.FTMEVENT_WAIT) {
            int index = (int) data;
            if(wait_in_queue[index])
                return;
            wait_in_queue[index] = true;
        }
        if(type == FTMEventType.FTMEVENT_TX_MESSAGE) {
            tx_in_queue = true;
        }
        FTMEventRec rec = new FTMEventRec();
        rec.type = type;
        rec.data = data;
        event_queue.add(rec);
    }

    public FTMEventRec popEvent() {
        FTMEventRec rec = null;
        try {
            rec = event_queue.take();
            if(rec.type==FTMEventType.FTMEVENT_WAIT) {
                int index = (int) rec.data;
                wait_in_queue[index] = false;
            }
            if(rec.type==FTMEventType.FTMEVENT_TX_MESSAGE)
                tx_in_queue = false;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return rec;
    }

    protected int getWaitIndex() { return waitIndex; }

    /////////////////////////////////////////////////////////////////////////////
    // Tick Generator
    private Timer timer = new Timer(true);

    private class TickTask extends TimerTask {

        private final int index;

        TickTask(int t) { this.index = t; }

        @Override
        public void run() {
            addEvent(FTMEventType.FTMEVENT_WAIT, index);
        }
    }

    private TickTask[] waitTask = new TickTask[3];

    void SetWaitTimer(int index, int ms10) {
        ClearWaitTimer(index);
        waitTask[index] = new TickTask(index);
        timer.schedule(waitTask[index], ms10*10);
        //Log.v(TAG, "purge:"+timer.purge()+" " );
    }

    void ClearWaitTimer(int index) {
        if(waitTask[index]!=null) {
            waitTask[index].cancel();
            waitTask[index] = null;
        }
    }

    void ClearAllWaitTimers() {
        for(int i=0;i<WAIT_EVENT_COUNT;i++) {
            ClearWaitTimer(i);
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    private NFCStateContext _fsm;
    private int last_state;
    protected D30Command command;
    private byte currentMbCtrlDyn;
    private NfcBuffer nfcBuffer;

    public NFCState()
    {
        super();

        setPriority(Thread.MAX_PRIORITY);

        ResetContext();

        // Uncomment to see debug output.
        // _fsm.setDebugFlag(true);

        currentMbCtrlDyn = 0;

        nfcBuffer = new NfcBuffer();
    }

    private void ResetContext() {
        _fsm = new NFCStateContext(this);
        log("==== NFCState reset _fsm");
        this.command = null;
    }

    // implement Thread:run()
    public void run() {
        while(!Thread.interrupted()) {
            current_fsm_event = popEvent();
            switch(current_fsm_event.type) {
                case FTMEVENT_WAIT:
                    waitIndex = (int) current_fsm_event.data;
                    _fsm.Wait(); break;

                case FTMEVENT_TX_MESSAGE:
                    if(current_fsm_event.data != null)
                        nfcBuffer.putWriteBuffer(ByteBuffer.wrap((byte[])current_fsm_event.data));
                    _fsm.TxMessage(); break;

                case FTMEVENT_RX_MESSAGE:
                    _fsm.RxMessage(); break;

                case FTMEVENT_TAG_FOUND:
                    _fsm.TagFound(); break;

                case FTMEVENT_EXCEPTION:
                    _fsm.Exception(); break;

                case FTMEVENT_RESET:
                    _fsm.Reset(); break;

                default:
                    break;
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////
    /// Actions (no-public)

    void T(int state, String msg) {
        logv(String.format("%04d->%04d: %s", last_state, state, msg));
        last_state = state;
    }

    void S(int state, String msg) {
        last_state = state;
        logv(String.format("%04d: %s", state, msg));
    }

    void C(String msg) {
        logv(msg);
    }

    void A(String msg) {
        logv(String.format("%04d: %s", last_state, msg));
    }

    void E(int state, String msg) {
        logv(String.format("%04d->%04d: %s", last_state, state, msg));
    }

//    void SetWaitTimer(int index, int ms) { SetEventTimer(index, ms); }
//
//    void ClearWaitTimer(int index) { }

    private void dumpMBCtrl(byte mbCtrlDyn) {
        String s = "MBCtrlDyn=";

        if((mbCtrlDyn&1)   !=0) s += " MB_EN";
        if((mbCtrlDyn&2)   !=0) s += " HOST_PUT_MSG";
        if((mbCtrlDyn&4)   !=0) s += " RF_PUT_MSG";
        if((mbCtrlDyn&8)   !=0) s += " RFU";
        if((mbCtrlDyn&16)  !=0) s += " HOST_MISS_MSG";
        if((mbCtrlDyn&32)  !=0) s += " RF_MISS_MSG";
        if((mbCtrlDyn&64)  !=0) s += " HOST_CURRENT_MSG";
        if((mbCtrlDyn&128) !=0) s += " RF_CURRENT_MSG";

        logv(s);
    }

    private void dumpEHCtrl(byte ehCtrlDyn) {
        String s = "EHCtrlDyn=";

        if((ehCtrlDyn&1)!=0) s +=  " EH_EN";
        if((ehCtrlDyn&2)!=0) s +=  " EH_ON";
        if((ehCtrlDyn&4)!=0) s +=  " FIELD_ON";
        if((ehCtrlDyn&8)!=0) s +=  " VCC_ON";

        logv(s);
    }

    private void dumpData(String m, byte[] recv) {
        String s = "";
        for(int i=1;i<recv.length;i++) s += String.format("%02X ", recv[i]);
        logv(String.format("%s=%s",m,s));
    }

    void SetupFTM() {
        if(command ==null)
            return;

        try {
            try {
                logv("unlock password");
                command.UnlockRFSecurity();
            } catch(NFCException e) {
                logv("Unlock fail");
            }
    
            byte ehCtrlDyn = command.readDynConfig(D30Command.DYN_ADDR_EH_CTRL);
            dumpEHCtrl(ehCtrlDyn);

            try {
                Thread.sleep(5);
            } catch(Exception e) {
            }            

            boolean USE_DYN_VEH = true;

            if(USE_DYN_VEH) {
                logv("USE_DYN_VEH");

                // Check EH_MODE (Disable)
                byte ehMode = command.readConfiguration(D30Command.CFG_ADDR_EH_MODE);
                logv("eh_mode "+ new Integer(ehMode).toString());
                if(ehMode != 1) {
                    command.writeConfiguration(D30Command.CFG_ADDR_EH_MODE, (byte)1);
                    logv("set EH_MODE=1");
                }

                // Set EH_EN if necessary
                if((ehCtrlDyn & 1) == 0) // no EH_EN
                {
                    logv("write EH_ON=1");
                    command.writeDynConfig(D30Command.DYN_ADDR_EH_CTRL, 1); // EN_ON
                    logv("EH_ON=1");
                }
                
            } else {
                // Set EH_MODE on
                byte ehCtrl = command.readConfiguration(D30Command.CFG_ADDR_EH_MODE);
                if(ehCtrl != 0) {
                    command.writeConfiguration(D30Command.CFG_ADDR_EH_MODE, (byte)0);
                    logv("EH_MODE=0");
                }
            }

            byte mbCtrlDyn = command.readDynConfig(D30Command.DYN_ADDR_MB_CTRL);
            dumpMBCtrl(mbCtrlDyn);

            // test current message
            try {
                // Enable FTM
                command.writeDynConfig(D30Command.DYN_ADDR_MB_CTRL, 0x01);
                try {
                    Thread.sleep(30);
                } catch(Exception e) {

                }

                // Check FTM
                mbCtrlDyn = command.readDynConfig(D30Command.DYN_ADDR_MB_CTRL);
                dumpMBCtrl(mbCtrlDyn);
                if((mbCtrlDyn&(byte)0x1)==0x1) {
                    logv("check FTM enable ok");
                } else {
                    logv("check FTM enable not ok");
                }

                byte len = command.readMessageLength();
                byte [] data = command.readMessage(0, 0);

                logv("msg "+ new Integer(len).toString());
                dumpData("RX", data);

            } catch(NFCException e) {
                Log.v(TAG, "Setup FTM fails");
            }
        } catch (NFCException e) {
            ResetContext();
        }
    }

    void CreateCommander() {
        command = new D30Command((NfcV) current_fsm_event.data);
    }

    public void TestAPI() {
        Log.v(TAG, "TestAPI");
        NfcEPDAPI api = NFCManager.getInstance().getNfcAPI();
        api.TestAPI();
    }

    public void CheckEH() {
        Log.v(TAG, "CheckEH");
        try {
            byte ehCtrlDyn = command.readDynConfig(D30Command.DYN_ADDR_EH_CTRL);
            if((ehCtrlDyn & 1) == 0) {
                // Enable EH_EN
                command.writeDynConfig(D30Command.DYN_ADDR_EH_CTRL, ehCtrlDyn | 0x01);
                try {
                    Thread.sleep(10);
                } catch(Exception e) {

                }
            }
        } catch(NFCException e) {

        }
    }

    void HandleTxMessage() {
        try {
            if(nfcBuffer.getWriteBufferLength()>0) {
                byte[] chunk = nfcBuffer.getDataTransmitted();
                command.writeMessage(chunk);
                dumpData("HTX", chunk);
            }
        } catch(NFCException e) {

        }
    }

    void HandleRxMessage() {
        try {
            int len = command.readMessageLength();
            byte[] recv = command.readMessage(0, len);
            dumpData("HRX", recv);
            nfcBuffer.putReadBuffer(ByteBuffer.wrap(Arrays.copyOfRange(recv, 1, recv.length)));
        } catch(NFCException e) {

        }
    }

    void CheckMBCtrl() {
        try {
            if(command==null)
                return;

            currentMbCtrlDyn = command.readDynConfig(D30Command.DYN_ADDR_MB_CTRL);

            if((currentMbCtrlDyn &MB_CTRL_BIT_MB_EN)==0) {
                // MB not enable
                return;
            }

            //dumpMBCtrl(currentMbCtrlDyn);

            if((currentMbCtrlDyn &(MB_CTRL_BIT_HOST_PUT_MSG))!=0) {
                // receive message
                addEvent(FTMEventType.FTMEVENT_RX_MESSAGE, null);
            }

            if((currentMbCtrlDyn &MB_CTRL_BIT_RF_PUT_MSG)!=0) {
                // RF put the message, but host not got yet
            }

            if((currentMbCtrlDyn &MB_CTRL_BIT_HOST_MISS_MSG)!=0) {
                // RF send the message, but host miss the msg due to timeout
                // maybe should re-send again
            }

            if((currentMbCtrlDyn &MB_CTRL_BIT_RF_MISS_MSG)!=0) {
                // Host send the message, bu rf miss the msg due to timeout
                // do nothing, host should resend again
            }

            // check mailbox is free
            if((currentMbCtrlDyn &(MB_CTRL_BIT_RF_PUT_MSG|MB_CTRL_BIT_HOST_PUT_MSG))==0) {
                // mailbox is free
                // check sending chunk list
                if(nfcBuffer.getWriteBufferLength()>0) {
                    byte[] chunk = nfcBuffer.getDataTransmitted();
                    command.writeMessage(chunk);
                    dumpData("TX", chunk);
                }

            }

        } catch (NFCException e) {
            addEvent(FTMEventType.FTMEVENT_EXCEPTION, null);
        }

    }

    public void setCommEnable(boolean b) {
        NFCManager.getInstance().setCommEnable(b);
    }

    ///////////////////////////////////////////////////////////////////////////////
    /// Helper Functions
    public boolean readyToTx() {
        if(command==null)
            return false;

        if(tx_in_queue)
            return false;

        if(nfcBuffer.getWriteBufferLength()>0)
            return false;

        switch(current_state) {
            case NFCSTATE_NONE:
            case NFCSTATE_INIT:
            case NFCSTATE_BUSY:
                return false;
            default:
                return true;
        }
    }

    public byte[] getRx() {
        if(command==null)
            return null;

        if(nfcBuffer.getReadBufferLength()==0)
            return null;

        return nfcBuffer.getDataReceived();
    }

    public byte[] buildNFCPacket(byte cmd, byte [] data) throws NFCException {
        NFCManager m = NFCManager.getInstance();

        int max_leng = m.getMaxNfcLength() - 3; // NFC Packet need 3 bytes
        int len;
        if(data==null) len = 0; else len = data.length;

        if(len > max_leng)
            throw new NFCException(NFCExceptionType.NFC_EXCEPTION_TYPE_SIZE);

        int chk;

        byte [] ret = new byte[len+3];
        ret[0] = (byte) cmd;
        ret[1] = (byte) len;
        chk = (ret[0]&0xFF)+(ret[1]&0xFF);
        for(int i=0;i<len;i++) { ret[i+2] = data[i]; chk += (data[i]&0xff); }
        ret[len+2] = (byte)(0x100-chk&0xFF);

        return ret;
    }

    ///////////////////////////////////////////////////////////////////////////////
    /// Public Functions for API

}
