package uk.co.boconi.emil.obd2aa;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.util.List;

import static java.lang.Integer.parseInt;
import uk.co.boconi.emil.obd2aa.AppSettings;

/**
 * Created by Emil on 31/08/2017.
 */

public class PIDSearch extends AlertDialog implements View.OnClickListener {

    private ListView list;
    private EditText filterText = null;
    ArrayAdapter<PidList> adapter = null;
    private static final String TAG = "OBD2AA";


    public PIDSearch(final Context context, List<PidList> pidlist, final String i, final AppSettings mAppSettings, final TpmsSettings tpmsSettings) {
        super(context);

        /** Design the dialog in main.xml file */
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.pidlist, null);


        AlertDialog.Builder b = new Builder(context);
        b.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

            } });
        b.setView(alertLayout);
        b.setTitle("Select PID");
        b.setCancelable(true);

        final android.support.v7.app.AlertDialog dialog = b.show();


        filterText = (EditText) alertLayout.findViewById(R.id.EditBox);
        filterText.addTextChangedListener(filterTextWatcher);
        list = (ListView) alertLayout.findViewById(R.id.List);
        adapter = new ArrayAdapter<PidList>(context, android.R.layout.simple_list_item_1, pidlist);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                ;
                Log.d(TAG, "Selected Item is = "+list.getItemAtPosition(position) + " position: " + position + "ID: " );
                if (mAppSettings!=null)
                    mAppSettings.updateview(list.getItemAtPosition(position).toString(),parseInt(i));
                else
                    tpmsSettings.updateview(list.getItemAtPosition(position).toString(),parseInt(i));
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                v.clearFocus();
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                dialog.dismiss();

            }
        });
    }
    @Override
    public void onClick(View v) {

    }
    private TextWatcher filterTextWatcher = new TextWatcher() {

        public void afterTextChanged(Editable s) {
        }

        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            adapter.getFilter().filter(s);
        }
    };
    @Override
    public void onStop(){
        filterText.removeTextChangedListener(filterTextWatcher);
    }
}
