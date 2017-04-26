package events.equaliser.android;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Utility class.
 */
public class Utils {

    public static final String API_ENDPOINT = "https://api.equaliser.gebn.co.uk";

    /**
     * Converts a JSONArray to String[].
     * @param jsonArray The JSONArray.
     * @return String[] version of jsonArray.
     * @throws JSONException
     */
    public static String[] jsonArrayToStringArray(JSONArray jsonArray) throws JSONException {
        int arraySize = jsonArray.length();
        String[] stringArray = new String[arraySize];

        for (int i = 0; i < arraySize; i++) {
            stringArray[i] = (String) jsonArray.get(i);
        }

        return stringArray;
    }

    /**
     * Expands out a ListView so that all of its elements are visible.
     * @param listView The ListView.
     */
    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}
