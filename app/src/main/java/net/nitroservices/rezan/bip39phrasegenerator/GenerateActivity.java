package net.nitroservices.rezan.bip39phrasegenerator;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;


import static org.bitcoinj.core.Utils.HEX;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.logging.Handler;


public class GenerateActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "GenerateActivity";

    private static final String KEY_XVALUETOTAL = "xValueTotal";
    private static final String KEY_YVALUETOTAL = "yValueTotal";
    private static final String KEY_ZVALUETOTAL = "zValueTotal";


    // private String keySoFar = "";
    private static String keySoFar = "";

    private static boolean doIt = true;

    private static int xValueTotal;
    private static int yValueTotal;
    private static int zValueTotal;

    private int xValueLatest;
    private int yValueLatest;
    private int zValueLatest;

    private TextView textView;
    private TextView shakeDeviceTextView;

    private TextView xTotalTextView;

    private static CountDownTimer shakeDeviceCountDownTimer;
    private static CountDownTimer clearAllThreadsCountDownTimer;


    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    boolean accelerometerPresent;
    private Display display;

    private HandlerThread sensorThread;
    private android.os.Handler sensorHandler;


    private MediaPlayer mPlay;


    // Converts a string of ints to a string of characters
    private String stringOfNumbersToChars(String numbers, int charSize) {
        Log.i(TAG, "stringOfNumbersToChars: numbers is " + numbers);
        String chars = "";
        // -2 prevents going out of bounds since we grab 2 chars at 'pos'
        for (int pos = 0; pos < numbers.length() - 2; pos += charSize) {
            // Log.i(TAG, "stringOfNumbersToChars: pos is " + pos + " numbers length is " + numbers.length());
            Log.i(TAG, "stringOfNumbersToChars Loop");

            try {
                chars += (char) (Integer.parseInt(numbers.substring(pos, pos + charSize)));
            } catch (NumberFormatException ex) // if we get here there was an invalid character when parsing the char
            {
                // just skip it
            } catch (IndexOutOfBoundsException ex) // substring went out of bounds
            {
                if (pos >= numbers.length()) {
                    // start position is at fault, not the end position
                    throw new IndexOutOfBoundsException("Starting position is out of bounds: pos=" + pos + " numbers length=" + numbers.length() + " ||| " + ex.toString());
                } else // pos is less than length
                {
                    // grab starting at pos and to the end of the string
                    chars += (char) (Integer.parseInt(numbers.substring(pos, numbers.length() - pos)));
                }
            }
        }

        return chars;
    }

    private static String makeSecurishRandomNumber() {
        SecureRandom prng;
        String secureRandomNum = null;
        try {
            prng = SecureRandom.getInstance("SHA1PRNG");
            secureRandomNum = new Integer(prng.nextInt()).toString();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return secureRandomNum;
    }

    private static String makeMnemonicPhraseFromHash(String aHash) {

        byte[] theHashByte = HEX.decode(aHash);

        MnemonicCode mnemonicCode;
        List<String> seedPhrase = null;
        String seedPhraseSentence = "";
        String loopWord = null;

        try {
            mnemonicCode = new MnemonicCode();
            try {

                seedPhrase = mnemonicCode.toMnemonic(theHashByte);
                for (int count = 0; count < 24; count++) {
                    loopWord = seedPhrase.get(count) + " ";
                    seedPhraseSentence = seedPhraseSentence + loopWord;

                }
            } catch (MnemonicException.MnemonicLengthException e) {

            }
        } catch (IOException ioe) {

        }


        Log.i(TAG, "seedPhraseSentence: " + seedPhraseSentence);

        return seedPhraseSentence;
    }

    private static String addMoreEntropy(int fromX, int fromY, int fromZ){
        String entropyString = "1";
        int[] listOfPrimes = {77647, 77659, 77681, 77687, 77689, 77699, 77711, 77713, 77719, 77723, 98519, 98533, 98543, 98561, 98563, 98573, 98597, 98621, 98627, 98639};

        int lengthListPrimes = listOfPrimes.length - 1;

        for (int counter = 0; counter < 3; counter++){
            int randX = 0 + (int)(Math.random() * ((lengthListPrimes - 0) + 1));
            int randY = 0 + (int)(Math.random() * ((lengthListPrimes - 0) + 1));
            int randZ = 0 + (int)(Math.random() * ((lengthListPrimes - 0) + 1));

            fromX = fromX * listOfPrimes[randX] % 1000;
            fromY = fromY * listOfPrimes[randY] % 1000;
            fromZ = fromZ * listOfPrimes[randZ] % 1000;

            entropyString = entropyString + (fromX + "") + (fromY + "") + (fromZ + "");
        }
        return entropyString;
    }


    private static String makeRandNum(String previousHash, String inputString) {
        long nanoTime = System.nanoTime();
        String nanoTimeString = Long.toString(nanoTime);
        String hashNanoTimeString = sha256(nanoTimeString);

        Random randomNum = new Random();
        long longRandNum = randomNum.nextLong();
        String randomNumString = Long.toString(longRandNum);
        String hashRandomNumString = sha256(randomNumString);

        String hashInputString = sha256(inputString);
        String hashPreviousHash = sha256(previousHash);

        String bigString = hashPreviousHash + "_" + hashInputString + "_" + hashRandomNumString + "_" + hashNanoTimeString;

        String theHash = sha256(bigString);
        theHash = sha256(theHash);

        String md5Hash = md5(theHash);

        String smallerNumberString = theHash.substring(0, 7);
        int smallerNumber = Integer.parseInt(smallerNumberString, 16);

        int randNumberLoops = (smallerNumber % 11) + 5;

        for (int counter = 0; counter <= randNumberLoops; counter++) {
            md5Hash = md5(sha256(md5Hash));
        }

        theHash = sha256(md5Hash + theHash);

        long nanoTime2 = System.nanoTime();
        String nanoTimeString2 = Long.toString(nanoTime2);
        String hashNanoTimeString2 = sha256(nanoTimeString2);

        theHash = sha256(hashNanoTimeString2 + theHash);
        theHash = sha256(theHash);

        return theHash;
    }

    public static String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String md5(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate);



        startCountDown();

        xTotalTextView = (TextView)findViewById(R.id.xTotalTextView);
        textView = (TextView)findViewById(R.id.textView);

        keySoFar = "BIP39 Phrase Generator - Android App";

        String theKeySoFarInBinary = convertKeySoFarToBinary();
        textView.setText(theKeySoFarInBinary);


        // Restore value from savedInstanceState if not null
        if (savedInstanceState != null) {
            xValueTotal = savedInstanceState.getInt(KEY_XVALUETOTAL);


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    xTotalTextView.setText(Integer.toString(xValueTotal));
                }
            });

        }


        display = getWindowManager().getDefaultDisplay();

        sensorThread = new HandlerThread("Sensor thread", Thread.MAX_PRIORITY);
        sensorThread.start();

        // Blocks until looper is prepared, which is fairly quick
        sensorHandler = new android.os.Handler(sensorThread.getLooper());

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);

        if (sensorList.size() > 0)
        {
            accelerometerPresent = true;
            accelerometerSensor = sensorList.get(0);
        }
        else
        {
            accelerometerPresent = false;
        }







    }

    private void startCountDown() {

        clearAllThreadsCountDownTimer = new CountDownTimer(200, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.i(TAG, "(clearAllThreads) Count Down Timer millisUntilFinished: " + millisUntilFinished);
            }

            @Override
            public void onFinish() {

            }
        }.start();

        shakeDeviceCountDownTimer = new CountDownTimer(2000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.i(TAG, "(shakeDevice) Count Down Timer millisUntilFinished: " + millisUntilFinished);
            }

            @Override
            public void onFinish() {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        shakeDeviceTextView = (TextView)findViewById(R.id.shakeDeviceTextView);
                        shakeDeviceTextView.setVisibility(View.VISIBLE);
                    }
                });

            }
        }.start();
    }


    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        // Log.i(TAG, "onSaveIstanceState");
        Log.i(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(savedInstanceState);


        savedInstanceState.putInt(KEY_XVALUETOTAL, xValueTotal);
        savedInstanceState.putInt(KEY_YVALUETOTAL, yValueTotal);
        savedInstanceState.putInt(KEY_ZVALUETOTAL, zValueTotal);

    }






    /*
    We are using Log to output entries to the logcat windows.
    This helps us see the current status of our Activity.
*/
    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() called");

        keySoFar = sha256(makeSecurishRandomNumber());
        Log.d(TAG, keySoFar);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() called");

        mPlay.stop();

        if (accelerometerPresent) {
            sensorManager.unregisterListener(this);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                sensorHandler.getLooper().quitSafely();
            else
                sensorHandler.getLooper().quit();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() called");


        mPlay = MediaPlayer.create(this, R.raw.mouseclick);


        if (accelerometerPresent) {

            sensorThread = new HandlerThread("Sensor thread", Thread.MAX_PRIORITY);
            sensorThread.start();

            // Blocks until looper is prepared, which is fairly quick
            sensorHandler = new android.os.Handler(sensorThread.getLooper());



            sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
            List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);

            if (sensorList.size() > 0)
            {
                Log.i(TAG, "Got here");

                accelerometerPresent = true;
                accelerometerSensor = sensorList.get(0);
            }
            else
            {
                accelerometerPresent = false;
            }


            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME, sensorHandler);
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");

        mPlay.stop();
        mPlay.release();
        mPlay = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Log.d(TAG, "onSensorChanged() called.");

        // xValueLatest = (int) event.values[0];
        // yValueLatest = (int) event.values[1];
        // zValueLatest = (int) event.values[2];

        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
        {
            return;
        }

        switch (display.getRotation()) {
            case Surface.ROTATION_0:
                xValueLatest = (int)event.values[0];
                yValueLatest = (int)event.values[1];
                break;

            case Surface.ROTATION_90:
                xValueLatest = (int)-event.values[1];
                yValueLatest = (int)event.values[0];
                break;

            case Surface.ROTATION_180:
                xValueLatest = (int)-event.values[0];
                yValueLatest = (int)-event.values[1];
                break;

            case Surface.ROTATION_270:
                xValueLatest = (int)event.values[1];
                yValueLatest = -(int)event.values[0];
                break;
        }
        zValueLatest = (int)event.values[2];



        if ((xValueLatest + yValueLatest + zValueLatest) > 15) {

            mPlay.start();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (shakeDeviceTextView != null) {
                        shakeDeviceTextView.setVisibility(View.INVISIBLE);
                        clearAllThreadsCountDownTimer.cancel();
                        clearAllThreadsCountDownTimer.start();
                        shakeDeviceCountDownTimer.cancel();
                        shakeDeviceCountDownTimer.start();
                    }
                }
            });

            String output = "[x=" + xValueLatest + ", y=" + yValueLatest + ", z=" + zValueLatest + ']';

            Log.d(TAG, "onSensorChanged called.  --> " + output);

            xValueTotal += xValueLatest;
            yValueTotal += yValueLatest;
            zValueTotal += zValueLatest;

            String totalCount = (xValueTotal + "") + (yValueTotal + "") + (zValueTotal + "") + keySoFar;

            String someEntropy = addMoreEntropy(xValueLatest, yValueLatest, zValueLatest);

            String moreInput = totalCount + someEntropy;

            keySoFar = makeRandNum(keySoFar, moreInput);

            String wordsOut = makeMnemonicPhraseFromHash(keySoFar);

            Log.d(TAG, "Jack's Test:");
            Log.d(TAG, String.valueOf(xValueLatest));
            Log.d(TAG, String.valueOf(yValueLatest));
            Log.d(TAG, String.valueOf(zValueLatest));
            Log.d(TAG, totalCount);
            Log.d(TAG, someEntropy);
            Log.d(TAG, keySoFar);


            String theKeySoFarInBinary = convertKeySoFarToBinary();

            final String theKeySoFarInBinaryfinalString = theKeySoFarInBinary;
            final String wordOutfinalString = wordsOut;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    textView = (TextView)findViewById(R.id.textView);
                    textView.setText(theKeySoFarInBinaryfinalString);

                    xTotalTextView.setText(wordOutfinalString);
                }
            });



        }

    }

    @NonNull
    private String convertKeySoFarToBinary() {
        byte[] bytes = keySoFar.getBytes();
        StringBuilder theKeySoFarInBinary = new StringBuilder();

        for (byte b : bytes) {
            int val = b;

            for (int i = 0; i < 8; i++) {
                theKeySoFarInBinary.append((val & 128) == 0 ? 0 : 1);
                val <<= 1;
            }
            theKeySoFarInBinary.append(' ');
        }
        return theKeySoFarInBinary.toString();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "onAccuracyChanged() called. accuracy: " + accuracy);
    }


}
