package uk.co.borconi.emil.obd2aa.preference;

import static uk.co.borconi.emil.obd2aa.MainActivity.pidlist;

import android.content.Context;
import android.content.DialogInterface;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;

import uk.co.borconi.emil.obd2aa.R;
import uk.co.borconi.emil.obd2aa.pid.PidList;

public class PIDSearch extends AlertDialog implements View.OnClickListener, TextWatcher {

    private static final String TAG = "OBD2AA";
    ArrayAdapter<PidList> adapter = null;
    private final ListView list;
    private EditText filterText = null;

    public PIDSearch(final Context context, ListPreference preference) {
        super(context);

        /** Design the dialog in main.xml file */
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.pid_list, null);


        AlertDialog.Builder b = new Builder(context);
        b.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

            }
        });

        b.setView(alertLayout);
        b.setTitle("Select PID");
        b.setCancelable(true);


        filterText = alertLayout.findViewById(R.id.EditBox);

        filterText.addTextChangedListener(this);
        list = alertLayout.findViewById(R.id.List);
        adapter = new ArrayAdapter<PidList>(context, android.R.layout.simple_list_item_single_choice, pidlist);

        list.setAdapter(adapter);

        AlertDialog dialog = b.create();

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                PidList pid = (PidList) a.getItemAtPosition(position);
                preference.setValue(pid.getPid());
                Log.d(TAG, "Selected Item is = " + list.getItemAtPosition(position) + " position: " + position + "ID: " + pid.getPid());

                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                v.clearFocus();
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                dialog.dismiss();

            }
        });
        dialog.show();
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        adapter.getFilter().filter(charSequence);
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }

    public void setFocus() {
        filterText.callOnClick();
        filterText.requestFocus();
    }

    @Override
    public void onClick(View v) {

    }

    public static class Builder extends AlertDialog.Builder {

        public Builder(@NonNull Context context) {
            super(context);
        }
    }


}