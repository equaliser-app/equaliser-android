package events.equaliser.android;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 'Home screen' of the app.
 */
public class MainActivity extends AppCompatActivity {
    private static final String LOGIN_INFORMATION_EXTRA = "LOGIN_INFORMATION_EXTRA";
    private static final String JSON_OBJECT_RESULT = "result";
    private static final String JSON_OBJECT_SESSION = "session";
    private static final String JSON_OBJECT_USER = "user";
    private static final String TAG = MainActivity.class.getSimpleName();

    private SharedPreferences sharedPreferences;

    // labels for items in the navigation drawer
    private String[] drawerItems = new String[4];
    private DrawerLayout drawerLayout;
    private ListView listView;

    private ActionBarDrawerToggle drawerToggle;
    private CharSequence drawerTitle;
    private CharSequence mTitle;

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();

            //TODO make this general to select the previously selected item
            selectItem(2, false);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        getSupportActionBar().setTitle(mTitle);
        super.onResume();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {

            }
        });

        sharedPreferences = EqualiserSharedPreferences
                .getEqualiserSharedPreferences(getApplicationContext());

        // populate the names of the items in the navigation drawer
        drawerItems[0] = getString(R.string.browse_tab_title);
        drawerItems[1] = getString(R.string.attending_tab_title);
        drawerItems[2] = getString(R.string.ticket_tab_title);
        drawerItems[3] = getString(R.string.logout_tab_title);

        setContentView(R.layout.activity_main);
        LayoutInflater inflater = getLayoutInflater();

        // set up the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // set the title of the navigation drawer
        mTitle = drawerTitle = getTitle();

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        // set up the header in the navigation drawer
        View headerView = inflater.inflate(R.layout.drawer_list_header, null, false);
        headerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // do nothing
            }
        });

        // set up the navigation drawer
        listView = (ListView) findViewById(R.id.list_view);
        listView.addHeaderView(headerView);
        listView.setAdapter(new ArrayAdapter<>(this,
                R.layout.drawer_list_item, drawerItems));
        listView.setOnItemClickListener(new DrawerItemClickListener());

        // set up opening and closing the navigation drawer
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View view) {
                getSupportActionBar().setTitle(drawerTitle);
                invalidateOptionsMenu();
            }
        };

        drawerLayout.setDrawerListener(drawerToggle);

        // by default, select the first item in the navigation drawer
        if (savedInstanceState == null) {
            selectItem(1, false);
        }

        Intent intent = getIntent();

        // only populate SharedPreferences if there is an extra
        if (intent.hasExtra(LOGIN_INFORMATION_EXTRA)) {
            Log.e(TAG, intent.getStringExtra(LOGIN_INFORMATION_EXTRA));
            parseLoginResponse(intent.getStringExtra(LOGIN_INFORMATION_EXTRA));
        }

        populateUserInformation(headerView);
    }

    /**
     * Add user information to the navigation drawer header.
     *
     * @param headerView The navigation drawer header.
     */
    private void populateUserInformation(View headerView) {

        if (sharedPreferences.contains(EqualiserSharedPreferences.FORENAME) &&
                sharedPreferences.contains(EqualiserSharedPreferences.SURNAME)) {
            TextView nameTextView = (TextView) headerView.findViewById(R.id.text_view_name);
            String forename = sharedPreferences.getString(EqualiserSharedPreferences.FORENAME, "");
            String surname = sharedPreferences.getString(EqualiserSharedPreferences.SURNAME, "");

            nameTextView.setText(forename + " " + surname);
        }

        if (sharedPreferences.contains(EqualiserSharedPreferences.EMAIL)) {
            TextView emailTextView = (TextView) headerView.findViewById(R.id.text_view_email);
            emailTextView.setText(sharedPreferences.getString(
                    EqualiserSharedPreferences.EMAIL, ""));
        }

        if (sharedPreferences.contains(EqualiserSharedPreferences.PROFILE_IMAGE)) {
            // set the profile image in the navigation drawer
            new DownloadImageTask((ImageView) headerView.findViewById(R.id.profile_image_view))
                    .execute(sharedPreferences.getString(EqualiserSharedPreferences.PROFILE_IMAGE, ""));
        }

    }

    /**
     * Parse the user data and populate SharedPreferences.
     *
     * @param json The JSON object containing user data.
     */
    private void parseLoginResponse(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONObject session = jsonObject.getJSONObject(JSON_OBJECT_RESULT)
                    .getJSONObject(JSON_OBJECT_SESSION);

            sharedPreferences.edit().putString(EqualiserSharedPreferences.SESSION_TOKEN,
                    session.getString(EqualiserSharedPreferences.TOKEN)).apply();

            JSONObject user = session.getJSONObject(JSON_OBJECT_USER);

            sharedPreferences.edit().putString(EqualiserSharedPreferences.TOKEN,
                    user.getString(EqualiserSharedPreferences.TOKEN)).apply();
            sharedPreferences.edit().putString(EqualiserSharedPreferences.USERNAME,
                    user.getString(EqualiserSharedPreferences.USERNAME)).apply();
            sharedPreferences.edit().putString(EqualiserSharedPreferences.FORENAME,
                    user.getString(EqualiserSharedPreferences.FORENAME)).apply();
            sharedPreferences.edit().putString(EqualiserSharedPreferences.SURNAME,
                    user.getString(EqualiserSharedPreferences.SURNAME)).apply();
            sharedPreferences.edit().putString(EqualiserSharedPreferences.EMAIL,
                    user.getString(EqualiserSharedPreferences.EMAIL)).apply();
            sharedPreferences.edit().putString(EqualiserSharedPreferences.PHONE,
                    user.getString(EqualiserSharedPreferences.PHONE)).apply();

            JSONArray sizes = user.getJSONObject("image").getJSONArray("sizes");

            for (int j = 0; j < sizes.length(); j++) {
                JSONObject currentSize = sizes.getJSONObject(0);
                if (currentSize.getInt("width") == 200) {
                    sharedPreferences.edit().putString(EqualiserSharedPreferences.PROFILE_IMAGE,
                            currentSize.getString(EqualiserSharedPreferences.URL)).apply();
                    break;
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Switches screens when an item in the navigation drawer is clicked.
     *
     * @param position The item's position.
     */
    public void selectItem(int position, boolean addToBackStack) {
        Fragment fragment;

        switch (position) {
            case 2:
                fragment = new AttendingTabFragment();
                break;
            case 3:
                fragment = new TicketTabFragment();
                break;
            case 4:
                logout();
                return;
            default:
                fragment = new BrowseTabFragment();
                break;
        }

        // replace the currently displayed fragment by the new one
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.content_frame, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();

        // update the currently selected item and title, then close the navigation drawer
        listView.setItemChecked(position, true);
        setTitle(drawerItems[position - 1]);
        drawerLayout.closeDrawer(listView);
    }

    private void logout() {
        new AlertDialog.Builder(this, R.style.AlertDialogCustom)
                .setTitle("Logout")
                .setMessage("You won't be able to log back in without access to the Equaliser website.")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        sharedPreferences.edit().clear().apply();
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        dialog.dismiss();
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        selectItem(1, false);
                    }
                })
                .show();
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Controls the click events for items in the navigation drawer.
     */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // Position 0 is the header which shouldn't be clickable.
            if (position > 0) {
                selectItem(position, false);
            }
        }
    }
}
