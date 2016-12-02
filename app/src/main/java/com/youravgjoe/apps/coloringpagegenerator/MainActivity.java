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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
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

    private XmlPullParserFactory mXmlFactoryObject;
    private XmlPullParser mParser;

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

        try {
            mXmlFactoryObject = XmlPullParserFactory.newInstance();
            mParser = mXmlFactoryObject.newPullParser();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

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
                if (mInput.getText().toString().isEmpty()) {
                    return;
                }

                String input = mInput.getText().toString().trim();
                if (URLUtil.isValidUrl(input)) {
                    String fullXml = XML_PART_1 + input + XML_PART_2;

                    // call task to start the photo conversion process
                    convertPhotoTask.execute(fullXml);
                }
            }
        });
    }

    // AsyncTask #1

    AsyncTask<String, Void, String> convertPhotoTask = new AsyncTask<String, Void, String>() {
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

            Log.d(TAG, result);
            Log.d(TAG, "convertPhotoTask finished");

            InputStream in = new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8));

            try {
                mParser.setInput(in, null);

//                String requestId = parseXml(result, true); // yes, we need the request id

                String requestId = result.substring(result.indexOf("<request_id>") + 12,result.indexOf("</request_id>")); // add 12 so we don't get tag with value

                /*
                <image_process_response>
                    <request_id>010afc13-6bba-44dd-b278-4f3bd1e41946</request_id>
                    <status>OK</status>
                    <description />
                    <err_code>0</err_code>
                </image_process_response>
                */

                // now that we have the request id, call the next task to get converted image url
                getConvertedPhotoUrlTask.execute(requestId);

            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
        }
    };

    // AsyncTask #2

    AsyncTask<String, Void, String> getConvertedPhotoUrlTask = new AsyncTask<String, Void, String>() {
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
            Log.d(TAG, "getConvertedPhotoUrlTask finished");

            /*
            <image_process_response>
                <request_id>010afc13-6bba-44dd-b278-4f3bd1e41946</request_id>
                <status>OK</status>
                <result_url>http://worker-images.ws.pho.to/i1/3BCB160A-691A-458B-9161-67AFA8A9EAA0.png</result_url>
                <result_url_alt>http://worker-images.ws.pho.to.s3.amazonaws.com/i1/3BCB160A-691A-458B-9161-67AFA8A9EAA0.png</result_url_alt>
                <nowm_image_url>http://worker-images.ws.pho.to/i1/3BCB160A-691A-458B-9161-67AFA8A9EAA0.png</nowm_image_url>
            </image_process_response>
            */

//            String convertedImageUrl = parseXml(result, false); // no, we don't need the request id

            String convertedImageUrl = result.substring(result.indexOf("<result_url>") + 12,result.indexOf("</result_url>")); // add 12 so we don't get tag with value

            // now that we have the url of the converted image, call the next task to download it
            getConvertedPhotoTask.execute(convertedImageUrl);
        }
    };

    // AsyncTask #3

    AsyncTask<String, Void, Bitmap> getConvertedPhotoTask = new AsyncTask<String, Void, Bitmap>() {
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

            mImageView.setImageBitmap(bitmap);

            Log.d(TAG, "getConvertedPhotoTask finished");
        }
    };

//    private String parseXml(String xml, boolean needRequestId) {
//        int event;
//        try {
//            event = mParser.getEventType();
//
//            while (event != XmlPullParser.END_DOCUMENT)  {
//                String name = mParser.getName();
//                switch (event) {
//                    case XmlPullParser.START_TAG:
//                        break;
//                    case XmlPullParser.END_TAG:
//                        if (needRequestId && name.equals("request_id")){
//                            String requestId = mParser.getAttributeValue(null, "value");
//                            Log.d(TAG, "parseXml " + requestId);
//                            return requestId;
//                        }
//                        if (name.equals("result_url")) {
//                            String resultUrl = mParser.getAttributeValue(null, "value");
//                            Log.d(TAG, "parseXml " + resultUrl);
//                            return resultUrl;
//                        }
//                        break;
//                }
//                event = mParser.next();
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

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