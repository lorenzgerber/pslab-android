package org.fossasia.pslab.activity;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.fossasia.pslab.R;
import org.fossasia.pslab.communication.ScienceLab;
import org.fossasia.pslab.others.ScienceLabCommon;

import java.text.DecimalFormat;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import it.beppi.knoblibrary.Knob;

/**
 * Created by Abhinav Raj on 26/5/18.
 */

public class MultimeterActivity extends AppCompatActivity {

    private ScienceLab scienceLab;

    @BindView(R.id.quantity)
    TextView quantity;
    @BindView(R.id.unit)
    TextView unit;
    @BindView(R.id.knob_center)
    TextView knobSelection;
    @BindView(R.id.knobs)
    Knob knob;
    @BindView(R.id.reset)
    Button reset;
    @BindView(R.id.read)
    Button read;
    @BindView(R.id.capacitance)
    Button readCapacitance;
    @BindView(R.id.resistance)
    Button readResistance;

    private int knobState;
    private String[] knobMarker;

    public static final String PREFS_NAME = "customDialogPreference";
    public CheckBox dontShowAgain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        howToConnectDialog(getString(R.string.multimeter_dialog_heading), getString(R.string.multimeter_dialog_text), R.drawable.multimeter_circuit, getString(R.string.multimeter_dialog_description));
        setContentView(R.layout.activity_multimeter);
        ButterKnife.bind(this);
        scienceLab = ScienceLabCommon.scienceLab;
        knobMarker = getResources().getStringArray(org.fossasia.pslab.R.array.multimeter_knob_states);

        readResistance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scienceLab.isConnected()) {
                    DecimalFormat resistanceFormat = new DecimalFormat("#.##");
                    Double resistance;
                    Double avgResistance = 0.0;
                    int loops = 20;
                    for (int i = 0; i < loops; i++) {
                        resistance = scienceLab.getResistance();
                        if (resistance == null) {
                            avgResistance = null;
                            break;
                        } else {
                            avgResistance = avgResistance + resistance / loops;
                        }
                    }
                    String resistanceUnit;
                    String Resistance = "";
                    if (avgResistance == null) {
                        Resistance = "Infinity";
                        resistanceUnit = "Ohms";
                    } else {
                        if (avgResistance > 10e5) {
                            Resistance = resistanceFormat.format((avgResistance / 10e5));
                            resistanceUnit = "MOhms";
                        } else if (avgResistance > 10e2) {
                            Resistance = resistanceFormat.format((avgResistance / 10e2));
                            resistanceUnit = "kOhms";
                        } else if (avgResistance > 1) {
                            Resistance = resistanceFormat.format(avgResistance);
                            resistanceUnit = "Ohms";
                        } else {
                            Resistance = "Cannot measure!";
                            resistanceUnit = "";
                        }
                    }
                    quantity.setText(Resistance);
                    unit.setText(resistanceUnit);
                }
            }
        });
        readCapacitance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scienceLab.isConnected()) {
                    Double capacitance = scienceLab.getCapacitance();
                    quantity.setText(String.valueOf(capacitance));
                    unit.setText(R.string.capacitance_unit);
                }
            }
        });
        knob.setOnStateChanged(new Knob.OnStateChanged() {
            @Override
            public void onState(int state) {
                knobState = state;
                knobSelection.setText(knobMarker[knobState]);
            }
        });
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                quantity.setText("");
                unit.setText("");
            }
        });
        read.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (knobState < 4) {
                    if (scienceLab.isConnected()) {
                        scienceLab.countPulses(knobMarker[knobState]);
                        double pulseCount = scienceLab.readPulseCount();
                        quantity.setText(String.valueOf(pulseCount));
                        unit.setText("");
                    }
                } else if (knobState < 8) {
                    if (scienceLab.isConnected()) {
                        Double frequency = scienceLab.getFrequency(knobMarker[knobState], null);
                        quantity.setText(String.valueOf(frequency));
                        unit.setText(R.string.frequency_unit);
                    }
                } else {
                    if (scienceLab.isConnected()) {
                        quantity.setText(String.format(Locale.ENGLISH, "%.2f", scienceLab.getVoltage(knobMarker[knobState], 1)));
                        unit.setText(R.string.multimeter_voltage_unit);
                    }
                }
            }
        });

    }

    @SuppressLint("ResourceType")
    public void howToConnectDialog(String title, String intro, int iconID, String desc) {
        try {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.custom_dialog_box, null);
            final SharedPreferences settings = this.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            Boolean skipMessage = settings.getBoolean("MultimeterSkipMessage", false);
            dontShowAgain = (CheckBox) dialogView.findViewById(R.id.toggle_show_again);
            final TextView heading_text = (TextView) dialogView.findViewById(R.id.custom_dialog_text);
            final TextView description_text = (TextView) dialogView.findViewById(R.id.description_text);
            final ImageView schematic = (ImageView) dialogView.findViewById(R.id.custom_dialog_schematic);
            final Button ok_button = (Button) dialogView.findViewById(R.id.dismiss_button);
            builder.setView(dialogView);
            builder.setTitle(title);
            heading_text.setText(intro);
            schematic.setImageResource(iconID);
            description_text.setText(desc);
            final AlertDialog dialog = builder.create();
            ok_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Boolean checkBoxResult = false;
                    if (dontShowAgain.isChecked())
                        checkBoxResult = true;
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("MultimeterSkipMessage", checkBoxResult);
                    editor.apply();
                    dialog.dismiss();
                }
            });
            if (!skipMessage)
                dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
