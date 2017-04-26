package events.equaliser.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

import java.util.ArrayList;

import events.equaliser.android.series.SeriesActivity;

/**
 * Shows upcoming events for the user to browse.
 */
public class BrowseTabFragment extends Fragment {
    private static final String TAG = BrowseTabFragment.class.getSimpleName();
    private static final String SHOWCASE_ENDPOINT = Utils.API_ENDPOINT + "/series/showcase";
    private static final String VOLLEY_TAG = "VOLLEY_STRING_REQUEST";
    private static final String EVENT_ID_EXTRA = "EVENT_ID";
    ArrayList<BrowseEventCard> listItems = new ArrayList<>();
    BrowseEventCardTabAdapter browseEventCardTabAdapter;
    RecyclerView recyclerView;
    private RequestQueue requestQueue;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        browseEventCardTabAdapter = new BrowseEventCardTabAdapter(listItems);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.event_card_tab_fragment, container, false);

        // set up the RecyclerView
        recyclerView = (RecyclerView) view.findViewById(R.id.card_view);
        recyclerView.setHasFixedSize(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        // populate the RecyclerView with the events
        if (recyclerView != null) {
            recyclerView.setAdapter(browseEventCardTabAdapter);
        }

        pullData();

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                pullData();
            }
        });

        return view;
    }

    public void cardOnClick(View view) {
        int clickedItem = recyclerView.getChildLayoutPosition(view);
        Intent intent = new Intent(getContext(), SeriesActivity.class);
        intent.putExtra(EVENT_ID_EXTRA, listItems.get(clickedItem).getEventId());
        startActivity(intent);
    }

    @Override
    public void onStop() {
        if (requestQueue != null) {
            requestQueue.cancelAll(VOLLEY_TAG);
        }

        super.onStop();
    }

    public void initializeList(ArrayList<BrowseEventCard> items) {
        listItems.clear();

        listItems.addAll(items);

        browseEventCardTabAdapter.notifyDataSetChanged();
    }

    /**
     * Retrieves series information from the web server.
     */
    public void pullData() {
        requestQueue = Volley.newRequestQueue(getContext());

        final ArrayList<BrowseEventCard> browseEventCards = new ArrayList<>();

        StringRequest stringRequest = new StringRequest
                (Request.Method.GET, SHOWCASE_ENDPOINT, new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            Log.e(TAG, response);
                            if (jsonObject.getBoolean("success")) {
                       JSONArray result = jsonObject.getJSONArray("result");
                                for (int i = 0; i < result.length(); i++) {
                                    JSONObject currentResult = result.getJSONObject(i);

                                    int id = currentResult.getInt("id");
                                    String title = currentResult.getString("name");
                                    String subtitle = TextUtils.join(", ", Utils.jsonArrayToStringArray(currentResult.getJSONArray("tags")));

                                    JSONArray sizes = currentResult.getJSONArray("images")
                                            .getJSONObject(0).getJSONArray("sizes");

                                    String imageURL = "";
                                    for (int j = 0; j < sizes.length(); j++) {
                                        JSONObject currentSize = sizes.getJSONObject(0);
                                        if (currentSize.getInt("width") == 1024) {
                                            imageURL = currentSize.getString("url");
                                            break;
                                        }
                                    }

                                    browseEventCards.add(new BrowseEventCard(title, subtitle, imageURL, id));
                                }

                                initializeList(browseEventCards);

                                if (swipeRefreshLayout != null) {
                                    swipeRefreshLayout.setRefreshing(false);
                                }
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

    /**
     * Adapter for RecyclerView.
     */
    class BrowseEventCardTabAdapter extends RecyclerView.Adapter<BrowseEventCardTabAdapter.BrowseEventCardTabViewHolder> {
        private ArrayList<BrowseEventCard> list;

        BrowseEventCardTabAdapter(ArrayList<BrowseEventCard> data) {
            list = data;
        }

        @Override
        public BrowseEventCardTabViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.browse_tab_recycler_view_item, parent, false);
            return new BrowseEventCardTabViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final BrowseEventCardTabViewHolder holder, int position) {
            holder.textView.setText(list.get(position).getTitle());
            new DownloadImageTask(holder.imageView).execute(list.get(position).getImageUrl());
            holder.subTextView.setText(list.get(position).getSubtitle());

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
                holder.cardView.setLayoutParams(layoutParams);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        /**
         * ViewHolder for RecyclerView.
         */
        class BrowseEventCardTabViewHolder extends RecyclerView.ViewHolder {

            TextView textView;
            ImageView imageView;
            TextView subTextView;
            CardView cardView;

            BrowseEventCardTabViewHolder(View v) {
                super(v);
                textView = (TextView) v.findViewById(R.id.event_title_text_view);
                imageView = (ImageView) v.findViewById(R.id.image_view);
                subTextView = (TextView) v.findViewById(R.id.event_sub_text_view);
                cardView = (CardView) v.findViewById(R.id.card_view);

                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        cardOnClick(view);
                    }
                });
            }


        }
    }
}
