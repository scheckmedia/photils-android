package app.photils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;


public class MainActivity extends AppCompatActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener, Keywhat.OnKeywhatListener {

    private Toolbar mToolbar;
    private InterstitialAd mInterstitialAd;
    private int mCurrentMenu = R.id.nav_keywhat;

    public Toolbar getmToolbar() {
        return mToolbar;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-4565424718929305/3632119213");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }
        });
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);


        BottomNavigationView bottomBar =findViewById(R.id.bottm_navigation);
        bottomBar.setOnNavigationItemSelectedListener(this);
        bottomBar.getMenu().getItem(0).setChecked(true);

        Fragment f = null;
        String title = "";

        Intent intent = getIntent();
        Uri uri = intent.getData();

        if (mCurrentMenu == R.id.nav_keywhat) {
            if(uri != null) {
                f = Keywhat.newInstance(uri);
                intent.setData(null);
            } else if(savedInstanceState != null) {
                KeywhatState state = savedInstanceState.getParcelable("fragmentState");
                f = Keywhat.newInstance(state);
            }

            title = getResources().getString(R.string.menu_keywhat);

            if(f != null) {
                //f.setRetainInstance(true);
                FragmentTransaction  ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.content_frame, f, "KEYWHAT");
                ft.commit();

                getSupportActionBar().setTitle(title);

            } else {
                changeFragment(R.id.nav_keywhat);
            }
        }





    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Keywhat fragment = (Keywhat) getSupportFragmentManager().findFragmentByTag("KEYWHAT");
        if(fragment != null && fragment.isVisible()) {
            outState.putParcelable("fragmentState", fragment.getState());
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

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        return changeFragment(item.getItemId());
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if(fragment instanceof Keywhat) {
            ((Keywhat) fragment).setListener(this);
        }
    }

    private boolean changeFragment(int id) {
        Fragment fragment = null;
        String title = "";
        mCurrentMenu = id;

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
            ft.replace(R.id.content_frame, fragment, "KEYWHAT");
            ft.commit();

            title = getResources().getString(R.string.menu_keywhat);
        }

        //fragment.setRetainInstance(true);
        getSupportActionBar().setTitle(title);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onTagSelectedSize(int size) {
        int numItems = mToolbar.getMenu().size();
        for(int i = 0; i < numItems; i++) {
            getmToolbar().getMenu().getItem(i).setVisible( size > 0);
        }
    }

    @Override
    public void onRequestTags() { }

    @Override
    public void onTagsAvailable() {
        if(mInterstitialAd.isLoaded())
            mInterstitialAd.show();
    }
}
