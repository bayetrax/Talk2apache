package com.intel.voice.talk2apache;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.reconinstruments.os.HUDOS;
import com.reconinstruments.os.speech.HUDRecognizerIntent;
import com.reconinstruments.os.wakeonvoice.HUDWakeOnVoiceListener;
import com.reconinstruments.os.wakeonvoice.HUDWakeOnVoiceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RecognitionListener, HUDWakeOnVoiceListener {
    private final static String TAG = "Voice";

    // to identify the permission request
    private static final int PERMISSIONS_REQUEST = 1;

    private static final int VOLUME_UPDATE = 0;
    private static final int AUTO_STOP = 1;

    private RadioGroup radioGroupMode;
    private RadioGroup radioGroupResource;

    private SpeechRecognizer recognizer;

    private Button startBtn = null;
    private Button stopBtn = null;
    private TextView resultText = null;
    private TextView partialResultText = null;
    private CheckBox partialResultCheckBox = null;
    private CheckBox recordCheckBox = null;
    private EditText maxResult = null;
    private EditText slienceLength = null;
    private EditText language = null;
    private ProgressBar mProgress = null;

    private static int mRecordSpeechCounter = 0;

    private Handler mStopHandler = new Handler();
    private Runnable mTask;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == VOLUME_UPDATE) {
                mProgress.setProgress(Math.round((float) msg.obj));
            } else if (msg.what == AUTO_STOP) {
                Log.i(TAG, "Auto Stopping the current recognition");
                recognizer.stopListening();
                startBtn.setEnabled(true);
                stopBtn.setEnabled(false);
            } else {
                Log.d(TAG, "ignored msg");
            }
        }
    };

    //WOVS
//    private final static String TAG = "WakeOnVoiceTest";

    private TextView display = null;
    private CheckBox autoRearm = null;


    private HUDWakeOnVoiceManager mHUDWakeOnVoiceManager;

    private final Handler mHandlerWOV = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            HUDWakeOnVoiceManager.WoVEvent event = HUDWakeOnVoiceManager.WoVEvent.values()[msg.what];
            HUDWakeOnVoiceManager.WoVError code = HUDWakeOnVoiceManager.WoVError.values()[(int) msg.obj];

            if (event == HUDWakeOnVoiceManager.WoVEvent.EVENT_DETECTED) {
                Toast.makeText(getApplicationContext(), "Hotword Detect", Toast.LENGTH_SHORT).show();


                if (isAutoRearm()) {
                    mHUDWakeOnVoiceManager.reArmWoV();
                }

                /*
                Starting speech when Ok Recon detected.
                 */
                startVoice(null);

            } else {
                StringBuilder eventString = new StringBuilder();
                switch (event) {
                    case EVENT_ERROR:
                        eventString = eventString.append("wov error, ");
                        break;
                    case EVENT_DISABLED:
                        eventString = eventString.append("WoV Disabled ");
                        break;
                    case EVENT_ENABLED:
                        eventString = eventString.append("WoV Enabled ");
                        break;
                    default:
                        break;
                }
                switch (code) {
                    case ERROR_UNSUPPORTED:
                        eventString = eventString.append("  WOV Unsupported");
                        break;
                    case ERROR_REMOTE_CONNECTION:
                        eventString = eventString.append("  WOV Service Error");
                        break;
                    case ERROR_DETECTION:
                        eventString = eventString.append("  WOV Detection");
                        break;
                    case ERROR_ENROLLMENT:
                        eventString = eventString.append("  WOV KP Enrollment");
                        break;
                    case ERROR_RECOGNITION:
                        eventString = eventString.append("  WOV Start/Stop");
                        break;
                    case ERROR_NONE:
                        break;
                    default:
                        Log.e(TAG, "unknown event and code");
                        eventString = null;
                        break;
                }
                if (eventString != null) {
                    display.setText(eventString.toString());
                }
            }
        }
    };
    //WOVE


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if ((checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) ||
                (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)) {
            this.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST);
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(this);

        recognizer.setRecognitionListener(this);

        radioGroupMode = (RadioGroup) findViewById(R.id.radiogroup_mode);
        radioGroupResource = (RadioGroup) findViewById(R.id.radiogroup_resource);

        startBtn = (Button) findViewById(R.id.btn_start);
        startBtn.setEnabled(true);
        stopBtn = (Button) findViewById(R.id.btn_stop);
        stopBtn.setEnabled(false);

        resultText = (TextView) findViewById(R.id.textView_val1);
        resultText.setText("");
        partialResultText = (TextView) findViewById(R.id.textView_val2);
        partialResultText.setText("");

        maxResult = (EditText) findViewById(R.id.editText);
        maxResult.setText("1");

        partialResultCheckBox = (CheckBox) findViewById(R.id.checkBox);
        recordCheckBox = (CheckBox) findViewById(R.id.checkBox2);

        mProgress = (ProgressBar) findViewById(R.id.progressBar);

        slienceLength = (EditText) findViewById(R.id.editText2);
        slienceLength.setText("250");

        language = (EditText) findViewById(R.id.editText3);
        language.setText("en_US");

        /////////////WOVS
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mHUDWakeOnVoiceManager = (HUDWakeOnVoiceManager) HUDOS.getHUDService(HUDOS.HUD_WAKE_ON_VOICE_SERVICE);
        mHUDWakeOnVoiceManager.register(this);

        autoRearm = (CheckBox) findViewById(R.id.checkBox);

        display = (TextView) findViewById(R.id.textView_stateresult);

        if (mHUDWakeOnVoiceManager.isWoVEnabled()) {
            display.setText("WoV Enabled");
        } else {
            display.setText("WoV Disabled");
        }
        /////////////WOVE
    }

    @Override
    protected void onDestroy() {
        recognizer.destroy();
        super.onDestroy();
        //WOVS

        mHUDWakeOnVoiceManager.unregister(this);

        //WOVE
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: Permissions Granted");
                } else {
                    showToastMessage("Insufficient Permissions");
                    Log.d(TAG, "onRequestPermissionsResult: Insufficient Permissions");
                    finish();
                }
                return;
            }
        }
    }


    public void showToastMessage(String error) {
        String errorMessage = "Error ID: " + error;
        Toast toast = Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG);
        toast.show();
    }

    public void startVoice(View view) {

        mRecordSpeechCounter += 1;

        Log.d(TAG, "Calling start detection: " + mRecordSpeechCounter);

        int selectedIdMode = radioGroupMode.getCheckedRadioButtonId();

        int selectedIdResource = radioGroupResource.getCheckedRadioButtonId();

        RadioButton btnMode = (RadioButton) findViewById(selectedIdMode);

        RadioButton btnResource = (RadioButton) findViewById(selectedIdResource);

        Log.d(TAG, "Radio Button selected to mode: " + btnMode.getText().toString());
        Log.d(TAG, "Radio Button selected to source: " + btnResource.getText().toString());

        Intent recognitionIntent = new Intent(HUDRecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        if ("Speech".equals(btnMode.getText().toString())) {
            recognitionIntent.putExtra(HUDRecognizerIntent.EXTRA_LANGUAGE_MODEL, HUDRecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        } else if ("Command".equals(btnMode.getText().toString())) {
            recognitionIntent.putExtra(HUDRecognizerIntent.EXTRA_LANGUAGE_MODEL, HUDRecognizerIntent.LANGUAGE_MODEL_COMMAND_FORM);
        }

        recognitionIntent.removeExtra(HUDRecognizerIntent.EXTRA_LANGUAGE_RESOURCE_DIR);
        recognitionIntent.removeExtra(HUDRecognizerIntent.EXTRA_LANUGAGE_CONFIGURATION_FILE);

        if ("User_Provide".equals(btnResource.getText().toString())) {
            Log.d(TAG, "Specifying the resource file and configuration file");
            // In order to use this, you need to push the Assets file to the device ("/data") directory.
            // The RH-Command resource
            //          English (en-US): added new command "SYSTEM TEST" / "TEST TEST" / "SHOW TEST"
            // command: adb root
            //          adb shell mkdir /data/RH
            //          adb push <root>/vendor/intel/PRIVATE/reconos_platform/tools/HUDVoiceRecognitionTest/RH-Resources/RH-Command /data/RH
            //
            // The RH-Speech resource is for speech recognition with Franch (fr-FR) and Spanish (es-ES)
            // command: adb root
            //          adb shell mkdir /data/RH
            //          adb push <root>/vendor/intel/PRIVATE/reconos_platform/tools/HUDVoiceRecognitionTest/RH-Resources/RH-Speech /data/RH
            //
            recognitionIntent.putExtra(HUDRecognizerIntent.EXTRA_LANGUAGE_RESOURCE_DIR, "/data/RH");
            // make sure the name of the configuration file matches with the one in the /data/RH folder !!!
            recognitionIntent.putExtra(HUDRecognizerIntent.EXTRA_LANUGAGE_CONFIGURATION_FILE, "/data/RH/command_asr_main_stream.txt");
        } else if ("Default".equals(btnResource.getText().toString())) {
            Log.d(TAG, "Using default resource files and configuration");
        }

        if (partialResultCheckBox.isChecked()) {
            recognitionIntent.putExtra(HUDRecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        } else {
            recognitionIntent.putExtra(HUDRecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        }

        // change RMS update cycle to the provided values
        //recognitionIntent.putExtra(HUDRecognizerIntent.EXTRA_RMS_UPDATE_CYCLE_MILLIS, 100);
        String maxNumberResults = maxResult.getText().toString();

        if(maxNumberResults == null || maxNumberResults.isEmpty()) {
            maxResult.setText("1");
        } else {
            try {
                Integer.parseInt(maxNumberResults);
                recognitionIntent.putExtra(HUDRecognizerIntent.EXTRA_MAX_RESULTS, Integer.valueOf(maxNumberResults));
            } catch (Exception e) {
                Log.d(TAG, "Max Number of Result Not an integer, using default (1)");
            }
        }

        String inputCompleteSilenceLength = slienceLength.getText().toString();
        if(inputCompleteSilenceLength == null || inputCompleteSilenceLength.isEmpty()) {
            slienceLength.setText("");
        } else {
            try {
                Integer.parseInt(inputCompleteSilenceLength);
                recognitionIntent.putExtra(HUDRecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, Integer.valueOf(inputCompleteSilenceLength));
            } catch (Exception e) {
                Log.d(TAG, "Trailing silence for speech input completion is Not an integer, using default (250)");
            }
        }

        String languageString = language.getText().toString();

        if (languageString != null && !languageString.isEmpty()) {
            recognitionIntent.putExtra(HUDRecognizerIntent.EXTRA_LANGUAGE, languageString);
        }

        Bundle intentBundle = recognitionIntent.getExtras();
        for (String key: intentBundle.keySet()) {
            Log.i(TAG, "key: " + key + " = " + intentBundle.get(key).toString());
        }

        recognizer.startListening(recognitionIntent);

        resultText.setText("");
        partialResultText.setText("");
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);

        // following is an auto stop testing mechanism
        mTask = new Runnable() {
            public void run() {
                Message msg = mHandler.obtainMessage(AUTO_STOP, 0);
                mHandler.sendMessage(msg);
            }
        };
        //mStopHandler.postDelayed(mTask, 5000);

        return;
    }

    public void stopVoice(View view) {

        recognizer.stopListening();

        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);

        return;
    }

    public void cancelVoice(View view) {

        recognizer.cancel();

        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);

        return;
    }

    public void checkService(View view) {

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Voice Recognition service is available", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Voice Recognition service is NOT available", Toast.LENGTH_SHORT).show();
        }
        List activities = getPackageManager().queryIntentActivities(new Intent(HUDRecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        Log.e(TAG, "app to handle activities: " + activities.size());
    }

    private void showError(int error) {
        Toast.makeText(this, "Error (" + error + ")", Toast.LENGTH_SHORT).show();
    }

    private void showResult (String result) {
        resultText.setText(result);
    }

    private void showPartialResult (String result) {
        partialResultText.setText(result);
    }

    private void writeRecordedSpeech (byte[] samples) {

        if (!recordCheckBox.isChecked()) {
            Log.d(TAG, "Not Recording");
            return;
        }

        if (samples != null) {
            try {
                String outputDirectory = Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/RH-Speech/";

                File dirs = new File(outputDirectory);
                if (!dirs.exists()) {
                    dirs.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(outputDirectory + "Recording-" + mRecordSpeechCounter + ".wav", false);
                writeBytes(fos, "RIFF"); // RIFF tag
                writeInt(fos, samples.length + 36); // RIFF data
                // length
                writeBytes(fos, "WAVE"); // WAVE tag
                writeBytes(fos, "fmt "); // "fmt " tag
                writeInt(fos, 16); // fmt data length
                writeShort(fos, 1); // data format 1 = PCM
                writeShort(fos, 1); // number of channels
                writeInt(fos, 16000); // sampling frequency
                writeInt(fos, 32000); // bytes per second
                writeShort(fos, 2); // block align
                writeShort(fos, 16); // bits per sample
                writeBytes(fos, "data"); // data tag
                writeInt(fos, samples.length); // data length
                fos.write(samples);
                fos.close();
                Toast.makeText(this, "Successfully written: Recording-" + mRecordSpeechCounter + ".wav to sdcard", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.d(TAG, e.toString());
                Toast.makeText(this, "Failed to write: Recording-" + mRecordSpeechCounter + ".wav", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Sample data is empty", Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, "done writing files");
    }

    private void writeBytes(OutputStream os, String value) throws IOException {
        os.write(value.getBytes());
    }

    private void writeInt(OutputStream os, int value) throws IOException {
        os.write((value) & 0xff);
        os.write((value >> 8) & 0xff);
        os.write((value >> 16) & 0xff);
        os.write((value >> 24) & 0xff);
    }

    private void writeShort(OutputStream os, int value) throws IOException {
        os.write((value) & 0xff);
        os.write((value >> 8) & 0xff);
    }

//    private RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "onReadyForSpeech is called ");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech called ");
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            Message msg = mHandler.obtainMessage(VOLUME_UPDATE, rmsdB);
            mHandler.sendMessage(msg);
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            writeRecordedSpeech(buffer);
        }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech called ");
        }

        @Override
        public void onError(int error) {
            Log.e(TAG, "on Error is called with error: " + error);
            showError(error);
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
        }

        @Override
        public void onResults(Bundle results) {
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);

            if (results == null) {
                Log.d(TAG, "got nothing");
                return;
            }

            ArrayList<String> resultlist = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            float [] conflist = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);

            for (int i = 0; i < resultlist.size(); i++) {
                Log.d(TAG, "results: " + resultlist.get(i));
                Log.d(TAG, "confidence: " + conflist[i]);
            }
            String resultString = resultlist.get(0);
            showResult(resultString);
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> resultlist = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            float [] conflist = partialResults.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);

            for (int i = 0; i < resultlist.size(); i++) {
                Log.d(TAG, "partial results: " + resultlist.get(i));
                Log.d(TAG, "partial results confidence: " + conflist[i]);
            }

            String resultString = resultlist.get(0);
            showPartialResult(resultString);
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            // Ignored
        }
//    };

    //WOVS

    @Override
    public void onWoVEvent(HUDWakeOnVoiceManager.WoVEvent event, HUDWakeOnVoiceManager.WoVError code) {
        mHandlerWOV.sendMessage(mHandler.obtainMessage(event.ordinal(), code.ordinal()));
    }

    public void enableWakeOnVoice(View view){
        Log.d(TAG, "Enabling WoV");
        mHUDWakeOnVoiceManager.setWoVEnabled(true);
        // used for sanity test
        /*
        mShowVolumeTask = new Runnable() {
            public void run() {
                boolean enabled = mRandom.nextBoolean();
                if (enabled) {
                    Log.e(TAG, "------------------------- enabling --------------");
                    mHUDWakeOnVoiceManager.setWoVEnabled(true);
                } else {
                    Log.e(TAG, "------------------------- disabling --------------");
                    mHUDWakeOnVoiceManager.setWoVEnabled(false);
                }
                mVolumeHandler.postDelayed(this, 500);
            }
        };
        mVolumeHandler.postDelayed(mShowVolumeTask, 500);
        */
        return;
    }

    public void disableWakeOnVoice(View view){
        Log.d(TAG, "Disabling WoV");
        mHUDWakeOnVoiceManager.setWoVEnabled(false);

        // used for sanity test
        /*
        mVolumeHandler.removeCallbacks(mShowVolumeTask);
         */

        return;
    }

    public void armWakeOnVoice(View view){
        Log.d(TAG, "re-arm WoV");
        mHUDWakeOnVoiceManager.reArmWoV();
        return;
    }

    private boolean isAutoRearm() {
        return autoRearm.isChecked();
    }

    public void getState(View view){
        Log.d(TAG, "Get State");
        HUDWakeOnVoiceManager.WoVState state = mHUDWakeOnVoiceManager.getWoVState();
        String stateString;
        switch (state) {
            case STATE_DISABLED:
                stateString = "WoV Disabled";
                break;
            case STATE_DISABLING:
                stateString = "WoV Disabling";
                break;
            case STATE_ENABLED:
                stateString = "WoV Enabled";
                break;
            case STATE_ENABLING:
                stateString = "WoV Enabling";
                break;
            case STATE_UNKNOWN:
                stateString = "WoV Unknown";
                break;
            default:
                stateString = "default?";
                break;
        }
        Toast.makeText(getApplicationContext(), stateString, Toast.LENGTH_SHORT).show();

        return;
    }


    //WOVE

}
