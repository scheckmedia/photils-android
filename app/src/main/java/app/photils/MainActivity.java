package app.photils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        Keywhat.OnKeywhatListener,
        Inspiration.OnInspirationListener {

    private Toolbar mToolbar;
    private DrawerLayout mDrawer;
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

        mDrawer = findViewById(R.id.main_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawer, mToolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(0).setChecked(true);

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
            title = getResources().getString(R.string.menu_keywhat);
        }
        else if (id == R.id.nav_inspiration) {
            fragment = new Inspiration();
            title = getResources().getString(R.string.menu_inspiration);
        }/* else if (id == R.id.nav_slideshow) {

        }*/

        if(fragment != null)
        {
            FragmentTransaction  ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.content_frame, fragment, "KEYWHAT");
            ft.commit();
        }

        //fragment.setRetainInstance(true);
        getSupportActionBar().setTitle(title);
        mDrawer.closeDrawer(GravityCompat.START);
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
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.main_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestTags() { }

    @Override
    public void onTagsAvailable() {
        if(mInterstitialAd.isLoaded())
            mInterstitialAd.show();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
