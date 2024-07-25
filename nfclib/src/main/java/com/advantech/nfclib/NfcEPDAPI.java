package com.advantech.nfclib;

/**
 * NFC EPD API interface.
 *
 * @author Fabian Chung
 * @version 1.0.0
 */
public interface NfcEPDAPI {

    /**
     * Draw Image State.
     *
     * Here are five draw image states:
     * 1. DIState_Erase: The first stage where it will erase flash on the previous image.
     * 2. DIState_SendData: The second stage where it will send image data to the EPD device.
     * 3. DIState_WriteToEPD: The third stage where it will write flash to the EPD device.
     * 4. DIState_Finish: The final stage when the process is done.
     * 5. DIState_Error: The stage where some problems occurred.
     */
    public enum DrawImageState
    {
        DIState_Erase,
        DIState_SendData,
        DIState_WriteToEPD,
        DIState_Finish,
        DIState_Error
    }

    /**
     * Draw Image Method.
     *
     * Here are two draw image methods:
     * 1. DIMethod_Normal: General usage.
     * 2. DIMethod_Direct_To_EPD
     */
    public enum DrawImageMethod {
        DIMethod_Normal,
        DIMethod_Direct_To_EPD,
    }

    /**
     * PIN Code Status.
     *
     * Here are three PIN code status:
     * 1. PINCode_Unlocked: EPD is unlocked.
     * 2. PINCode_Locked: EPD is locked by a PIN code.
     * 3. PINCode_Blocked: EPD is blocked and cannot be used anymore. If you unlock the EPD failed
     * for five times, it will automatically block the EPD. Please proceed the system reset action.
     * @see NfcEPDAPI#SystemReset()
     * @see NFCManager#resetPINCode()
     */
    public enum PinCodeStatus {
        PINCode_Unlocked,
        PINCode_Locked,
        PINCode_Blocked
    }

    /**
     * Draw Image Callback Interface.
     *
     * This listener contains an callback function which will return draw image progress when EPD
     * is transmitting.
     * @see DrawImageState
     */
    public interface DrawImageCallback {
        /**
         * Draw Image Callback Function.
         *
         * @param state Draw image state
         * @param data Progress percent (from 0 to 100)
         */
        void onProgress(DrawImageState state, Object data);
    }

    /**
     * Get Firmware Version.
     *
     * @return firmware version
     */
    String GetVersion();


    /**
     * Get Firmware Platform Name.
     *
     * @return firmware platform name.
     */
    String GetPlatformName();

    // check NFC connection is valid now

    /**
     * Check NFC Connection.
     *
     * @return NFC connection is valid
     */
    boolean isValid();

    /**
     * Check NFC API Communication.
     *
     * @return NFC API communication is busy
     */
    boolean isBusy();

    /**
     * Get EPD Tag ID.
     *
     * @return EPD Tag ID
     * @throws NFCException
     */
    byte[] getTagID() throws NFCException;

    /**
     * Generate Test Command API.
     *
     */
    void TestAPI();

    /**
     * Check the EPD Status.
     *
     * @return the EPD status is ready
     */
    boolean CheckEPDStatus();

    /**
     * Send the Raw Data Chunk.
     *
     * @param data raw data chunk
     */
    void TxData(byte[] data);

    // Recv Raw data chunk

    /**
     * Read the Raw Data Chunk.
     *
     * @return raw data chunk
     */
    byte[] RxData();

    /**
     * Draw an Eink Image.
     *
     * @param image Eink image object
     * @param method Draw image method
     * @param cb Draw Image callback function
     * @throws NFCException
     */
    void DrawImage(EinkImage image, DrawImageMethod method, DrawImageCallback cb) throws NFCException;

    /**
     * Get the EPD Device Serial Number.
     *
     * @return serial number
     */
    String GetSN();


    /**
     * Get Current PinCode Status.
     *
     * High Nibble: 0 - Unlock, 1 - Locked, 2 - Blocked
     * Low  Nibble: available try_count
     * @return
     */
    byte GetPinCodeStatus();

    /**
     * Unlock by PIN Code.
     *
     * @param data PIN code data (4 bytes)
     * @return Unlock result
     */
    boolean UnlockPinCode(byte[] data);

    // Change pin code, data (4 bytes), only on unlocked mode

    /**
     * Change PIN Code.
     *
     * This action only allowed on the unlocked mode.
     *
     * @param data PIN code data (4 bytes)
     * @return Set PIN code result
     */
    boolean SetPinCode(byte[] data);

    // Reset pin code
    // data: 8 bytes (custom CCITT checksum)

    /**
     * Reset PIN Code.
     *
     * @param data 8 bytes (custom CCITT checksum)
     * @return Reset PIN code result.
     */
    boolean ResetPinCode(byte[] data);

    /**
     * System Reset.
     *
     * This action will reset PIN Code and system.
     *
     * @return System reset result
     */
    byte[] SystemReset();
}
