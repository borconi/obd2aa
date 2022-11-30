package uk.co.borconi.emil.obd2aa.preference;

import static uk.co.borconi.emil.obd2aa.MainActivity.pidlist;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;

import uk.co.borconi.emil.obd2aa.R;
import uk.co.borconi.emil.obd2aa.pid.PidList;

public class PidListPreference extends ListPreference implements View.OnClickListener, TextWatcher {

    ArrayAdapter<PidList> adapter = null;

    public PidListPreference(Context context) {
        super(context);
    }

    public PidListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
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

    @Override
    public void onClick(View view) {

    }

    @Override
    protected void onClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = (LayoutInflater) getContext().
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View alertLayout = inflater.inflate(R.layout.pid_list, null);

        EditText filterText = alertLayout.findViewById(R.id.EditBox);
        filterText.addTextChangedListener(this);

        ListView list = alertLayout.findViewById(R.id.List);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_single_choice, pidlist);
        list.setAdapter(adapter);

        PidList selectedpid = null;
        for (PidList pid : pidlist) {
            if ((pid.getPid() + "__" + pid.getShortPidName() + "__" + pid.getUnit()).equals(getValue())) { // TODO This is ugly
                selectedpid = pid;
                break;
            }
        }
        if (selectedpid != null) {
            list.setItemChecked(adapter.getPosition(selectedpid), true);
        }

        builder.setView(alertLayout);
        builder.setNegativeButton(getContext().getString(R.string.cancel_button_label), null);
        builder.setTitle(getContext().getString(R.string.choose_button_label) + " PID");
        AlertDialog dialog = builder.create();
        list.setOnItemClickListener((a, v, position, id) -> {
            PidList pid = (PidList) a.getItemAtPosition(position);
            setValue(pid.getPid() + "__" + pid.getShortPidName() + "__" + pid.getUnit()); // TODO This is ugly

            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            dialog.dismiss();
        });
        dialog.show();
    }

    public static class Builder extends AlertDialog.Builder {

        public Builder(@NonNull Context context) {
            super(context);
        }
    }
}
