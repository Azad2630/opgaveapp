package com.example.opgaveapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // DATA LISTER
    private List<Task> taskList = new ArrayList<>();
    private List<Habit> habitList = new ArrayList<>();
    private List<Transaction> transactionList = new ArrayList<>();
    private int currentBalance = 0;

    // UI Elementer
    private LinearLayout layoutTasks, layoutHabits, layoutMoney;
    private LinearLayout containerActiveTasks, containerDoneTasks, containerHabits, containerHistory;
    private TextView tvBalance;

    // Gemme Værktøjer
    private SharedPreferences sharedPreferences;
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Find UI elementer fra XML
        layoutTasks = findViewById(R.id.layoutTasks);
        layoutHabits = findViewById(R.id.layoutHabits);
        layoutMoney = findViewById(R.id.layoutMoney);

        containerActiveTasks = findViewById(R.id.containerActiveTasks);
        containerDoneTasks = findViewById(R.id.containerDoneTasks);
        containerHabits = findViewById(R.id.containerHabits);
        containerHistory = findViewById(R.id.containerHistory);
        tvBalance = findViewById(R.id.tvBalance);

        // 2. Navigation Knapper
        findViewById(R.id.btnNavTasks).setOnClickListener(v -> showTab(1));
        findViewById(R.id.btnNavHabits).setOnClickListener(v -> showTab(2));
        findViewById(R.id.btnNavMoney).setOnClickListener(v -> showTab(3));

        // 3. Indlæs gemt data
        loadData();

        // 4. Opsætning af knapper (Tilføj opgave, Vane, Køb)
        setupButtons();

        // 5. Opdater visning
        refreshAllUI();
    }

    // NAVIGATION
    private void showTab(int tabIndex) {
        layoutTasks.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        layoutHabits.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);
        layoutMoney.setVisibility(tabIndex == 3 ? View.VISIBLE : View.GONE);
    }

    // KNAPPER LOGIK
    private void setupButtons() {
        // A) Tilføj Opgave
        EditText etTaskInput = findViewById(R.id.etTaskInput);
        findViewById(R.id.btnAddTask).setOnClickListener(v -> {
            String title = etTaskInput.getText().toString();
            if (!title.isEmpty()) {
                taskList.add(new Task(title)); // Opretter med dags dato
                etTaskInput.setText("");
                saveData();
                refreshTasksUI();
            }
        });

        // B) Tilføj Vane
        EditText etHabitInput = findViewById(R.id.etHabitInput);
        findViewById(R.id.btnAddHabit).setOnClickListener(v -> {
            String name = etHabitInput.getText().toString();
            if (!name.isEmpty()) {
                habitList.add(new Habit(name)); // Opretter med startdato
                etHabitInput.setText("");
                saveData();
                refreshHabitsUI();
            }
        });

        // C) Registrer Køb
        EditText etSpendDesc = findViewById(R.id.etSpendDesc);
        EditText etSpendAmount = findViewById(R.id.etSpendAmount);
        findViewById(R.id.btnSpend).setOnClickListener(v -> {
            try {
                int amount = Integer.parseInt(etSpendAmount.getText().toString());
                String desc = etSpendDesc.getText().toString();
                if (amount > 0 && !desc.isEmpty()) {
                    updateBalance(-amount, desc); // Minus beløb
                    etSpendAmount.setText("");
                    etSpendDesc.setText("");
                    Toast.makeText(this, "Køb registreret!", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Indtast et gyldigt tal", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // UI OPDATERING
    private void refreshAllUI() {
        refreshTasksUI();
        refreshHabitsUI();
        refreshMoneyUI();
    }

    private void refreshTasksUI() {
        containerActiveTasks.removeAllViews();
        containerDoneTasks.removeAllViews();

        for (Task task : taskList) {
            // Række container
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(10, 20, 10, 20);

            // Tekst container (Titel + Dato)
            LinearLayout textContainer = new LinearLayout(this);
            textContainer.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            textContainer.setLayoutParams(params);

            TextView tvTitle = new TextView(this);
            String icon = task.isDone ? "✅ " : "⭕ ";
            tvTitle.setText(icon + task.title);
            tvTitle.setTextSize(16f);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvDate = new TextView(this);
            tvDate.setText("   Oprettet: " + task.creationDate);
            tvDate.setTextSize(12f);
            tvDate.setTextColor(Color.GRAY);

            textContainer.addView(tvTitle);
            textContainer.addView(tvDate);

            // Knap (Færdig / Slet)
            Button btnAction = new Button(this);
            btnAction.setText(task.isDone ? "Slet" : "Færdig");
            btnAction.setOnClickListener(v -> {
                if (!task.isDone) {
                    task.isDone = true;
                    updateBalance(10, "Opgave: " + task.title);
                } else {
                    taskList.remove(task);
                }
                saveData();
                refreshAllUI();
            });

            row.addView(textContainer);
            row.addView(btnAction);

            if (task.isDone) {
                containerDoneTasks.addView(row);
            } else {
                containerActiveTasks.addView(row);
            }
        }
    }

    private void refreshHabitsUI() {
        containerHabits.removeAllViews();

        for (Habit habit : habitList) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(20, 20, 20, 20);
            row.setBackgroundColor(Color.parseColor("#F5F5F5")); // grå

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, 20);
            row.setLayoutParams(rowParams);

            // Titel
            TextView tvTitle = new TextView(this);
            tvTitle.setText("🌱 " + habit.name);
            tvTitle.setTextSize(18f);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

            // Start Dato
            TextView tvStart = new TextView(this);
            tvStart.setText("Startet: " + habit.startDate);
            tvStart.setTextSize(12f);
            tvStart.setTextColor(Color.GRAY);
            tvStart.setPadding(0, 0, 0, 10);

            // Streak info
            TextView tvStreak = new TextView(this);
            tvStreak.setText("🔥 Streak: " + habit.streak + " dage i træk");
            tvStreak.setTextSize(14f);
            tvStreak.setPadding(0, 0, 0, 10);

            // Knap
            Button btnCheck = new Button(this);
            boolean isDoneToday = LocalDate.now().toString().equals(habit.lastDoneDate);

            if (isDoneToday) {
                btnCheck.setText("✅ Udført i dag");
                btnCheck.setEnabled(false);
            } else {
                btnCheck.setText("Marker som udført (+2 kr)");
                btnCheck.setOnClickListener(v -> {
                    habit.streak++;
                    habit.lastDoneDate = LocalDate.now().toString();
                    updateBalance(2, "Vane bonus: " + habit.name);
                    saveData();
                    refreshAllUI();
                });
            }

            row.addView(tvTitle);
            row.addView(tvStart);
            row.addView(tvStreak);
            row.addView(btnCheck);
            containerHabits.addView(row);
        }
    }

    private void refreshMoneyUI() {
        tvBalance.setText("💰 Saldo: " + currentBalance + " kr");
        containerHistory.removeAllViews();

        for (int i = transactionList.size() - 1; i >= 0; i--) {
            Transaction t = transactionList.get(i);
            TextView tv = new TextView(this);
            String sign = t.amount >= 0 ? "+" : "";
            tv.setText(t.date + ": " + sign + t.amount + " kr (" + t.desc + ")");
            tv.setPadding(10, 10, 10, 10);
            if(t.amount < 0) tv.setTextColor(Color.RED);
            else tv.setTextColor(Color.parseColor("#008000")); // Grøn

            containerHistory.addView(tv);
        }
    }

    private void updateBalance(int amount, String description) {
        currentBalance += amount;
        transactionList.add(new Transaction(amount, description));
        saveData();
        refreshMoneyUI();
    }

    // DATA GEM/HENT
    private void saveData() {
        sharedPreferences = getSharedPreferences("OpgaveAppDB", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("tasks", gson.toJson(taskList));
        editor.putString("habits", gson.toJson(habitList));
        editor.putString("transactions", gson.toJson(transactionList));
        editor.putInt("balance", currentBalance);

        editor.apply();
    }

    private void loadData() {
        sharedPreferences = getSharedPreferences("OpgaveAppDB", MODE_PRIVATE);

        currentBalance = sharedPreferences.getInt("balance", 0);

        String tasksJson = sharedPreferences.getString("tasks", null);
        String habitsJson = sharedPreferences.getString("habits", null);
        String transJson = sharedPreferences.getString("transactions", null);

        Type taskListType = new TypeToken<ArrayList<Task>>(){}.getType();
        Type habitListType = new TypeToken<ArrayList<Habit>>(){}.getType();
        Type transListType = new TypeToken<ArrayList<Transaction>>(){}.getType();

        if (tasksJson != null) taskList = gson.fromJson(tasksJson, taskListType);
        if (habitsJson != null) habitList = gson.fromJson(habitsJson, habitListType);
        if (transJson != null) transactionList = gson.fromJson(transJson, transListType);
    }

    // DATAMODELLER

    // Opgave model
    private static class Task {
        String title;
        boolean isDone;
        String creationDate; // Dato

        Task(String title) {
            this.title = title;
            this.isDone = false;
            this.creationDate = LocalDate.now().toString(); // Sætter dags dato automatisk
        }
    }

    // Vane model
    private static class Habit {
        String name;
        int streak;
        String lastDoneDate;
        String startDate; // Dato

        Habit(String name) {
            this.name = name;
            this.streak = 0;
            this.lastDoneDate = "";
            this.startDate = LocalDate.now().toString(); // Sætter startdato automatisk
        }
    }

    // Transaktion model
    private static class Transaction {
        int amount;
        String desc;
        String date;
        Transaction(int amount, String desc) {
            this.amount = amount;
            this.desc = desc;
            this.date = LocalDate.now().toString();
        }
    }
}