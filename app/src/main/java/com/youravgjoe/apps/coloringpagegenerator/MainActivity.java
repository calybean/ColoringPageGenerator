package com.youravgjoe.apps.coloringpagegenerator;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final char[] HEX_DIGITS_ARRAY = "0123456789abcdef".toCharArray();

    private static final String BASE_URL_POST = "http://opeapi.ws.pho.to/addtask/?data=";
    private static final String SIGN_DATA = "&sign_data=";
    private static final String KEY = "&key=";
    private static final String APP_ID = "&app_id=";

    public static final String BASE_URL_GET = "http://opeapi.ws.pho.to/getresult?request_id=";

    private static final String XML_PART_1 = "<image_process_call><image_url>";
    private static final String XML_PART_2 = "</image_url><methods_list><method><name>cartoon</name><params>fill_solid_color=1;target_color=(255,255,255);</params></method></methods_list><result_format>png</result_format><result_size>1500</result_size></image_process_call>";

    // todo: clean this up when you decide which one to use

    // testing different methods of photo editing:

    // 1. cartoon + deblurring
    private static final String XML_PART_2_1 = "</image_url><methods_list><method order=\"1\"><name>deblurring</name></method><method order=\"2\"><name>cartoon</name><params>use_anisotropic=TRUE;fill_solid_color=1;target_color=(255,255,255);</params></method></methods_list><result_format>png</result_format><result_size>1500</result_size></image_process_call>";

    // 2. more different cartoon
    private static final String XML_PART_2_2 = "</image_url><methods_list><method><name>cartoon</name><params>use_anisotropic=TRUE;fill_solid_color=1;target_color=(255,255,255);</params></method></methods_list><result_format>png</result_format><result_size>1500</result_size></image_process_call>";

    // 3. adaptive cartoon + cartoon
    private static final String XML_PART_2_3 = "</image_url><methods_list><method order=\"1\"><name>adaptive_cartoon</name></method><method order=\"2\"><name>cartoon</name><params>fill_solid_color=1;target_color=(255,255,255);</params></method></methods_list><result_format>png</result_format><result_size>1500</result_size></image_process_call>";

    // 4. adaptive cartoon + cartoon with anistropic=true
    private static final String XML_PART_2_4 = "</image_url><methods_list><method order=\"1\"><name>adaptive_cartoon</name></method><method order=\"2\"><name>cartoon</name><params>use_anisotropic=TRUE;fill_solid_color=1;target_color=(255,255,255);</params></method></methods_list><result_format>png</result_format><result_size>1500</result_size></image_process_call>";

    // 5. deblurring + adaptive cartoon + cartoon with anistropic=true
    private static final String XML_PART_2_5 = "</image_url><methods_list><method order=\"1\"><name>deblurring</name></method><method order=\"2\"><name>adaptive_cartoon</name></method><method order=\"3\"><name>cartoon</name><params>use_anisotropic=TRUE;fill_solid_color=1;target_color=(255,255,255);</params></method></methods_list><result_format>png</result_format><result_size>1500</result_size></image_process_call>";

    // 6. desaturation + deblurring + adaptive cartoon + cartoon with anistropic=true
    private static final String XML_PART_2_6 = "</image_url><methods_list><method order=\"1\"><name>desaturation</name></method><method order=\"2\"><name>deblurring</name></method><method order=\"3\"><name>adaptive_cartoon</name></method><method order=\"4\"><name>cartoon</name><params>use_anisotropic=TRUE;fill_solid_color=1;target_color=(255,255,255);</params></method></methods_list><result_format>png</result_format><result_size>1500</result_size></image_process_call>";

    // change this one to the one you're testing
    private static final String XML_TO_USE = XML_PART_2_6;

    private int mRetryCount;

    CoordinatorLayout mCoordinatorLayout;
    ImageView mImageView;
    EditText mInput;

    ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        mImageView = (ImageView) findViewById(R.id.image_view);
        mInput = (EditText) findViewById(R.id.input);
        mDialog = new ProgressDialog(this);

        Button convertButton = (Button) findViewById(R.id.convert);
        convertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // hide keyboard
                mInput.clearFocus();
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mImageView.getWindowToken(), 0);

                String input = mInput.getText().toString().trim();

                // check for invalid input
                if (!URLUtil.isValidUrl("http://" + input)) {
                    Toast.makeText(v.getContext(), "URL is invalid", Toast.LENGTH_LONG).show();
                    return;
                }
                // make sure it starts with http or https
                if (!input.startsWith("http://") && !input.startsWith("https://")) {
                    input = "http://" + input;
                }

                String fullXml = XML_PART_1 + input + XML_TO_USE;

                mRetryCount = 0;

                // call task to start the photo conversion process
                new ConvertPhotoTask().execute(fullXml);
            }
        });

        ImageButton clearButton = (ImageButton) findViewById(R.id.clear);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // clear input
                mInput.setText("");
                // set focus to input
                mInput.requestFocus();
                // show keyboard
                InputMethodManager keyboard = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(mInput, 0);
            }
        });
    }

    // AsyncTask #1

    class ConvertPhotoTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            // keep this dialog up until all 3 tasks are complete
            mDialog.setMessage("Converting Image...");
            mDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {

            String xml = params[0];

            try {
                String signData = hmacSha1(xml, getResources().getString(R.string.key));

                String fullUrl = BASE_URL_POST + xml +
                        SIGN_DATA + signData +
                        KEY + getResources().getString(R.string.key) +
                        APP_ID + getResources().getString(R.string.app_id);

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(fullUrl).build();
                Response response;

                response = client.newCall(request).execute();
                return response.body().string();
            } catch (Exception e) {
                e.printStackTrace();
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result == null) {
                mDialog.hide();
                return;
            }

            if (!result.contains("<status>OK</status>")) {
                Log.d(TAG, "Error converting image: " + result);
                Toast.makeText(getApplicationContext(), "Error converting image: " + result, Toast.LENGTH_LONG).show();
                mDialog.hide();
                return;
            }

            String requestId = result.substring(result.indexOf("<request_id>") + 12,result.indexOf("</request_id>")); // add 12 so we don't get tag with value
            // now that we have the request id, call the next task to get converted image url
            new GetConvertedPhotoUrlTask().execute(requestId);
        }
    }

    // AsyncTask #2

    class GetConvertedPhotoUrlTask extends AsyncTask<String, Void, String> {

        String mRequestId;

        @Override
        protected String doInBackground(String... params) {

            mRequestId = params[0];

            try {
                String fullUrl = BASE_URL_GET + mRequestId;

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(fullUrl).build();
                Response response;

                response = client.newCall(request).execute();
                return response.body().string();
            } catch (Exception e) {
                e.printStackTrace();
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result == null) {
                mDialog.hide();
                return;
            }
            if (result.contains("Bad Request")) {
                Toast.makeText(getApplicationContext(), "There was a problem with the URL", Toast.LENGTH_LONG).show();
                mDialog.hide();
                return;
            }
            // if the task is still running, call this same task again, and end this one
            if (result.contains("InProgress")) {
                new GetConvertedPhotoUrlTask().execute(mRequestId);
//                Log.d(TAG, "Task still in progress, trying again...");
                mRetryCount++;

                if (mRetryCount == 30) {
                    mDialog.setMessage("Still working...");
                }

                return;
            }

            String convertedImageUrl = result.substring(result.indexOf("<result_url>") + 12,result.indexOf("</result_url>")); // add 12 so we don't get tag with value
            // now that we have the url of the converted image, call the next task to download it
            new GetConvertedPhotoTask().execute(convertedImageUrl);
        }
    }

    // AsyncTask #3

    class GetConvertedPhotoTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... params) {

            String convertedImageUrl = params[0];

            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(convertedImageUrl).build();
                Response response;
                response = client.newCall(request).execute();

                return (BitmapFactory.decodeStream(response.body().byteStream()));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            if (mDialog.isShowing()) {
                mDialog.dismiss();
            }

            if (bitmap == null) {
                Toast.makeText(getApplicationContext(),
                        "There was an error converting the photo. Please try again.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            mImageView.setImageBitmap(bitmap);

            Log.d(TAG, "getConvertedPhotoTask finished");
            Log.d(TAG, "retry count: " + mRetryCount);
        }
    }

    private static String hmacSha1(String value, String key)
            throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        final String type = "HmacSHA1";
        final SecretKeySpec secret = new SecretKeySpec(key.getBytes(), type);
        final Mac mac = Mac.getInstance(type);
        mac.init(secret);
        final byte[] bytes = mac.doFinal(value.getBytes());
        return bytesToHex(bytes);
    }

    private static String bytesToHex(byte[] bytes) {
        final char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_DIGITS_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_DIGITS_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}