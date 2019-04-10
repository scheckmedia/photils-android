package app.photils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;

public class SplashScreen extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        Intent intent = new Intent(getApplicationContext(),
                MainActivity.class);

        String type = getIntent().getType();
        if(Intent.ACTION_SEND.equals(getIntent().getAction()) && type != null) {
            Uri imageUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            if(type.startsWith("image/") && imageUri != null) {
                intent.putExtra("shared_image", imageUri.toString());
            }
        }

        startActivity(intent);
        finish();
    }

}
