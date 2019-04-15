package app.photils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;

import com.google.android.gms.ads.MobileAds;

public class SplashScreen extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        MobileAds.initialize(this, "ca-app-pub-4565424718929305~3838388374");

        Intent intent = new Intent(getApplicationContext(),
                MainActivity.class);

        String type = getIntent().getType();
        if(Intent.ACTION_SEND.equals(getIntent().getAction()) && type != null) {
            Uri imageUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            if(type.startsWith("image/") && imageUri != null) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setData(imageUri);
            }
        }

        startActivity(intent);
        finish();
    }

}
