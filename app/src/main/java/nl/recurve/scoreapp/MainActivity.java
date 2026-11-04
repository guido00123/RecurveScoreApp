package nl.recurve.scoreapp;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.text.*;
import android.view.*;
import android.widget.*;
import android.content.*;
import android.database.sqlite.*;
import android.database.*;
import android.graphics.pdf.PdfDocument;
import android.graphics.Paint;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;

public class MainActivity extends Activity {
    DatabaseHelper dbHelper;
    long currentMatchId = -1;
    int numShooters = 4;
    boolean isDarkMode = false;

    int navyColor = Color.rgb(31, 78, 121);
    int paleColor = Color.rgb(221, 235, 247);

    int getBgColor() { return isDarkMode ? 0xff0f172a : 0xfff8fafc; }
    int getCardColor() { return isDarkMode ? 0xff1e293b : Color.WHITE; }
    int getTextColor() { return isDarkMode ? 0xfff1f5f9 : Color.rgb(31, 78, 121); }
    int getSubTextColor() { return isDarkMode ? 0xff94a3b8 : Color.DKGRAY; }
    int getInputBgColor() { return isDarkMode ? 0xff334155 : Color.WHITE; }
    int getInputTextColor() { return isDarkMode ? Color.WHITE : Color.BLACK; }

    LinearLayout root;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        dbHelper = new DatabaseHelper(this);
        isDarkMode = getSharedPreferences("app_prefs", 0).getBoolean("dark_mode", false);
        showHomeView();
    }

    void toggleTheme() {
        isDarkMode = !isDarkMode;
        getSharedPreferences("app_prefs", 0).edit().putBoolean("dark_mode", isDarkMode).apply();
        showHomeView();
    }

    int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    LinearLayout.LayoutParams lp() { return new LinearLayout.LayoutParams(-1, -2); }

    TextView label(String s, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(getTextColor());
        if (bold) t.setTypeface(null, 1);
        t.setPadding(0, dp(12), 0, dp(5));
        return t;
    }

    Button createStyledButton(String text, int bgColor, int fgColor) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setBackgroundColor(bgColor);
        btn.setTextColor(fgColor);
        btn.setPadding(dp(12), dp(12), dp(12), dp(12));
        return btn;
    }

    void showHomeView() {
        ScrollView sc = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        root.setBackgroundColor(getBgColor());
        sc.addView(root);
        setContentView(sc);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(0);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = label("🏹 Recurve Score", 24, true);
        topBar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        Button themeBtn = new Button(this);
        themeBtn.setText(isDarkMode ? "☀️ Licht" : "🌙 Donker");
        themeBtn.setOnClickListener(v -> toggleTheme());
        topBar.addView(themeBtn, new LinearLayout.LayoutParams(-2, -2));
        root.addView(topBar);

        Button newMatchBtn = createStyledButton("➕ Nieuwe Wedstrijd Starten", navyColor, Color.WHITE);
        LinearLayout.LayoutParams btnLp = lp();
        btnLp.setMargins(0, dp(10), 0, dp(15));
        newMatchBtn.setOnClickListener(v -> showConfigView());
        root.addView(newMatchBtn, btnLp);

        root.addView(label("Eerdere Wedstrijden", 18, true));
        LinearLayout historyList = new LinearLayout(this);
        historyList.setOrientation(1);
        root.addView(historyList, lp());

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, date, shooters_count FROM matches ORDER BY id DESC", null);
        
        if (cursor.getCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("Nog geen opgeslagen wedstrijden.");
            empty.setTextColor(getSubTextColor());
            empty.setPadding(0, dp(10), 0, 0);
            historyList.addView(empty);
        } else {
            while (cursor.moveToNext()) {
                long matchId = cursor.getLong(0);
                String date = cursor.getString(1);
                int count = cursor.getInt(2);

                Button matchBtn = createStyledButton("📅 " + (date.isEmpty() ? "Onbekende datum" : date) + " (" + count + " schutters)", getCardColor(), getTextColor());
                LinearLayout.LayoutParams mParams = lp();
                mParams.setMargins(0, 0, 0, dp(8));
                matchBtn.setOnClickListener(v -> {
                    currentMatchId = matchId;
                    numShooters = count;
                    showScoreView();
                });
                historyList.addView(matchBtn, mParams);
            }
        }
        cursor.close();
    }

    void showConfigView() {
        ScrollView sc = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        root.setBackgroundColor(getBgColor());
        sc.addView(root);
        setContentView(sc);

        root.addView(label("Nieuwe Wedstrijd Instellen", 22, true));
        root.addView(label("Aantal schutters (1 t/m 8):", 16, true));
        
        EditText countInput = new EditText(this);
        countInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        countInput.setText("4");
        countInput.setTextColor(getTextColor());
        root.addView(countInput, lp());

        root.addView(label("Datum wedstrijd:", 16, true));
        EditText dateInput = new EditText(this);
        dateInput.setText(new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.ROOT).format(new Date()));
        dateInput.setTextColor(getTextColor());
        root.addView(dateInput, lp());

        Button startBtn = createStyledButton("Start Score Invoeren", navyColor, Color.WHITE);
        LinearLayout.LayoutParams sLp = lp();
        sLp.setMargins(0, dp(15), 0, dp(10));
        startBtn.setOnClickListener(v -> {
            try {
                numShooters = Integer.parseInt(countInput.getText().toString().trim());
                if (numShooters < 1) numShooters = 1;
                if (numShooters > 8) numShooters = 8;
            } catch (Exception e) {
                numShooters = 4;
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("date", dateInput.getText().toString().trim());
            cv.put("shooters_count", numShooters);
            currentMatchId = db.insert("matches", null, cv);

            for (int i = 0; i < numShooters; i++) {
                ContentValues scv = new ContentValues();
                scv.put("match_id", currentMatchId);
                scv.put("shooter_index", i);
                scv.put("name", "Schutter " + (i + 1));
                db.insert("shooters", null, scv);
            }
            showScoreView();
        });
        root.addView(startBtn, sLp);

        Button backBtn = createStyledButton("Terug naar overzicht", getCardColor(), getTextColor());
        backBtn.setOnClickListener(v -> showHomeView());
        root.addView(backBtn, lp());
    }

    Spinner picker;
    LinearLayout body;
    int selectedShooter = 0;
    TextView rankView;
    TextView[][] endsViews;
    TextView[][] turnsViews;
    TextView[] grandViews;

    void showScoreView() {
        ScrollView sc = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        root.setBackgroundColor(getBgColor());
        sc.addView(root);
        setContentView(sc);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(0);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        Button backHome = createStyledButton("⬅ Terug", getCardColor(), getTextColor());
        backHome.setOnClickListener(v -> showHomeView());
        topBar.addView(backHome, new LinearLayout.LayoutParams(-2, -2));

        TextView title = label("🏹 Invoer & Klassement", 18, true);
        title.setGravity(17);
        topBar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        Button themeBtn = new Button(this);
        themeBtn.setText(isDarkMode ? "☀️" : "🌙");
        themeBtn.setOnClickListener(v -> toggleTheme());
        topBar.addView(themeBtn, new LinearLayout.LayoutParams(-2, -2));
        root.addView(topBar);

        root.addView(label("Selecteer schutter:", 16, true));
        picker = new Spinner(this);
        root.addView(picker, lp());
        updatePickerAdapter();

        picker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
                if (selectedShooter != pos) {
                    selectedShooter = pos;
                    renderShooterScores();
                }
