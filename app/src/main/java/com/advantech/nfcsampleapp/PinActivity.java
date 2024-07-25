package com.advantech.nfcsampleapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.advantech.nfclib.NFCException;
import com.advantech.nfclib.NFCManager;
import com.advantech.nfclib.utils.CommonUtil;


public class PinActivity extends AppCompatActivity implements NFCManager.NFCTagChangeListener {
    private Context context;
    // elements
    private TextView tvType;
    private TextView tvID;
    private TextView tvNotion;
    private ImageView ivLed;
    private Button btnSetPIN;
    private Button btnReset;
    private Button goBack;
    private EditText etOldPIN;
    private EditText etNewPIN;
    // nfc manager
    private NFCManager manager;
    private boolean isDone = false;
    // nfc adapter
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] writingTagFilters;
    private String[][] mTechListsArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        context = this;

        // Step 1. initiate an NFC Manager instance
        manager = NFCManager.getInstance();
        // Step 2. register onTagStateChange events
        manager.addChangeTagListener(this);
        // Step 3. new a NfcAdapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "This device does not support NFC", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Step 4. prepare an pending intent
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writingTagFilters = new IntentFilter[]{ tagDetected, new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED), new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)};
        mTechListsArray = null;

        // elements
        tvType = (TextView) findViewById(R.id.tvType);
        tvID = (TextView) findViewById(R.id.tvID);
        tvNotion = (TextView) findViewById(R.id.tvNotion);
        tvNotion.setText("Waiting for EPD Card");
        etOldPIN = (EditText) findViewById(R.id.etOldPIN);
        etNewPIN = (EditText) findViewById(R.id.etNewPIN);
        btnSetPIN = (Button) findViewById(R.id.btnSetPIN);
        btnReset = (Button) findViewById(R.id.btnReset);
        goBack = (Button) findViewById(R.id.goBack);
        ivLed = (ImageView) findViewById(R.id.ivLed);

        // events
        btnSetPIN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Step 1. Unlock PIN code
                String str = etOldPIN.getText().toString().trim();
                char[] chars = str.toCharArray();
                byte[] bytes = CommonUtil.charToByteArray(chars);
                boolean result = manager.unlockPINCode(bytes);
                if (result) {
                    // Step 2. Set PIN code
                    str = etNewPIN.getText().toString().trim();
                    chars = str.toCharArray();
                    bytes = CommonUtil.charToByteArray(chars);
                    result = manager.setPINCode(bytes);
                    Toast.makeText(context, "Set PIN code: " + result, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Unlock PIN code failed. ", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Reset PIN code
                boolean result = manager.resetPINCode();
                Toast.makeText(context, "Reset PIN code: " + result, Toast.LENGTH_SHORT).show();
            }
        });

        goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                Intent intent = new Intent();
                intent.setClass(PinActivity.this, MainActivity.class);
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
            // Step 5. start listening NFC devices
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, writingTagFilters, mTechListsArray);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Step x. stop listening NFC devices
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Step 6. NFC device is detected.
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
        // Step 8. Tag State Changed.
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
                    // Step 9. After EPD tag communication is on, we can send an draw image command.
                    ivLed.setImageResource(R.drawable.green_led);
                    tvNotion.setText("EPD Communication On");
                    break;
            }
        });
    }
}