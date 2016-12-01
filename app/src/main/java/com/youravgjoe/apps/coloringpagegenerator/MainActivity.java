package com.youravgjoe.apps.coloringpagegenerator;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {

    private final int PICK_IMAGE = 1;

    private final String XML = "<image_process_call>\n" +
            "    <lang>en</lang>\n" +
            "    <image_url order=\"1\">http://pho.to/parrot.jpg</image_url>\n" +
            "    <methods_list>\n" +
            "        <method order=\"1\">\n" +
            "            <name>desaturation</name>\n" +
            "        </method>\n" +
            "        <method order=\"2\">\n" +
            "            <name>cartoon</name>\n" +
            "            <params>fill_solid_color=1;target_color=(255,255,255);border_strength=20;border_width=3</params>\n" +
            "        </method>\n" +
            "    </methods_list>\n" +
            "    <result_format>png</result_format>\n" +
            "    <result_size>600</result_size>\n" +
            "</image_process_call>";

    CoordinatorLayout mCoordinatorLayout;

    ImageView mImageView;
    Button mBrowseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);

        try {
            Log.d("XML SHA1", SHA1(XML));
        } catch (Exception e) {
            e.printStackTrace();
        }

        mImageView = (ImageView) findViewById(R.id.image_view);

        mBrowseButton = (Button) findViewById(R.id.browse);
        mBrowseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
                gallIntent.setType("image/*");
                startActivityForResult(Intent.createChooser(gallIntent, "Select Picture"), PICK_IMAGE);
            }
        });
    }

    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte b : data) {
            int halfbyte = (b >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) : (char) ('a' + (halfbyte - 10)));
                halfbyte = b & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] textBytes = text.getBytes("iso-8859-1");
        md.update(textBytes, 0, textBytes.length);
        byte[] sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                mImageView.setImageBitmap(bitmap);
                Toast.makeText(getApplicationContext(), "Image Set", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void editPhoto() {
        // todo: these are the things I need to do with the photo
        // filters -> color -> desaturation -- convert to black and white
        // filters -> others -> cartoon -- set cartoon filter (fill solid color = true, target color = white)
    }
}