package app.photils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import app.photils.keywhat.KeywhatCustomTag;
import app.photils.keywhat.KeywhatViewModel;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        Keywhat.OnKeywhatListener {

    private static String BEER = "https://www.paypal.com/paypalme2/uncloned/2";
    private Toolbar mToolbar;
    private DrawerLayout mDrawer;
    private InterstitialAd mInterstitialAd;
    private int mCurrentMenu = R.id.nav_keywhat;
    private static final int CUSTOM_TAG_CODE = 42;
    private boolean mShowAds = true;


    public Toolbar getToolbar() {
        return mToolbar;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(BuildConfig.ads_key);
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
        //mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

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
            }

            title = getResources().getString(R.string.menu_keywhat);

            if(f != null) {
                //f.setRetainInstance(true);
                FragmentTransaction  ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.content_frame, f, title);
                ft.commit();

                getSupportActionBar().setTitle(title);

            } else {
                changeFragment(R.id.nav_keywhat);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if(id == R.id.main_action_info) {
            Info f = Info.newInstance(mCurrentMenu);
            f.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.InfoDialog);
            f.show(getSupportFragmentManager(), "InfoDialog");
        } else if(id == R.id.keywhat_action_custom_tags) {
            Intent i = new Intent(this, KeywhatCustomTag.class);
            startActivityForResult(i, CUSTOM_TAG_CODE);
        }

        return super.onOptionsItemSelected(item);
    }



    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.

        switch (item.getItemId()) {
            case R.id.nav_keywhat:
                return changeFragment(item.getItemId());
            case R.id.nav_custom_tags:
                Intent i = new Intent(this, KeywhatCustomTag.class);
                startActivityForResult(i, CUSTOM_TAG_CODE);
                break;
            case R.id.nav_share_app:
                Utils.shareContent(this, getString(R.string.share_app_title), getString(R.string.share_app_message));
                break;
            case R.id.nav_buy_beer:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BEER)));
                break;

        }

        return true;
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
        /*else if (id == R.id.nav_inspiration) {
            fragment = new Inspiration();
            title = getResources().getString(R.string.menu_inspiration);
        }/* else if (id == R.id.nav_slideshow) {

        }*/

        if(fragment != null)
        {
            FragmentTransaction  ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.content_frame, fragment, title);
            ft.commit();
        }

        //fragment.setRetainInstance(true);
        getSupportActionBar().setTitle(title);
        mDrawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CUSTOM_TAG_CODE) {
            if(resultCode == RESULT_OK && data != null) {
                boolean isDirty = data.getBooleanExtra("dirty", false);
                Keywhat k = (Keywhat)getSupportFragmentManager().findFragmentByTag("Keywhat");
                if(k != null && isDirty) {
                    k.notifyChange();
                }
            }
        }
    }

    @Override
    public void onTagSelectedSize(int size) {
        int numItems = mToolbar.getMenu().size();
        for(int i = 0; i < numItems; i++) {
            MenuItem item = getToolbar().getMenu().getItem(i);
            if(item.getOrder() > 100)
                item.setVisible( size > 0);
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
    public void onRequestTags() {
        mShowAds = true;
    }

    @Override
    public void onTagsAvailable() {
        if(mInterstitialAd.isLoaded() && mShowAds) {
            mInterstitialAd.show();
            mShowAds = false;
        }
    }


    /*@Override
    public void onFragmentInteraction(Uri uri) {

    }*/
}
