package app.photils;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;

public class MainActivity extends AppCompatActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener, Keywhat.OnFragmentInteractionListener {

    private Toolbar toolbar;

    public Toolbar getToolbar() {
        return toolbar;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        BottomNavigationView bottomBar =findViewById(R.id.bottm_navigation);
        bottomBar.setOnNavigationItemSelectedListener(this);
        bottomBar.getMenu().getItem(0).setChecked(true);

        changeFragment(R.id.nav_keywhat);

        Intent intent = getIntent();
        String type = intent.getType();

        if(Intent.ACTION_SEND.equals(intent.getAction()) && type != null) {
            if(type.startsWith("image/")) {
                handleReceivedImage(intent);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.keyhwat_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.keywhat_action_share) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        return changeFragment(item.getItemId());
    }

    private boolean changeFragment(int id) {
        Fragment fragment = null;
        String title = "";

        if (id == R.id.nav_keywhat) {
            // Handle the camera action
            fragment = new Keywhat();
        }
        /*else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        }*/

        if(fragment != null)
        {
            FragmentTransaction  ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.content_frame, fragment);
            ft.commit();

            title = getResources().getString(R.string.menu_keywhat);

        }

        getSupportActionBar().setTitle(title);
        return true;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleReceivedImage(Intent intent) {
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            changeFragment(R.id.nav_keywhat);

            Keywhat fragment = Keywhat.newInstance(imageUri);

            if(fragment != null)
            {
                FragmentTransaction  ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.content_frame, fragment);
                ft.commit();

                String title = getResources().getString(R.string.menu_keywhat);
                getSupportActionBar().setTitle(title);
            }
        }
    }
}
