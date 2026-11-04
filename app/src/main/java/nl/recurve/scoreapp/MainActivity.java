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
    
    // Thema instellingen (Licht / Donker)
    boolean isDarkMode = false;
    ShprefsWrapper prefs; // Helper voor SharedPreferences

    // Kleurenpalet gebaseerd op Material Design 3 (Licht & Donker)
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
        
        // Lees donkere modus voorkeur uit SharedPreferences
        SharedPreferences sp = getSharedPreferences("app_prefs", 0);
        isDarkMode = sp.getBoolean("dark_mode", false);

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

    // ==================== 1. HOOFDSCHERM (HISTORIE & NIEUWE WEDSTRIJD) ====================
    void showHomeView() {
        ScrollView sc = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        root.setBackgroundColor(getBgColor());
        sc.addView(root);
        setContentView(sc);

        // Bovenste balk met titel en Theme Toggle
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(0);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = label("🏹 Recurve Score", 24, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        topBar.addView(title, titleParams);

        Button themeBtn = new Button(this);
        themeBtn.setText(isDarkMode ? "☀️ Licht" : "🌙 Donker");
        themeBtn.setOnClickListener(v -> toggleTheme());
        topBar.addView(themeBtn, new LinearLayout.LayoutParams(-2, -2));

        root.addView(topBar);

        Button newMatchBtn = createStyledButton("➕ Nieuwe Wedstrijd Starten", navyColor, Color.WHITE);
        newMatchBtn.setOnClickListener(v -> showConfigView());
        LinearLayout.LayoutParams btnLp = lp();
        btnLp.setMargins(0, dp(10), 0, dp(15));
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

    // ==================== 2. CONFIGURATIE SCHERM ====================
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
        String today = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.ROOT).format(new Date());
        dateInput.setText(today);
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

            String matchDate = dateInput.getText().toString().trim();

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("date", matchDate);
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

    // ==================== 3. SCORE INVOER SCHERM ====================
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

        // Bovenste balk met terugknop en theme toggle
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(0);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        Button backHome = createStyledButton("⬅ Terug", getCardColor(), getTextColor());
        topBar.addView(backHome, new LinearLayout.LayoutParams(-2, -2));
        backHome.setOnClickListener(v -> showHomeView());

        TextView title = label("🏹 Invoer & Klassement", 18, true);
        title.setGravity(17);
        LinearLayout.LayoutParams tParams = new LinearLayout.LayoutParams(0, -2, 1);
        topBar.addView(title, tParams);

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
            }
            public void onNothingSelected(AdapterView<?> a) {}
        });

        body = new LinearLayout(this);
        body.setOrientation(1);
        root.addView(body, lp());

        // Actieknoppen voor Export en Delen
        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(0);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(0, -2, 1);
        bLp.setMargins(0, dp(10), dp(4), dp(10));

        Button exportExcel = createStyledButton("📊 Excel", navyColor, Color.WHITE);
        exportExcel.setOnClickListener(v -> triggerFileExport("xlsx"));
        btnLayout.addView(exportExcel, bLp);

        Button exportPdf = createStyledButton("📄 PDF", navyColor, Color.WHITE);
        LinearLayout.LayoutParams bLp2 = new LinearLayout.LayoutParams(0, -2, 1);
        bLp2.setMargins(dp(4), dp(10), 0, dp(10));
        exportPdf.setOnClickListener(v -> triggerFileExport("pdf"));
        btnLayout.addView(exportPdf, bLp2);

        root.addView(btnLayout);

        Button shareBtn = createStyledButton("📲 Deel Resultaten via WhatsApp/Mail", Color.parseColor("#25D366"), Color.WHITE);
        LinearLayout.LayoutParams shLp = lp();
        shLp.setMargins(0, 0, 0, dp(15));
        shareBtn.setOnClickListener(v -> shareViaIntent());
        root.addView(shareBtn, shLp);

        root.addView(label("Klassement", 18, true));
        rankView = new TextView(this);
        rankView.setTextSize(13);
        rankView.setPadding(dp(12), dp(12), dp(12), dp(12));
        rankView.setBackgroundColor(getCardColor());
        rankView.setTextColor(getTextColor());
        root.addView(rankView, lp());

        endsViews = new TextView[numShooters][24];
        turnsViews = new TextView[numShooters][6];
        grandViews = new TextView[numShooters];

        renderShooterScores();
        updateLeaderboard();
    }

    void updatePickerAdapter() {
        if (picker == null) return;
        String[] names = new String[numShooters];
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT name FROM shooters WHERE match_id = ? ORDER BY shooter_index", new String[]{String.valueOf(currentMatchId)});
        int idx = 0;
        while (c.moveToNext() && idx < numShooters) {
            names[idx] = "Schutter " + (idx + 1) + " – " + c.getString(0);
            idx++;
        }
        c.close();

        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        picker.setAdapter(ad);
        if (selectedShooter >= numShooters) selectedShooter = 0;
        picker.setSelection(selectedShooter);
    }

    void renderShooterScores() {
        body.removeAllViews();

        LinearLayout nameLayout = new LinearLayout(this);
        nameLayout.setOrientation(0);
        nameLayout.setGravity(Gravity.CENTER_VERTICAL);
        TextView nameLbl = label("Naam: ", 14, false);
        EditText nameEdit = new EditText(this);
        nameEdit.setText(getShooterName(selectedShooter));
        nameEdit.setTextColor(getTextColor());
        nameEdit.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) {
                saveShooterName(selectedShooter, s.toString());
                updatePickerAdapter();
                updateLeaderboard();
            }
            public void afterTextChanged(Editable e) {}
        });
        nameLayout.addView(nameLbl, new LinearLayout.LayoutParams(-2, -2));
        nameLayout.addView(nameEdit, new LinearLayout.LayoutParams(-1, -2, 1));
        body.addView(nameLayout);

        // 6 beurten (0,1 = test, 2,3,4,5 = wedstrijd)
        for (int turn = 0; turn < 6; turn++) {
            String tTitle = (turn < 2) ? "Testbeurt " + (turn + 1) : "Beurt " + (turn - 1);
            body.addView(label(tTitle, 16, true));

            TableLayout tab = new TableLayout(this);
            tab.setStretchAllColumns(true);
            TableRow h = new TableRow(this);
            for (String z : new String[]{"End", "Pijl 1", "Pijl 2", "Pijl 3", "Totaal"}) h.addView(cell(z, true));
            tab.addView(h);

            for (int e = 0; e < 4; e++) {
                int ix = turn * 4 + e;
                TableRow row = new TableRow(this);
                row.addView(cell("" + (e + 1), false));

                for (int a = 0; a < 3; a++) {
                    final int ar = a;
                    final int fix = ix;
                    Spinner in = new Spinner(this);
                    
                    // In donkere modus geven we de spinner een goed contrast
                    in.setBackgroundColor(getInputBgColor());

                    String[] options = {"–", "X", "10", "9", "8", "7", "6", "5", "4", "3", "2", "1", "0", "M"};
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, options) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);
                            ((TextView) view).setTextColor(getInputTextColor());
                            return view;
                        }
                        @Override
                        public View getDropDownView(int position, View convertView, ViewGroup parent) {
                            View view = super.getDropDownView(position, convertView, parent);
                            view.setBackgroundColor(getInputBgColor());
                            ((TextView) view).setTextColor(getInputTextColor());
                            return view;
                        }
                    };
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    in.setAdapter(adapter);

                    String savedVal = getArrowScore(selectedShooter, fix, ar);
                    int pos = 0;
                    for (int j = 0; j < options.length; j++) {
                        if (options[j].equalsIgnoreCase(savedVal)) { pos = j; break; }
                    }
                    in.setSelection(pos);

                    in.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        boolean first = true;
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (first) { first = false; return; }
                            String val = options[position];
                            if (val.equals("–")) val = "";
                            saveArrowScore(selectedShooter, fix, ar, val);
                            calculateTotalsForShooter(selectedShooter);
                            updateLeaderboard();
                        }
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });

                    row.addView(in, new TableRow.LayoutParams(0, dp(42), 1));
                }

                endsViews[selectedShooter][ix] = cell("0", false);
                row.addView(endsViews[selectedShooter][ix]);
                tab.addView(row);
            }
            body.addView(tab);

            turnsViews[selectedShooter][turn] = label("", 14, true);
            turnsViews[selectedShooter][turn].setGravity(
