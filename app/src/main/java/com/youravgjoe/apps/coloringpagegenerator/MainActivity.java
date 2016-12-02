package com.youravgjoe.apps.coloringpagegenerator;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
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

        try {
            String fullXml = XML_PART_1 + mInput.getText().toString() + XML_PART_2;
            String sha1 = hmacSha1(fullXml, getResources().getString(R.string.key));
            Log.d("XML hmacSha1", sha1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Button mConvertButton = (Button) findViewById(R.id.convert);
        mConvertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

                String fullXml = XML_PART_1 + input + XML_PART_2;

                // call task to start the photo conversion process
                new ConvertPhotoTask().execute(fullXml);
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
                return;
            }

            String requestId = result.substring(result.indexOf("<request_id>") + 12,result.indexOf("</request_id>")); // add 12 so we don't get tag with value
            // now that we have the request id, call the next task to get converted image url
            new GetConvertedPhotoUrlTask().execute(requestId);
        }
    }

    // AsyncTask #2

    class GetConvertedPhotoUrlTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {

            String requestId = params[0];

            try {
                String fullUrl = BASE_URL_GET + requestId;

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

            Log.d(TAG, result);

            if (result == null) {
                return;
            }

            if (result.contains("Bad Request")) {
                Toast.makeText(getApplicationContext(), "There was a problem with the URL", Toast.LENGTH_LONG).show();
                mDialog.hide();
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

            // todo: hide keyboard

            Log.d(TAG, "getConvertedPhotoTask finished");
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