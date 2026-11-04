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

        Button clearBtn = createStyledButton("🗑️ Alle scores wissen voor deze wedstrijd", Color.parseColor("#dc2626"), Color.WHITE);
        LinearLayout.LayoutParams clLp = lp();
        clLp.setMargins(0, dp(15), 0, dp(15));
        clearBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Scores wissen?")
                .setMessage("Weet je zeker dat alle ingevoerde pijlscores voor deze wedstrijd gewist moeten worden? Namen en datum blijven behouden.")
                .setNegativeButton("Annuleren", null)
                .setPositiveButton("Wissen", (d, w) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.delete("scores", "match_id = ?", new String[]{String.valueOf(currentMatchId)});
                    renderShooterScores();
                    updateLeaderboard();
                    Toast.makeText(this, "Alle scores zijn gewist.", Toast.LENGTH_SHORT).show();
                })
                .show();
        });
        root.addView(clearBtn, clLp);

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
            turnsViews[selectedShooter][turn].setGravity(Gravity.RIGHT);
            body.addView(turnsViews[selectedShooter][turn]);
        }

        grandViews[selectedShooter] = label("", 18, true);
        grandViews[selectedShooter].setGravity(17);
        grandViews[selectedShooter].setBackgroundColor(paleColor);
        grandViews[selectedShooter].setTextColor(Color.rgb(31, 78, 121));
        body.addView(grandViews[selectedShooter]);

        calculateTotalsForShooter(selectedShooter);
    }

    TextView cell(String s, boolean head) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(head ? 12 : 14);
        t.setGravity(17);
        t.setPadding(dp(2), dp(6), dp(2), dp(6));
        t.setBackgroundColor(head ? paleColor : getCardColor());
        t.setTextColor(head ? Color.rgb(31, 78, 121) : getTextColor());
        if (head) t.setTypeface(null, 1);
        return t;
    }

    String getShooterName(int sIndex) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT name FROM shooters WHERE match_id = ? AND shooter_index = ?", new String[]{String.valueOf(currentMatchId), String.valueOf(sIndex)});
        String name = "Schutter " + (sIndex + 1);
        if (c.moveToFirst()) name = c.getString(0);
        c.close();
        return name;
    }

    void saveShooterName(int sIndex, String name) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        db.update("shooters", cv, "match_id = ? AND shooter_index = ?", new String[]{String.valueOf(currentMatchId), String.valueOf(sIndex)});
    }

    String getArrowScore(int sIndex, int endIdx, int arrowIdx) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT score FROM scores WHERE match_id = ? AND shooter_index = ? AND end_index = ? AND arrow_index = ?",
                new String[]{String.valueOf(currentMatchId), String.valueOf(sIndex), String.valueOf(endIdx), String.valueOf(arrowIdx)});
        String val = "";
        if (c.moveToFirst()) val = c.getString(0);
        c.close();
        return val;
    }

    void saveArrowScore(int sIndex, int endIdx, int arrowIdx, String val) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("match_id", currentMatchId);
        cv.put("shooter_index", sIndex);
        cv.put("end_index", endIdx);
        cv.put("arrow_index", arrowIdx);
        cv.put("score", val);
        db.insertWithOnConflict("scores", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    int parseVal(String v) {
        if (v.equalsIgnoreCase("X")) return 10;
        try { int n = Integer.parseInt(v); return (n >= 0 && n <= 10) ? n : 0; } catch (Exception e) { return 0; }
    }

    void calculateTotalsForShooter(int s) {
        int compTotal = 0;
        for (int t = 0; t < 6; t++) {
            int turnSum = 0;
            for (int e = t * 4; e < t * 4 + 4; e++) {
                int endSum = 0;
                for (int a = 0; a < 3; a++) {
                    endSum += parseVal(getArrowScore(s, e, a));
                }
                if (endsViews[s][e] != null) endsViews[s][e].setText("" + endSum);
                turnSum += endSum;
            }
            if (turnsViews[s][t] != null) {
                String labelStr = (t < 2) ? "Totaal testbeurt " + (t + 1) + ": " : "Totaal beurt " + (t - 1) + ": ";
                turnsViews[s][t].setText(labelStr + turnSum + " / 120");
            }
            if (t >= 2) compTotal += turnSum;
        }
        if (grandViews[s] != null) grandViews[s].setText("EINDTOTAAL: " + compTotal + " / 480");
    }

    void updateLeaderboard() {
        if (rankView == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "%-12s %5s\n", "Schutter", "Totaal"));

        for (int s = 0; s < numShooters; s++) {
            String name = getShooterName(s);
            if (name.length() > 12) name = name.substring(0, 12);

            int compTotal = 0;
            for (int t = 2; t < 6; t++) {
                for (int e = t * 4; e < t * 4 + 4; e++) {
                    for (int a = 0; a < 3; a++) {
                        compTotal += parseVal(getArrowScore(s, e, a));
                    }
                }
            }
            sb.append(String.format(Locale.ROOT, "%-12s %5d\n", name, compTotal));
        }

        rankView.setTypeface(android.graphics.Typeface.MONOSPACE);
        rankView.setText(sb.toString());
    }

    String exportType = "xlsx";

    void triggerFileExport(String type) {
        exportType = type;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (type.equals("xlsx")) {
            intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            intent.putExtra(Intent.EXTRA_TITLE, "RecurveScore.xlsx");
        } else {
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_TITLE, "RecurveScore.pdf");
        }
        startActivityForResult(intent, 2002);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2002 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                OutputStream out = getContentResolver().openOutputStream(data.getData());
                if (out != null) {
                    if (exportType.equals("xlsx")) writeExcel(out);
                    else writePdf(out);
                    out.close();
                    Toast.makeText(this, "Bestand succesvol opgeslagen!", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Export mislukt: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    void shareViaIntent() {
        StringBuilder sb = new StringBuilder("🏹 Recurve Score Uitslag:\n\n");
        for (int s = 0; s < numShooters; s++) {
            int total = 0;
            for (int t = 2; t < 6; t++) {
                for (int e = t * 4; e < t * 4 + 4; e++) {
                    for (int a = 0; a < 3; a++) total += parseVal(getArrowScore(s, e, a));
                }
            }
            sb.append(getShooterName(s)).append(": ").append(total).append(" / 480 punten\n");
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, "Deel uitslag via"));
    }

    void writePdf(OutputStream output) throws IOException {
        PdfDocument pdf = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdf.startPage(pageInfo);

        android.graphics.Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(14);

        int y = 40;
        canvas.drawText("Recurve Score - Wedstrijduitslag", 40, y, paint);
        y += 30;

        for (int s = 0; s < numShooters; s++) {
            int total = 0;
            for (int t = 2; t < 6; t++) {
                for (int e = t * 4; e < t * 4 + 4; e++) {
                    for (int a = 0; a < 3; a++) total += parseVal(getArrowScore(s, e, a));
                }
            }
            canvas.drawText("Schutter: " + getShooterName(s) + " - Totaal: " + total + " / 480", 40, y, paint);
            y += 25;
        }

        pdf.finishPage(page);
        pdf.writeTo(output);
        pdf.close();
    }

    void writeExcel(OutputStream output) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(output);
        List<String[]> detail = new ArrayList<>();
        detail.add(new String[]{"Schutter", "Beurt", "End", "Pijl 1", "Pijl 2", "Pijl 3", "Totaal"});

        for (int s = 0; s < numShooters; s++) {
            String name = getShooterName(s);
            for (int e = 0; e < 24; e++) {
                int turn = e / 4;
                String[] a = {getArrowScore(s, e, 0), getArrowScore(s, e, 1), getArrowScore(s, e, 2)};
                int etot = parseVal(a[0]) + parseVal(a[1]) + parseVal(a[2]);
                detail.add(new String[]{name, String.valueOf(turn + 1), String.valueOf((e % 4) + 1), a[0], a[1], a[2], String.valueOf(etot)});
            }
        }

        putZip(zip, "[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
        putZip(zip, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
        putZip(zip, "xl/workbook.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Scores\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
        putZip(zip, "xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
        putZip(zip, "xl/worksheets/sheet1.xml", sheetXml(detail));
        zip.finish();
    }

    void putZip(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    String sheetXml(List<String[]> rows) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        for (int r = 0; r < rows.size(); r++) {
            xml
