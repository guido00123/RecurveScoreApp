
package nl.recurve.scoreapp;
import android.app.*; 
import android.os.*; 
import android.graphics.Color; 
import android.text.*; 
import android.text.InputFilter; 
import android.view.*; 
import android.widget.*; 
import android.content.*; 
import java.util.*;
import android.net.Uri;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
public class MainActivity extends Activity {
 int navy=Color.rgb(31,78,121), pale=Color.rgb(221,235,247);
SharedPreferences p; LinearLayout root,body; Spinner picker;
EditText[] names=new EditText[4]; TextView rank; int selected=0; boolean loading=false;
EditText dateInput;
 EditText[][][] inputs=new EditText[4][16][3]; TextView[][] ends=new TextView[4][16]; TextView[][] turns=new TextView[4][4]; TextView[] grands=new TextView[4];
 public void onCreate(Bundle b){super.onCreate(b);p=getSharedPreferences("scores",0); build(); leaderboard();}
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 LinearLayout.LayoutParams lp(){return new LinearLayout.LayoutParams(-1,-2);}
 TextView label(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(navy);if(bold)t.setTypeface(null,1);t.setPadding(0,dp(12),0,dp(5));return t;}
 void build(){
  ScrollView sc=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(14),dp(12),dp(14),dp(24));root.setBackgroundColor(0xfff8fafc);sc.addView(root);setContentView(sc);
  TextView title=label("🏹 Recurve Score",26,true);title.setGravity(17);root.addView(title);
  TextView sub=new TextView(this);sub.setText("4 schutters • 4 beurten • 4 ends • 3 pijlen");sub.setGravity(17);root.addView(sub);
  
root.addView(label("Datum wedstrijd", 18, true));

dateInput = new EditText(this);
dateInput.setSingleLine();
dateInput.setHint("Kies de wedstrijddatum");
dateInput.setTextSize(16);
dateInput.setFocusable(false);
dateInput.setClickable(true);
dateInput.setCompoundDrawablesWithIntrinsicBounds(
    0, 0, android.R.drawable.ic_menu_my_calendar, 0
);

dateInput.setText(p.getString("wedstrijd_datum", ""));
root.addView(dateInput, new LinearLayout.LayoutParams(-1, dp(48)));

dateInput.setOnClickListener(v -> {
    java.util.Calendar cal = java.util.Calendar.getInstance();

    String saved = p.getString("wedstrijd_datum", "");
    try {
        java.text.SimpleDateFormat fmt =
            new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.ROOT);
        fmt.setLenient(false);
        java.util.Date date = fmt.parse(saved);
        if (date != null) cal.setTime(date);
    } catch (Exception ignored) {
    }

    android.app.DatePickerDialog dialog =
        new android.app.DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                String datum = String.format(
                    java.util.Locale.ROOT,
                    "%02d-%02d-%04d",
                    dayOfMonth, month + 1, year
                );

                dateInput.setText(datum);
                p.edit().putString("wedstrijd_datum", datum).apply();
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        );

    dialog.show();
});

});
  root.addView(label("Namen van de schutters",18,true));
  for(int i=0;i<4;i++){final int k=i;EditText n=new EditText(this);n.setSingleLine();n.setHint("Naam schutter "+(i+1));n.setText(p.getString("name"+i,""));root.addView(n,new LinearLayout.LayoutParams(-1,dp(46)));names[i]=n;n.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int before,int count){p.edit().putString("name"+k,s.toString()).apply();leaderboard();labels();}public void afterTextChanged(Editable e){}});}
  root.addView(label("Scores invoeren",18,true));picker=new Spinner(this);root.addView(picker,lp());labels();picker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> a,View v,int pos,long id){if(selected!=pos){selected=pos;show(pos);}}public void onNothingSelected(AdapterView<?> a){}});
  body=new LinearLayout(this);body.setOrientation(1);root.addView(body,lp());Button export = new Button(this);
export.setText("Exporteer naar Excel (.xlsx)");
export.setOnClickListener(v -> {
    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    intent.putExtra(Intent.EXTRA_TITLE, "RecurveScore.xlsx");
    startActivityForResult(intent, 1001);
});
root.addView(export, lp());root.addView(label("Klassement",18,true));rank=new TextView(this);rank.setTextSize(16);rank.setPadding(dp(12),dp(12),dp(12),dp(12));rank.setBackgroundColor(Color.WHITE);root.addView(rank,lp());
  Button clear=new Button(this);clear.setText("Alle scores wissen");clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Alle scores wissen?").setMessage("Alle pijlscores worden verwijderd; namen blijven behouden.").setNegativeButton("Annuleren",null).setPositiveButton("Wissen",(d,w)->{SharedPreferences.Editor e=p.edit();for(int s=0;s<4;s++)for(int x=0;x<16;x++)for(int a=0;a<3;a++)e.remove(key(s,x,a));e.apply();show(selected);leaderboard();}).show());root.addView(clear,lp());
  TextView note=new TextView(this);note.setText("Werkt offline. Scores worden automatisch op deze telefoon bewaard. Vul 0–10 of X in; X telt als 10.");note.setTextSize(12);note.setPadding(0,dp(10),0,0);root.addView(note);
 }
 void labels(){if(picker==null)return;String[] a=new String[4];for(int i=0;i<4;i++){String n=names[i]==null?"":names[i].getText().toString().trim();a[i]="Schutter "+(i+1)+(n.isEmpty()?"":" – "+n);}ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,a);ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);picker.setAdapter(ad);picker.setSelection(selected);}
 TextView cell(String s,boolean head){TextView t=new TextView(this);t.setText(s);t.setTextSize(head?12:15);t.setGravity(17);t.setPadding(dp(2),dp(7),dp(2),dp(7));t.setBackgroundColor(head?pale:Color.WHITE);if(head)t.setTypeface(null,1);return t;}
 void show(int s){loading=true;body.removeAllViews();for(int turn=0;turn<4;turn++){body.addView(label("Beurt "+(turn+1),18,true));TableLayout tab=new TableLayout(this);tab.setStretchAllColumns(true);TableRow h=new TableRow(this);for(String z:new String[]{"End","Pijl 1","Pijl 2","Pijl 3","Totaal"})h.addView(cell(z,true));tab.addView(h);
  for(int e=0;e<4;e++){int ix=turn*4+e;TableRow row=new TableRow(this);row.addView(cell(""+(e+1),false));for(int a=0;a<3;a++){
  final int ar = a;

Spinner in = new Spinner(this);
String[] options = {"–", "X", "10", "9", "8", "7", "6",
                    "5", "4", "3", "2", "1", "0", "M"};

ArrayAdapter<String> adapter = new ArrayAdapter<>(
    this, android.R.layout.simple_spinner_item, options);
adapter.setDropDownViewResource(
    android.R.layout.simple_spinner_dropdown_item);
in.setAdapter(adapter);

String saved = p.getString(key(s, ix, a), "");
int pos = 0;
for (int j = 0; j < options.length; j++) {
    if (options[j].equalsIgnoreCase(saved)) {
        pos = j;
        break;
    }
}
in.setSelection(pos);
inputs[s][ix][a] = null;

in.setOnItemSelectedListener(
    new android.widget.AdapterView.OnItemSelectedListener() {
        boolean first = true;

        @Override
        public void onItemSelected(
                android.widget.AdapterView<?> parent,
                View view, int position, long id) {
            if (first) {
                first = false;
                return;
            }

            String value = options[position];
            if (value.equals("–")) value = "";

            p.edit().putString(key(s, ix, ar), value).apply();
            totals(s);
            leaderboard();
        }

        @Override
        public void onNothingSelected(
                android.widget.AdapterView<?> parent) {}
    });

row.addView(in, new TableRow.LayoutParams(0, dp(44), 1));}ends[s][ix]=cell("0",false);row.addView(ends[s][ix]);tab.addView(row);}body.addView(tab);turns[s][turn]=label("Totaal beurt "+(turn+1)+": 0 / 120",16,true);turns[s][turn].setGravity(Gravity.RIGHT);body.addView(turns[s][turn]);}
  grands[s]=label("EINDTOTAAL: 0 / 480",20,true);grands[s].setGravity(17);grands[s].setBackgroundColor(pale);body.addView(grands[s]);loading=false;totals(s);
 }
 boolean valid(String v){if(v.isEmpty()||v.equals("X"))return true;try{int n=Integer.parseInt(v);return n>=0&&n<=10;}catch(Exception e){return false;}}
 String key(int s,int e,int a){return "s"+s+"e"+e+"a"+a;}
 int val(String v){if(v.equalsIgnoreCase("X"))return 10;try{int n=Integer.parseInt(v);return n>=0&&n<=10?n:0;}catch(Exception e){return 0;}}
 int score(int s){int sum=0;for(int e=0;e<16;e++)for(int a=0;a<3;a++)sum+=val(p.getString(key(s,e,a),""));return sum;}
 void totals(int s){int all=0;for(int t=0;t<4;t++){int ts=0;for(int e=t*4;e<t*4+4;e++){int es=0;for(int a=0;a<3;a++)es+=val(p.getString(key(s,e,a),""));if(ends[s][e]!=null)ends[s][e].setText(""+es);ts+=es;}if(turns[s][t]!=null)turns[s][t].setText("Totaal beurt "+(t+1)+": "+ts+" / 120");all+=ts;}if(grands[s]!=null)grands[s].setText("EINDTOTAAL: "+all+" / 480");}
 
void leaderboard() {
    if (rank == null) return;

    StringBuilder b = new StringBuilder();

    b.append(String.format(Locale.ROOT,
        "%-12s %4s %4s %4s %4s %5s\n",
        "Schutter", "B1", "B2", "B3", "B4", "Totaal"));

    for (int s = 0; s < 4; s++) {
        String n = names[s] == null ? "" :
            names[s].getText().toString().trim();

        if (n.isEmpty()) n = "Schutter " + (s + 1);
        if (n.length() > 12) n = n.substring(0, 12);

        int[] beurt = new int[4];

        for (int t = 0; t < 4; t++) {
            for (int e = t * 4; e < t * 4 + 4; e++) {
                for (int a = 0; a < 3; a++) {
                    beurt[t] += val(
                        p.getString(key(s, e, a), "")
                    );
                }
            }
        }

        b.append(String.format(Locale.ROOT,
            "%-12s %4d %4d %4d %4d %5d\n",
            n,
            beurt[0],
            beurt[1],
            beurt[2],
            beurt[3],
            beurt[0] + beurt[1] + beurt[2] + beurt[3]
        ));
    }

    rank.setTypeface(android.graphics.Typeface.MONOSPACE);
    rank.setTextSize(13);
    rank.setText(b.toString());
}
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == 1001 && resultCode == RESULT_OK
            && data != null && data.getData() != null) {
        try {
            OutputStream out =
                getContentResolver().openOutputStream(data.getData());

            if (out == null) {
                throw new IOException("Bestand kan niet worden geopend.");
            }

            try {
                writeExcel(out);
            } finally {
                out.close();
            }

            Toast.makeText(this, "Excelbestand opgeslagen!",
                Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Export mislukt: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        }
    }
}

void writeExcel(OutputStream output) throws IOException {
    ZipOutputStream zip = new ZipOutputStream(output);
    List<String[]> detail = new ArrayList<>();
    List<String[]> summary = new ArrayList<>();

    String date = p.getString("wedstrijd_datum", "");

    detail.add(new String[]{
        "Datum", "Schutter", "Beurt", "End",
        "Pijl 1", "Pijl 2", "Pijl 3",
        "Endtotaal", "Beurttotaal"
    });

    summary.add(new String[]{
        "Datum", "Schutter", "Beurt 1",
        "Beurt 2", "Beurt 3", "Beurt 4", "Eindtotaal"
    });

    for (int s = 0; s < 4; s++) {
        String name = p.getString("name" + s, "");
        if (name.trim().isEmpty()) {
            name = "Schutter " + (s + 1);
        }

        int grand = 0;
        int[] turnTotals = new int[4];

        for (int e = 0; e < 16; e++) {
            int turn = e / 4;
            int endTotal = 0;
            String[] arrows = new String[3];

            for (int a = 0; a < 3; a++) {
                arrows[a] = p.getString(key(s, e, a), "");
                endTotal += val(arrows[a]);
            }

            turnTotals[turn] += endTotal;
        }

        for (int t = 0; t < 4; t++) {
            grand += turnTotals[t];
        }

        for (int e = 0; e < 16; e++) {
            int turn = e / 4;
            String[] arrows = new String[3];
            int endTotal = 0;

            for (int a = 0; a < 3; a++) {
                arrows[a] = p.getString(key(s, e, a), "");
                endTotal += val(arrows[a]);
            }

            detail.add(new String[]{
                date, name,
                String.valueOf(turn + 1),
                String.valueOf((e % 4) + 1),
                arrows[0], arrows[1], arrows[2],
                String.valueOf(endTotal),
                String.valueOf(turnTotals[turn])
            });
        }

        summary.add(new String[]{
            date, name,
            String.valueOf(turnTotals[0]),
            String.valueOf(turnTotals[1]),
            String.valueOf(turnTotals[2]),
            String.valueOf(turnTotals[3]),
            String.valueOf(grand)
        });
    }

    putZip(zip, "[Content_Types].xml",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
        "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
        "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
        "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
        "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
        "<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
        "</Types>");

    putZip(zip, "_rels/.rels",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
        "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
        "</Relationships>");

    putZip(zip, "xl/workbook.xml",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
        "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
        "<sheets><sheet name=\"Pijlscores\" sheetId=\"1\" r:id=\"rId1\"/>" +
        "<sheet name=\"Overzicht\" sheetId=\"2\" r:id=\"rId2\"/></sheets></workbook>");

    putZip(zip, "xl/_rels/workbook.xml.rels",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
        "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
        "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>" +
        "</Relationships>");

    putZip(zip, "xl/worksheets/sheet1.xml", sheetXml(detail));
    putZip(zip, "xl/worksheets/sheet2.xml", sheetXml(summary));
    zip.finish();
}

void putZip(ZipOutputStream zip, String name, String content)
        throws IOException {
    zip.putNextEntry(new ZipEntry(name));
    zip.write(content.getBytes(StandardCharsets.UTF_8));
    zip.closeEntry();
}

String sheetXml(List<String[]> rows) {
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    xml.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");

    for (int r = 0; r < rows.size(); r++) {
        xml.append("<row r=\"").append(r + 1).append("\">");
        String[] cells = rows.get(r);

        for (int c = 0; c < cells.length; c++) {
            String value = cells[c] == null ? "" : cells[c];
            String ref = columnName(c + 1) + (r + 1);

            if (value.matches("-?\\d+(\\.\\d+)?")) {
                xml.append("<c r=\"").append(ref).append("\"><v>")
                    .append(value).append("</v></c>");
            } else {
                xml.append("<c r=\"").append(ref)
                    .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                    .append(xmlEscape(value))
                    .append("</t></is></c>");
            }
        }

        xml.append("</row>");
    }

    xml.append("</sheetData></worksheet>");
    return xml.toString();
}

String columnName(int number) {
    String result = "";
    while (number > 0) {
        number--;
        result = (char) ('A' + number % 26) + result;
        number /= 26;
    }
    return result;
}

String xmlEscape(String value) {
    return value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
}
}
