package com.advantech.nfcsampleapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.advantech.nfclib.NFCException;
import com.advantech.nfclib.NFCManager;
import com.advantech.nfclib.NfcEPDAPI;
import com.advantech.nfclib.utils.CommonUtil;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements NFCManager.NFCTagChangeListener, NfcEPDAPI.DrawImageCallback {
    private Context context;
    private String TAG = "MainActivity";
    // elements
    private ProgressBar progressBar;
    private TextView textProgress;
    private TextView tvType;
    private TextView tvID;
    private ImageView ivLed;
    private TextView tvNotion;
    private ImageView ivImage;
    private Button btnUnload;
    private Button goPINCode;
    private CheckBox cbDithering;
    private CheckBox cbAdjust;
    private EditText etPIN;
    // nfc manager
    private NFCManager manager;
    private boolean isDone = false;
    // nfc adapter
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] writingTagFilters;
    private String[][] mTechListsArray;
    // permission
    private static final int REQUEST_PICK_PICTURE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        // Step 1. initiate an NFC Manager instance
        manager = NFCManager.getInstance();

        // Step 2. register change tag listener and draw image listener
        manager.addChangeTagListener(this);
        manager.setDrawImageListener(this);

        // Step 3. Create an NfcAdapter Instance
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "This device does not support NFC", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Step 4. Prepare a Pending Intent and Intent Filters
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writingTagFilters = new IntentFilter[]{ tagDetected, new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED), new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)};
        mTechListsArray = null;

        // elements
        tvType = (TextView) findViewById(R.id.tvType);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        textProgress = (TextView) findViewById(R.id.textProgress);
        ivLed = (ImageView) findViewById(R.id.ivLed);
        tvNotion = (TextView) findViewById(R.id.tvNotion);
        tvNotion.setText("Waiting for EPD Card");
        ivImage = (ImageView) findViewById(R.id.ivImage);
        cbDithering = (CheckBox) findViewById(R.id.cbDithering);
        cbAdjust = (CheckBox) findViewById(R.id.cbAdjust);
        tvID = (TextView) findViewById(R.id.tvID);
        etPIN = (EditText) findViewById(R.id.etPIN);
        btnUnload = (Button) findViewById(R.id.btnUnload);
        goPINCode = (Button) findViewById(R.id.goPINCode);

        // events
        btnUnload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_PICK_PICTURE);
            }
        });

        etPIN.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence cs, int i, int i1, int i2) {
                String str = cs.toString().trim();
                if (str.length() != 4) {
                    Toast.makeText(context, "PIN Code should have 4 characters!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        goPINCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, PinActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            if (!nfcAdapter.isEnabled()) {
                Toast.makeText(this, "You need to enable NFC first!", Toast.LENGTH_SHORT).show();
                return;
            }
            // Step 5. Start listening NFC devices
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, writingTagFilters, mTechListsArray);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Step x. Stop listening NFC devices
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Step 6. When NFC tag is detected.
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) || NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            // read tech tag
            Tag myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = myTag.getTechList();
            boolean isStopped = true;
            if(techList != null && techList.length > 0) {
                String type = techList[0];
                tvType.setText(type);
                Toast.makeText(this, "NFC type is correct! "+ type, Toast.LENGTH_SHORT).show();
                if ("android.nfc.tech.NfcV".equals(type)) {
                    isStopped = false;
                } else {
                    Toast.makeText(this, "NFC type is wrong. Please try to scan it again.", Toast.LENGTH_LONG).show();
                    manager.resetNFCState(); // reset NFC state, drive MCU to reboot!
                }
            }

            if (!isStopped && !isDone) {
                if (myTag != null) {
                    // Step 7. Set a new tag to NFC Manager
                    manager.setTag(myTag);
                } else {
                    Toast.makeText(context, "No NFC tag detected", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onTagStateChange(NFCManager.NFCTagState state) {
        // Step 8. Tag State Changed Callback function
        try {
            byte[] tagId = manager.getTagId();
            String tagIdString = "";
            for (int i = 0; i < tagId.length; i++) {
                tagIdString += String.format("%02X", tagId[i]);
            }
            String finalTagIdString = tagIdString;
            tvID.post(() -> {
                tvID.setText(finalTagIdString);
            });
        } catch (NFCException e) {
        }

        if(ivLed == null) return;
        ivLed.post(() -> {
            switch (state) {
                case NFC_TAG_STATE_TAG_OFF:
                    ivLed.setImageResource(R.drawable.red_led);
                    tvNotion.setText("EPD State Off");
                    break;
                case NFC_TAG_STATE_TAG_ON:
                    ivLed.setImageResource(R.drawable.yellow_led);
                    tvNotion.setText("EPD State On");
                    break;
                case NFC_TAG_STATE_COMM_ON:
                    // Step 9. When EPD tag communication is ready, you can send the draw image command.
                    ivLed.setImageResource(R.drawable.green_led);
                    tvNotion.setText("EPD Communication On");
                    drawImage();
                    break;
            }
        });
    }

    @Override
    public void onProgress(NfcEPDAPI.DrawImageState state, Object data) {
        // Step 12. Draw Image Results
        switch (state) {
            case DIState_SendData: {
                final int percent = ((int) data == 100) ? 99: (int) data;
                final String text = String.format("%d%%", percent);
                progressBar.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress(percent);
                        textProgress.setText(text);
                    }
                });
            }
            break;

            case DIState_Erase: {
                Log.e(TAG, "onProgress:  DIState_Erase ["+System.currentTimeMillis()+"]");
                progressBar.post(new Runnable() {
                    @Override
                    public void run() {
                        textProgress.setText("ERASE");
                    }
                });
            }
            break;

            case DIState_Error: {
                manager.resetNFCState(); // reset NFC state, drive MCU to reboot!
                progressBar.post(() -> {
                    textProgress.setText("Error!");
                    Toast.makeText(context, "Error during writing NFC tag, try again!", Toast.LENGTH_SHORT).show();

                });
            }
            break;

            case DIState_Finish: {
                Log.e(TAG, "onProgress:  DIState_Finish ["+System.currentTimeMillis()+"]");
                manager.resetNFCState(); // reset NFC state, drive MCU to reboot!
                final int percent = 100;
                progressBar.post(() -> {
                    progressBar.setProgress(percent);
                    textProgress.setText("Success!");
                    showSuccess();
                });
            }
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_PICTURE:
                    Uri uri = intent.getData();

                    Bitmap srcImage = loadFromUri(uri);
                    if (srcImage != null) {
                        ivImage.setImageBitmap(srcImage);
                    } else {
                        Toast.makeText(context, "Upload failure! Please upload it again", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    }

    public Bitmap loadFromUri(Uri photoUri) {
        Bitmap image = null;
        try {
            // check version of Android on device
            if (Build.VERSION.SDK_INT > 27) {
                // on newer versions of Android, use the new decodeBitmap method
                ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), photoUri);
                image = ImageDecoder.decodeBitmap(source);
            } else {
                // support older versions of Android by using getBitmap
                image = MediaStore.Images.Media.getBitmap(context.getContentResolver(), photoUri);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    public void drawImage() {
        boolean isDither = cbDithering.isChecked();
        boolean isAdjust = cbAdjust.isChecked();

        // Step 10. Unlock PIN code
        String str = etPIN.getText().toString().trim();
        char[] chars = str.toCharArray();
        byte[] bytes = CommonUtil.charToByteArray(chars);
        boolean result = manager.unlockPINCode(bytes);
        if (result) {
            Bitmap bm = ((BitmapDrawable) ivImage.getDrawable()).getBitmap();
            Bitmap copyBitmap = bm.copy(Bitmap.Config.ARGB_8888, true);
            try {
                // Step 11. Draw an image
                manager.drawImage(copyBitmap, isDither, isAdjust);
            } catch (Exception e) {
                e.printStackTrace();
            }
            copyBitmap.recycle();
        } else {
            textProgress.setText("Unlock PIN Code failed!");
            Toast.makeText(context, "Unlock PIN Code failed!", Toast.LENGTH_SHORT).show();
        }
    }

    public void showSuccess() {
        isDone = true;
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isDone = false;
                manager.resetNFCState(); // reset NFC state, drive MCU to reboot!
            }
        };

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle("Congratulations!!");
        alertDialog.setMessage("Push Image Success");
        alertDialog.setIcon(R.drawable.ic_baseline_info_24);
        alertDialog.setPositiveButton("Confirm", listener);
        alertDialog.show();
    }
}