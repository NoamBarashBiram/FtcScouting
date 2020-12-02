package com.noam.ftcscouting.ui.myMatches;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.noam.ftcscouting.R;
import com.noam.ftcscouting.alarm.Alarm;
import com.noam.ftcscouting.alarm.AlarmReceiver;
import com.noam.ftcscouting.alarm.AlarmRepository;
import com.noam.ftcscouting.database.FieldsConfig;
import com.noam.ftcscouting.database.FirebaseHandler;
import com.noam.ftcscouting.ui.teams.TeamsFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.noam.ftcscouting.alarm.AlarmReceiver.EXTRA_EVENT;
import static com.noam.ftcscouting.alarm.RescheduleAlarmsService.repo;
import static com.noam.ftcscouting.ui.events.EventsFragment.eventsString;


public class MyMatchesFragment extends Fragment {

    private static final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private static final String TAG = "MyMatchesFragment";

    private LinearLayout alarmsView;
    private String event;
    private SharedPreferences preferences;
    private final ArrayList<String> teams = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_my_matches, container, false);
        root.findViewById(R.id.addMatch).setOnClickListener(this::addMatch);
        alarmsView = root.findViewById(R.id.alarmsView);
        return root;
    }

    private void addMatch(View view) {
        View dialogView = getLayoutInflater().inflate(R.layout.team_choosing_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Create a Reminder")
                .setView(dialogView)
                .setPositiveButton("Done", (dialog1, which) -> {
                })
                .setNegativeButton("Cancel", (dialog1, which) -> {
                })
                .setNeutralButton("Edit Date", (dialog1, which) -> {
                })
                .create();

        Spinner team = dialogView.findViewById(R.id.teamSpinner),
                match = dialogView.findViewById(R.id.matchSpinner);

        team.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == parent.getCount()) return;
                match.setAdapter(new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        FirebaseHandler.snapshot.child(eventsString)
                                .child(event)
                                .child(FirebaseHandler.fireKey((String) team.getSelectedItem()))
                                .child(FieldsConfig.matches)
                                .getValue(String.class)
                                .split(";")
                ));
                match.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        team.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                teams
        ));

        TimePicker picker = dialogView.findViewById(R.id.timePicker);
        picker.setIs24HourView(true);

        dialog.show();

        Button saveBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        saveBtn.setOnClickListener(v1 -> {
            saveReminder(FirebaseHandler.fireKey((String) team.getSelectedItem()),
                    (String) match.getSelectedItem(),
                    parseTime(picker));
            dialog.dismiss();
        });

        dialog.getButton(DialogInterface.BUTTON_NEUTRAL)
                .setOnClickListener(v1 ->
                        openDateDialog((String) team.getSelectedItem(),
                                (String) match.getSelectedItem(),
                                parseTime(picker),
                                null));
    }

    private long parseTime(TimePicker picker) {
        Calendar calendar = Calendar.getInstance();
        int hour = picker.getHour();
        int minute = picker.getMinute();

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute - 1);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private void saveReminder(String team, String match, long time) {
        int id = preferences.getInt(AlarmReceiver.EXTRA_ID, -1);
        Alarm alarm = new Alarm(++id, time, event, team, match);
        alarm.schedule(getContext());
        preferences.edit().putInt(AlarmReceiver.EXTRA_ID, id).apply();
        new Thread(() -> {
            repo.insert(alarm);
            updateUI();
        }).start();
    }

    private void openDateDialog(String team, String match, long time, Alarm alarm) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        DatePickerDialog picker = new DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) -> {
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        picker.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
                (dialog, which) -> {
                }
        );
        picker.setButton(DialogInterface.BUTTON_POSITIVE, "Save",
                (dialog, which) -> {
                    calendar.set(Calendar.YEAR, picker.getDatePicker().getYear());
                    calendar.set(Calendar.MONTH, picker.getDatePicker().getMonth());
                    calendar.set(Calendar.DAY_OF_MONTH, picker.getDatePicker().getDayOfMonth());
                    if (alarm != null) {
                        updateAlarm(alarm, calendar.getTimeInMillis());
                    } else {
                        saveReminder(team, match, calendar.getTimeInMillis());
                    }
                });
        picker.setCancelable(false);
        picker.show();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        event = getActivity().getIntent().getStringExtra(EXTRA_EVENT);
        if (repo == null) {
            new Thread(() -> repo = new AlarmRepository(getContext())).start();
        }
        preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        new Thread(this::updateUI).start();
        for (String team : TeamsFragment.teams) {
            teams.add(FirebaseHandler.unFireKey(team));
        }
    }

    private void updateUI() {
        List<Alarm> alarms = repo.findByEvent(event);

        if (alarms.size() > 0) {
            runOnUiThread(() -> alarmsView.removeAllViews());
            for (final Alarm alarm : alarms) {
                TextView alarmView = new TextView(getContext());
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                alarmView.setLayoutParams(params);
                alarmView.setTextSize(20);
                alarmView.setPaddingRelative(16, 16, 16, 16);
                alarmView.setBackgroundResource(R.drawable.ripple_background);
                alarmView.setOnClickListener(v -> onAlarmClicked(alarm));
                String time = formatter.format(alarm.time + 60 * 1000);
                alarmView.setText(String.format("%s's %s in %s", FirebaseHandler.unFireKey(alarm.team), alarm.match, time));
                runOnUiThread(() -> alarmsView.addView(alarmView));
            }
        } else {
            TextView noMatches = createNoMatches();
            runOnUiThread(() -> {
                alarmsView.removeAllViews();
                alarmsView.addView(noMatches);
            });
        }
    }

    private void onAlarmClicked(Alarm alarm) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(alarm.time);

        AtomicInteger
                hour = new AtomicInteger(calendar.get(Calendar.HOUR_OF_DAY)),
                min = new AtomicInteger(calendar.get(Calendar.MINUTE + 1));

        TimePickerDialog picker = new TimePickerDialog(getContext(),
                (view, hourOfDay, minute) -> {
                    hour.set(hourOfDay);
                    min.set(minute);
                },
                hour.get(),
                min.get(),
                true
        );
        picker.setButton(DialogInterface.BUTTON_NEGATIVE, "Delete Reminder",
                (dialog, which) -> {
                    alarm.cancelAlarm(getContext());
                    new Thread(() -> {
                        repo.remove(alarm);
                        updateUI();
                    }).start();
                }

        );
        picker.setButton(DialogInterface.BUTTON_POSITIVE, "Save", (dialog, which) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hour.get());
            calendar.set(Calendar.MINUTE, min.get() + 1);
            updateAlarm(alarm, calendar.getTimeInMillis());
        });
        picker.setButton(DialogInterface.BUTTON_NEUTRAL, "Edit Date",
                (dialog, which) -> openDateDialog(null, null, alarm.time, alarm));

        picker.show();
    }

    private void updateAlarm(Alarm alarm, long time) {
        alarm.cancelAlarm(getContext());
        Alarm newAlarm = new Alarm(alarm.id, time, alarm.event, alarm.team, alarm.match);
        newAlarm.schedule(getContext());
        new Thread(() -> {
            repo.update(newAlarm);
            updateUI();
        }).start();
    }

    private TextView createNoMatches() {
        TextView noMatches = new TextView(getContext());
        noMatches.setGravity(Gravity.CENTER);
        noMatches.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                ));
        noMatches.setTextSize(60);
        noMatches.setText(R.string.no_reminders);
        return noMatches;
    }

    private void runOnUiThread(Runnable action) {
        getActivity().runOnUiThread(action);
    }
}