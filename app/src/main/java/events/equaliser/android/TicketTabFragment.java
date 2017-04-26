package events.equaliser.android;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Dimension;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.InputStream;

/**
 * Displays the user's ticket on the screen.
 */
public class TicketTabFragment extends Fragment {

    private static final String TAG = TicketTabFragment.class.getSimpleName();
    // AXTEC barcodes are supposed to be more readable than many types; clarity is key to
    // prevent mis-scans: http://www.scandit.com/types-barcodes-choosing-right-barcode/
    private static final BarcodeFormat BARCODE_FORMAT = BarcodeFormat.AZTEC;
    private static final Dimension BARCODE_DIMENSIONS = new Dimension(800, 800);
    private SharedPreferences sharedPreferences;

    private static Bitmap encodeAsBitmap(String data) throws WriterException {
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix matrix = writer.encode(data, BARCODE_FORMAT, BARCODE_DIMENSIONS.getWidth(),
                BARCODE_DIMENSIONS.getHeight());
        int[] pixels = new int[matrix.getWidth() * matrix.getHeight()];

        for (int y = 0; y < matrix.getHeight(); y++) {
            int offset = y * matrix.getWidth();
            for (int x = 0; x < matrix.getWidth(); x++) {
                pixels[offset + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(matrix.getWidth(), matrix.getHeight(),
                Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, matrix.getWidth(), 0, 0, matrix.getWidth(), matrix.getHeight());
        return bitmap;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        sharedPreferences = EqualiserSharedPreferences.getEqualiserSharedPreferences(getContext());
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.ticket_tab_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageView imageView = (ImageView) view.findViewById(R.id.ticket_image);
        String ticketToken = sharedPreferences.getString(EqualiserSharedPreferences.TOKEN, "");
        new RenderTicketTask(imageView, ticketToken).execute();
    }

    private class RenderTicketTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView imageView;
        private String data;
        private ProgressDialog dialog;

        RenderTicketTask(ImageView imageView, String data) {
            this.imageView = imageView;
            this.data = data;
        }

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(getActivity());
            dialog.setTitle("Loading ticket...");
            dialog.show();
        }

        protected Bitmap doInBackground(String... params) {
            try {
                return encodeAsBitmap(data);
            } catch (WriterException e) {
                Log.e(TAG, "Error rendering ticket", e);
                return null;
            }
        }

        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                imageView.setImageBitmap(result);
            } else {
                Toast.makeText(getContext(), "Failed to load ticket", Toast.LENGTH_LONG).show();
            }

            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }
}