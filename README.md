# ColoringPageGenerator - Pho.to API
I implemented this API in an Android app, but these instructions are not specific to Android. The info here will help you to use this API, regardless of platform. Hopefully it will help you get started, and minimize the time you spend researching and pulling your hair out.

First off, here's the link to the API itself, where you can sign up for a key and read the documentation: http://developers.pho.to/

———————————————

Starting out, you'll need to generate a SHA1 from your XML and your key. If you're doing this in Android, take a look at my `hmacSha1()` method in `MainActivity.java`. If you just want one to test out the API, here's some PHP code that will generate one for you:

    <?php
    
    $xml = '<image_process_call><image_url>http://blog.nuraypictures.com/wp-content/uploads/2013/11/Ron-Swanson.jpg</image_url><methods_list><method order="1"><name>deblurring</name></method><method order="2"><name>cartoon</name><params>use_anisotropic=TRUE;fill_solid_color=1;target_color=(255,255,255);</params></method></methods_list><result_format>png</result_format><result_size>1500</result_size></image_process_call>';
    
    echo(hash_hmac('SHA1', $xml, '<my-key>'));
    
    ?>
    
Just throw this code in a PHP sandbox (I used http://stargento.com/) to get your SHA1 for testing. Make sure you replace the image URL with your own.

———————————————

I tried a whole lot of different methods to get the images to look most like a coloring page. I'm still trying to decide which combination of methods will yield the best result every time. In general, the further down this list you go, the worse they get.

desaturation + deblurring + adaptive cartoon + cartoon with anistropic=true:

    <image_process_call><image_url>http://blog.nuraypictures.com/wp-content/uploads/2013/11/Ron-Swanson.jpg</image_url><methods_list><method order="1"><name>desaturation</name></method><method order="2"><name>deblurring</name></method><method order="3"><name>adaptive_cartoon</name></method><method order="4"><name>cartoon</name><params>use_anisotropic=TRUE;fill_solid_color=1;target_color=(255,255,255);</params></method></methods_list><result_format>png</result_format><result_size>1500</result_size></image_process_call>

deblurring + adaptive cartoon + cartoon with anistropic=true:

    <image_process_call><image_url>http://blog.nuraypictures.com/wp-content/uploads/2013/11/Ron-Swanson.jpg</image_url><methods_list><method order="1"><name>deblurring</name></method><method order="2"><name>adaptive_cartoon</name></method><method order="3"><name>cartoon</name><params>use_anisotropic=TRUE;fill_solid_color=1;target_color=(255,255,255);</params></method></methods_list><result_format>png</result_format><result_size>1500</result_size></image_process_call>

deblurring + cartoon with anisotropic=true:

    <image_process_call><image_url>http://blog.nuraypictures.com/wp-content/uploads/2013/11/Ron-Swanson.jpg</image_url><methods_list><method order="1"><name>deblurring</name></method><method order="2"><name>cartoon</name><params>use_anisotropic=TRUE;fill_solid_color=1;target_color=(255,255,255);</params></method></methods_list><result_format>png</result_format><result_size>1500</result_size></image_process_call>

cartoon with anisotropic=true:

    <image_process_call><image_url>http://blog.nuraypictures.com/wp-content/uploads/2013/11/Ron-Swanson.jpg</image_url><methods_list><method><name>cartoon</name><params>use_anisotropic=TRUE;fill_solid_color=1;target_color=(255,255,255);</params></method></methods_list><result_format>png</result_format><result_size>1500</result_size></image_process_call>

adaptive cartoon + cartoon:

    <image_process_call><image_url>http://blog.nuraypictures.com/wp-content/uploads/2013/11/Ron-Swanson.jpg</image_url><methods_list><method order="1"><name>adaptive_cartoon</name></method><method order="2"><name>cartoon</name><params>fill_solid_color=1;target_color=(255,255,255);</params></method></methods_list><result_format>png</result_format><result_size>1500</result_size></image_process_call>

cartoon:

    <image_process_call><image_url>http://blog.nuraypictures.com/wp-content/uploads/2013/11/Ron-Swanson.jpg</image_url><methods_list><method><name>cartoon</name><params>fill_solid_color=1;target_color=(255,255,255);</params></method></methods_list><result_format>png</result_format><result_size>1500</result_size></image_process_call>

ink:

    <image_process_call><image_url>http://blog.nuraypictures.com/wp-content/uploads/2013/11/Ron-Swanson.jpg</image_url><methods_list><method><name>ink</name><params>black_area=0;</params></method></methods_list><result_format>png</result_format><result_size>1500</result_size></image_process_call>

sketch:

    <image_process_call><image_url>http://blog.nuraypictures.com/wp-content/uploads/2013/11/Ron-Swanson.jpg</image_url><methods_list><method><name>sketch</name><params>type=0;strength=1;details=0;color=(0, 0, 0);</params></method></methods_list><result_format>png</result_format><result_size>1500</result_size></image_process_call>

———————————————

Formatted XML (cartoon):

    <image_process_call>
        <image_url>http://blog.nuraypictures.com/wp-content/uploads/2013/11/Ron-Swanson.jpg</image_url>
        <methods_list>
            <method>
                <name>cartoon</name>
                <params>fill_solid_color=1;target_color=(255,255,255);</params>
            </method>
        </methods_list>
        <result_format>png</result_format>
        <result_size>1500</result_size>
    </image_process_call>

———————————————

Full API call (POST): 

    http://opeapi.ws.pho.to/addtask?data=<image_process_call><image_url>http://blog.nuraypictures.com/wp-content/uploads/2013/11/Ron-Swanson.jpg</image_url><methods_list><method><name>cartoon</name><params>fill_solid_color=1;target_color=(255,255,255);</params></method></methods_list><result_format>png</result_format><result_size>1500</result_size></image_process_call>&sign_data=fada3e244de49c7e35d1c3714ca87e49d0467185&app_id=<my-app_id>&key=<my-key>

Again, make sure to replace <my-key> and <my-app-id> with the values you get from pho.to

———————————————

Here's the response you'll get back:

    <?xml version="1.0" ?>
        <image_process_response>
        <request_id>010afc13-6bba-44dd-b278-4f3bd1e41946</request_id>
        <status>OK</status>
        <description />
        <err_code>0</err_code>
    </image_process_response>

———————————————

Use the `request_id` in another call (GET): 

    http://opeapi.ws.pho.to/getresult?request_id=010afc13-6bba-44dd-b278-4f3bd1e41946

———————————————

This will return another block of XML that will contain the URL to your converted image:

    <image_process_response>
        <request_id>010afc13-6bba-44dd-b278-4f3bd1e41946</request_id>
        <status>OK</status>
        <result_url>http://worker-images.ws.pho.to/i1/3BCB160A-691A-458B-9161-67AFA8A9EAA0.png</result_url>
        <result_url_alt>http://worker-images.ws.pho.to.s3.amazonaws.com/i1/3BCB160A-691A-458B-9161-67AFA8A9EAA0.png</result_url_alt>
        <nowm_image_url>http://worker-images.ws.pho.to/i1/3BCB160A-691A-458B-9161-67AFA8A9EAA0.png</nowm_image_url>
    </image_process_response>
    
———————————————

And you're done!
