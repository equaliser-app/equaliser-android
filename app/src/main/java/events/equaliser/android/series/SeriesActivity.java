package events.equaliser.android.series;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import events.equaliser.android.DownloadImageTask;
import events.equaliser.android.R;
import events.equaliser.android.Utils;

/**
 * Provides functionality for displaying events in a CardView on a Fragment.
 */
public class SeriesActivity extends AppCompatActivity {
    private static final String TAG = SeriesActivity.class.getSimpleName();
    private static final String EVENT_ID_EXTRA = "EVENT_ID";
    private static final String VOLLEY_TAG = "VOLLEY_STRING_REQUEST";
    private static final String SERIES_ENDPOINT = Utils.API_ENDPOINT + "/series/";

    ArrayList<SeriesRecyclerViewItem> listItems = new ArrayList<>();
    FixtureAdapter fixtureAdapter;
    RecyclerView fixturesRecyclerView;

    RequestQueue requestQueue;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fixtureAdapter = new FixtureAdapter(listItems);

        getSupportActionBar().setTitle(getString(R.string.fixtures_activity_title));

        setContentView(R.layout.series_activity);

        // set up the RecyclerView
        fixturesRecyclerView = (RecyclerView) findViewById(R.id.series_recycler_view);
        fixturesRecyclerView.setHasFixedSize(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        fixturesRecyclerView.setLayoutManager(linearLayoutManager);

        // populate the RecyclerView with the events
        if (fixturesRecyclerView != null) {
            fixturesRecyclerView.setAdapter(fixtureAdapter);
        }

        pullFixtures();
    }

    /**
     * Clears and repopulates the list of information items.
     */
    public void initializeList(ArrayList<SeriesRecyclerViewItem> items) {
        listItems.clear();
        listItems.addAll(items);

        // add the header for the list of fixtures
        listItems.add(4, new SeriesName(getString(R.string.fixtures)));
        fixtureAdapter.notifyDataSetChanged();
    }

    /**
     * Retrieves the fixtures from the web server.
     */
    public void pullFixtures() {
        int eventId = getIntent().getIntExtra(EVENT_ID_EXTRA, 0);

        if (eventId != 0) {
            requestQueue = Volley.newRequestQueue(getApplicationContext());
            final ArrayList<SeriesRecyclerViewItem> items = new ArrayList<>();

            StringRequest stringRequest = new StringRequest
                    (Request.Method.GET, (SERIES_ENDPOINT + eventId), new Response.Listener<String>() {

                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject jsonObject = new JSONObject(response);
                                if (jsonObject.getBoolean("success")) {
                                    JSONObject result = jsonObject.getJSONObject("result");

                                    // get the image URL
                                    JSONArray sizes = result.getJSONArray("images")
                                            .getJSONObject(0).getJSONArray("sizes");

                                    for (int j = 0; j < sizes.length(); j++) {
                                        JSONObject currentSize = sizes.getJSONObject(0);
                                        if (currentSize.getInt("width") == 1024) {
                                            items.add(new SeriesImage(currentSize.getString("url")));
                                            break;
                                        }
                                    }

                                    // get the fixture title
                                    items.add(new SeriesName(result.getString("name")));
                                    // get the fixture tags
                                    items.add(new SeriesTags(Utils.jsonArrayToStringArray(result.getJSONArray("tags"))));
                                    // get the fixture description
                                    items.add(new SeriesDescription(result.getString("description")));

                                    // get the fixture tiers
                                    JSONArray fixtures = result.getJSONArray("fixtures");
                                    for (int i = 0; i < fixtures.length(); i++) {
                                        JSONObject currentFixture = fixtures.getJSONObject(i);
                                        long longDate = (long) currentFixture.getInt("start") * 1000;
                                        Date date = new java.util.Date(longDate);

                                        JSONArray tiers = currentFixture.getJSONArray("tiers");
                                        ArrayList<FixtureTier> tiersList = new ArrayList<>();
                                        for (int j = 0; j < tiers.length(); j++) {
                                            JSONObject currentTier = tiers.getJSONObject(j);
                                            String tierName = currentTier.getString("name");
                                            BigDecimal tierPrice = new BigDecimal(currentTier.getString("price"));
                                            Boolean tierAvailable = currentTier.getBoolean("available");
                                            tiersList.add(new FixtureTier(tierName, tierPrice, tierAvailable));
                                        }
                                        items.add(new FixtureCard(currentFixture.getJSONObject("venue").getString("name"), date, tiersList));
                                    }

                                    initializeList(items);
                                }

                            } catch (JSONException e) {
                                Log.e(TAG, e.getMessage());
                            }
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, error.toString());
                        }
                    });

            stringRequest.setTag(VOLLEY_TAG);
            stringRequest.setShouldCache(false);
            requestQueue.add(stringRequest);
        }
    }

    /**
     * Custom RecyclerView Adapter for fixture information items.
     */
    private class FixtureAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int IMAGE_VIEW = 0;
        private static final int TITLE_VIEW = 1;
        private static final int TAGS_VIEW = 2;
        private static final int DESCRIPTION_VIEW = 3;
        private static final int TIER_CARD_VIEW = 4;
        private ArrayList<SeriesRecyclerViewItem> list;

        FixtureAdapter(ArrayList<SeriesRecyclerViewItem> data) {
            list = data;
        }

        @Override
        public int getItemViewType(int position) {
            switch (position) {
                case 0:
                    return IMAGE_VIEW;
                case 1:
                case 4:
                    return TITLE_VIEW;
                case 2:
                    return TAGS_VIEW;
                case 3:
                    return DESCRIPTION_VIEW;
                default:
                    return TIER_CARD_VIEW;
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.series_recycler_view_image, parent, false);
                    return new FixtureImageViewHolder(view);
                case 1:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.series_recycler_view_title, parent, false);
                    return new FixtureTitleViewHolder(view);
                case 2:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.series_recycler_view_tags, parent, false);
                    return new FixtureTagsViewHolder(view);
                case 3:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.series_recycler_view_description, parent, false);
                    return new FixtureDescriptionViewHolder(view);
                default:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.fixture_card, parent, false);
                    return new FixtureTierCardViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
            switch (holder.getItemViewType()) {
                case 0:
                    FixtureImageViewHolder viewHolder0 = (FixtureImageViewHolder) holder;
                    SeriesImage currentItem0 = (SeriesImage) list.get(position);
                    new DownloadImageTask(viewHolder0.image).execute(currentItem0.getImageUrl());
                    break;
                case 1:
                    FixtureTitleViewHolder viewHolder1 = (FixtureTitleViewHolder) holder;
                    SeriesName currentItem1 = (SeriesName) list.get(position);
                    viewHolder1.title.setText(currentItem1.getName());
                    break;
                case 2:
                    FixtureTagsViewHolder viewHolder3 = (FixtureTagsViewHolder) holder;
                    SeriesTags currentItem3 = (SeriesTags) list.get(position);

                    // set the background colour of each tag
                    CharSequence cs = "";

                    for (int i = 0; i < currentItem3.getTags().length; i++) {
                        SpannableString spannableString = new SpannableString(currentItem3.getTags()[i]);
                        spannableString.setSpan(new BackgroundColorSpan(getResources().getColor(R.color.textBackgroundDark)), 0, spannableString.length(), 0);
                        cs = TextUtils.concat(cs, " ", spannableString);
                    }

                    viewHolder3.tags.setText(cs);
                    break;
                case 3:
                    FixtureDescriptionViewHolder viewHolder2 = (FixtureDescriptionViewHolder) holder;
                    SeriesDescription currentItem2 = (SeriesDescription) list.get(position);
                    viewHolder2.description.setText(currentItem2.getDescription());
                    break;
                default:
                    FixtureTierCardViewHolder viewHolderDefault = (FixtureTierCardViewHolder) holder;
                    FixtureCard currentItemDefault = (FixtureCard) list.get(position);
                    viewHolderDefault.dateTime.setText(new SimpleDateFormat("dd MMMM yyyy HH:mm").format(currentItemDefault.getDateTime()));
                    viewHolderDefault.venue.setText(currentItemDefault.getVenue());

                    ListView listView = viewHolderDefault.listView;
                    listView.setAdapter(new TierAdapter(getApplicationContext(), R.layout.fixture_tier_item, currentItemDefault.getTiers()));
                    Utils.setListViewHeightBasedOnChildren(listView);

                    // if the card is the last item in the list, fix the margins to be in line with the
                    // rest of the cards in the list
                    if (position == getItemCount() - 1) {
                        CardView.LayoutParams layoutParams = new CardView.LayoutParams(
                                CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.WRAP_CONTENT);
                        layoutParams.setMargins(
                                (int) (getResources().getDimension(R.dimen.activity_vertical_margin)),
                                (int) (getResources().getDimension(R.dimen.activity_vertical_margin)),
                                (int) (getResources().getDimension(R.dimen.activity_vertical_margin)),
                                (int) (getResources().getDimension(R.dimen.activity_vertical_margin)));
                        viewHolderDefault.cardView.setLayoutParams(layoutParams);
                    }
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        /**
         * Custom ViewHolder for fixture images.
         */
        class FixtureImageViewHolder extends RecyclerView.ViewHolder {
            ImageView image;

            FixtureImageViewHolder(View view) {
                super(view);
                image = (ImageView) view.findViewById(R.id.fixture_image);
            }
        }

        /**
         * Custom ViewHolder for fixture titles.
         */
        class FixtureTitleViewHolder extends RecyclerView.ViewHolder {
            TextView title;

            FixtureTitleViewHolder(View view) {
                super(view);
                title = (TextView) view.findViewById(R.id.fixture_title);
            }
        }

        /**
         * Custom ViewHolder for fixture descriptions.
         */
        class FixtureDescriptionViewHolder extends RecyclerView.ViewHolder {
            TextView description;

            FixtureDescriptionViewHolder(View view) {
                super(view);
                description = (TextView) view.findViewById(R.id.fixture_description);
            }
        }

        /**
         * Custom ViewHolder for fixture tags.
         */
        class FixtureTagsViewHolder extends RecyclerView.ViewHolder {
            TextView tags;

            FixtureTagsViewHolder(View view) {
                super(view);
                tags = (TextView) view.findViewById(R.id.fixture_tags);
            }
        }

        /**
         * Custom ViewHolder for fixture tier cards.
         */
        class FixtureTierCardViewHolder extends RecyclerView.ViewHolder {
            CardView cardView;
            TextView venue;
            TextView dateTime;
            ListView listView;

            FixtureTierCardViewHolder(View view) {
                super(view);
                cardView = (CardView) view.findViewById(R.id.card_view);
                venue = (TextView) view.findViewById(R.id.venue_text_view);
                dateTime = (TextView) view.findViewById(R.id.date_time_text_view);
                listView = (ListView) view.findViewById(R.id.tiers_list_view);
            }
        }
    }

    /**
     * Custom ListAdapter for list of tiers.
     */
    private class TierAdapter extends ArrayAdapter<FixtureTier> {

        /**
         * Constructor.
         *
         * @param context  The context.
         * @param resource The layout resource file.
         * @param items    The underlying data.
         */
        TierAdapter(Context context, int resource, ArrayList<FixtureTier> items) {
            super(context, resource, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            FixtureTier currentItem = getItem(position);

            TierViewHolder tierViewHolder;

            if (convertView == null) {
                tierViewHolder = new TierViewHolder();
                LayoutInflater layoutInflater = LayoutInflater.from(getContext());
                convertView = layoutInflater.inflate(R.layout.fixture_tier_item, null);
                tierViewHolder.tierName = (TextView) convertView.findViewById(R.id.tier_name);
                tierViewHolder.tierPrice = (TextView) convertView.findViewById(R.id.tier_price);
                tierViewHolder.buyWaitButton = (Button) convertView.findViewById(R.id.buy_wait_button);

                convertView.setTag(tierViewHolder);
            } else {
                tierViewHolder = (TierViewHolder) convertView.getTag();
            }

            tierViewHolder.tierName.setText(currentItem.getTier());

            NumberFormat formatter = NumberFormat.getCurrencyInstance();
            tierViewHolder.tierPrice.setText(formatter.format(currentItem.getPrice()));

            if (currentItem.isAvailable()) {
                tierViewHolder.buyWaitButton.setText(getResources().getString(R.string.buy));
                tierViewHolder.buyWaitButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                tierViewHolder.buyWaitButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // TODO allow user to buy ticket(s)
                    }
                });
            } else {
                tierViewHolder.buyWaitButton.setText(getResources().getString(R.string.wait));
                tierViewHolder.buyWaitButton.setBackgroundColor(getResources().getColor(R.color.backgroundColorOrange));
                tierViewHolder.buyWaitButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // TODO allow user to join waiting list
                    }
                });
            }

            // remove the bottom padding of the last item in the list
            if (position == getCount() - 1) {
                RelativeLayout relativeLayout = (RelativeLayout) convertView.findViewById(R.id.fixtures_list_view_item_relative_layout);
                relativeLayout.setPadding(0, (int) getResources().getDimension(R.dimen.tier_padding), 0, 0);
            }

            return convertView;
        }

        /**
         * ViewHolder for tier list items.
         */
        private class TierViewHolder {
            TextView tierName;
            TextView tierPrice;
            Button buyWaitButton;
        }
    }
}
