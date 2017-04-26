package events.equaliser.android;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import events.equaliser.android.AttendingEventCard.Status;

/**
 * Shows upcoming events for the user to browse.
 */
public class AttendingTabFragment extends Fragment {
    private static final String TAG = AttendingTabFragment.class.getSimpleName();
    private static final String ATTENDING_ENDPOINT = Utils.API_ENDPOINT + "/group/list";
    private static final String VOLLEY_TAG = "VOLLEY_STRING_REQUEST";
    ArrayList<AttendingEventCard> listItems = new ArrayList<>();
    AttendingEventCardTabAdapter attendingEventCardTabAdapter;
    RecyclerView recyclerView;
    private RequestQueue requestQueue;
    private SharedPreferences sharedPreferences;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        sharedPreferences = EqualiserSharedPreferences.getEqualiserSharedPreferences(getContext());
        attendingEventCardTabAdapter = new AttendingEventCardTabAdapter(listItems);
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
            recyclerView.setAdapter(attendingEventCardTabAdapter);
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

    @Override
    public void onStop() {
        if (requestQueue != null) {
            requestQueue.cancelAll(VOLLEY_TAG);
        }

        super.onStop();
    }

    public void cardOnClick() {
        ((MainActivity) getActivity()).selectItem(3, true);
    }

    public void initializeList(ArrayList<AttendingEventCard> items) {
        listItems.clear();

        listItems.addAll(items);

        attendingEventCardTabAdapter.notifyDataSetChanged();
    }

    /**
     * Retrieves series information from the web server.
     */
    public void pullData() {
        requestQueue = Volley.newRequestQueue(getContext());

        final ArrayList<AttendingEventCard> attendingEvents = new ArrayList<>();

        StringRequest stringRequest = new StringRequest
                (Request.Method.GET, ATTENDING_ENDPOINT, new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            if (jsonObject.getBoolean("success")) {
                                JSONArray results = jsonObject.getJSONArray("result");
                                for (int i = 0; i < results.length(); i++) {
                                    JSONObject currentResult = results.getJSONObject(i);
                                    JSONObject fixture = currentResult.getJSONObject("fixture");
                                    String seriesName = fixture.getJSONObject("series").getString("name");

                                    JSONArray sizes = fixture.getJSONObject("series").getJSONArray("images")
                                            .getJSONObject(0).getJSONArray("sizes");

                                    String imageURL = "";
                                    for (int j = 0; j < sizes.length(); j++) {
                                        JSONObject currentSize = sizes.getJSONObject(0);
                                        if (currentSize.getInt("width") == 1024) {
                                            imageURL = currentSize.getString("url");
                                            break;
                                        }
                                    }

                                    long longDate = (long) fixture.getInt("start") * 1000;
                                    Date date = new java.util.Date(longDate);
                                    String dateString = new SimpleDateFormat("dd MMMM yyyy HH:mm").format(date);

                                    String venue = fixture.getJSONObject("venue").getString("name");

                                    String subtitle = String.format("%s at %s", dateString, venue);

                                    Status eventStatus = null;
                                    String eventPayee = "";

                                    if (currentResult.getString("offer").equals("WAITING")) {
                                        eventStatus = Status.WAITING_TICKETS;
                                    } else {
                                        JSONArray paymentGroups = currentResult.getJSONArray("paymentGroups");
                                        paymentGroupsLoop:
                                        for (int j = 0; j < paymentGroups.length(); j++) {
                                            JSONObject currentPaymentGroup = paymentGroups.getJSONObject(j);
                                            String status = currentPaymentGroup.getString("status");

                                            String userToken = sharedPreferences.getString(EqualiserSharedPreferences.TOKEN, "");
                                            JSONObject payee = currentPaymentGroup.getJSONObject("payee");
                                            String payeeName = String.format("%s %s", payee.getString("forename"), payee.getString("surname"));

                                            if (payee.getString("token").equals(userToken)) {
                                                switch (status) {
                                                    case "COMPLETE":
                                                        eventStatus = Status.COMPLETE;
                                                        break;
                                                    case "EXPIRED":
                                                        eventStatus = Status.EXPIRED;
                                                        break;
                                                    default:
                                                        eventStatus = Status.WAITING_PAYMENT;
                                                        break;
                                                }
                                                eventPayee = payeeName;
                                                break;
                                            }

                                            JSONArray attendees = currentPaymentGroup.getJSONArray("attendees");
                                            for (int k = 0; k < attendees.length(); k++) {
                                                JSONObject currentAttendee = attendees.getJSONObject(k);

                                                if (currentAttendee.getString("token").equals(userToken)) {
                                                    switch (status) {
                                                        case "COMPLETE":
                                                            eventStatus = Status.COMPLETE;
                                                            break;
                                                        case "EXPIRED":
                                                            eventStatus = Status.EXPIRED;
                                                            break;
                                                        default:
                                                            eventStatus = Status.WAITING_PAYEE;
                                                            break;
                                                    }
                                                    eventPayee = payeeName;
                                                    break paymentGroupsLoop;
                                                }
                                            }
                                        }
                                    }
                                    attendingEvents.add(new AttendingEventCard(seriesName, subtitle, imageURL, eventStatus, eventPayee));
                                }

                                initializeList(attendingEvents);

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
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", sharedPreferences.getString(EqualiserSharedPreferences.SESSION_TOKEN, ""));
                return params;
            }
        };

        stringRequest.setTag(VOLLEY_TAG);
        stringRequest.setShouldCache(false);
        requestQueue.add(stringRequest);
    }

    /**
     * Adapter for RecyclerView.
     */
    class AttendingEventCardTabAdapter extends RecyclerView.Adapter<AttendingEventCardTabAdapter.AttendingEventCardTabViewHolder> {
        private ArrayList<AttendingEventCard> list;

        AttendingEventCardTabAdapter(ArrayList<AttendingEventCard> data) {
            list = data;
        }

        @Override
        public AttendingEventCardTabViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.attending_tab_recycler_view_item, parent, false);
            return new AttendingEventCardTabViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final AttendingEventCardTabViewHolder holder, int position) {
            holder.textView.setText(list.get(position).getTitle());
            new DownloadImageTask(holder.imageView).execute(list.get(position).getImageUrl());
            holder.subTextView.setText(list.get(position).getSubtitle());
            holder.statusTextView.setText(String.format(list.get(position).getStatus().getText(), list.get(position).getPayee()));

            switch (list.get(position).getStatus()) {
                case COMPLETE:
                    holder.statusTextView.setTextColor(getResources().getColor(R.color.textColorGreen));
                    break;
                case EXPIRED:
                    holder.statusTextView.setTextColor(getResources().getColor(R.color.textColorRed));
                    break;
                case WAITING_PAYEE:
                case WAITING_PAYMENT:
                    holder.statusTextView.setTextColor(getResources().getColor(R.color.textColorYellow));
                    break;
                default:
                    holder.statusTextView.setTextColor(getResources().getColor(R.color.textColorBlack));
                    break;
            }

            holder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cardOnClick();
                }
            });

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
        class AttendingEventCardTabViewHolder extends RecyclerView.ViewHolder {

            TextView textView;
            ImageView imageView;
            TextView subTextView;
            TextView statusTextView;
            CardView cardView;

            AttendingEventCardTabViewHolder(View v) {
                super(v);
                textView = (TextView) v.findViewById(R.id.event_title_text_view);
                imageView = (ImageView) v.findViewById(R.id.image_view);
                subTextView = (TextView) v.findViewById(R.id.event_sub_text_view);
                statusTextView = (TextView) v.findViewById(R.id.status_text_view);
                cardView = (CardView) v.findViewById(R.id.card_view);
            }
        }
    }
}
