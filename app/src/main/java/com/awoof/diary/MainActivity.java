package com.awoof.diary;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.awoof.diary.R;

public class MainActivity extends Activity {

    private ListView listView;
    private DataAdapter adapter;
    private List<DataItem> allData = new ArrayList<DataItem>();
    private List<DataItem> filteredData = new ArrayList<DataItem>();
    private EditText searchBar;
    private RadioGroup networkGroup;
    private Button btnSticky;
    private ImageView btnPrivacy; // New Privacy Button

    private boolean showOnlyCheats = false;
    private boolean isPaid = false;
    
    private String selectedNetwork = "MTN";
    private String pendingCallCode = null;

    private static final String CSV_URL = "https://docs.google.com/spreadsheets/d/e/2PACX-1vT3bm7IR0Znur1M9DkHvlyN882huI7gNm3BMG9NSdIe8QrbE1ru1bxX5sNnUNALTj8bIByQrysNvbkM/pub?output=csv";
    private static final String CACHE_FILE = "cache.dat";
    private static final String XOR_KEY = "AWOOF";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView) findViewById(R.id.listView);
        searchBar = (EditText) findViewById(R.id.searchBar);
        networkGroup = (RadioGroup) findViewById(R.id.rg_main_network); 
        btnSticky = (Button) findViewById(R.id.btnSticky);
        btnPrivacy = (ImageView) findViewById(R.id.btnPrivacy); // Bind Icon

        checkPaymentStatus();

        adapter = new DataAdapter(this, filteredData, isPaid);
        listView.setAdapter(adapter);

        loadOfflineData();
        fetchCloudData(); 

        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPaymentStatus();
        if (adapter != null) {
            adapter.setPaidStatus(isPaid);
        }
    }

    private void checkPaymentStatus() {
        SharedPreferences prefs = getSharedPreferences("AwoofPrefs", MODE_PRIVATE);
        isPaid = prefs.getBoolean("isPaid", false);
    }

    private void setupListeners() {
        // Privacy Policy Dialog
        btnPrivacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPrivacyDialog();
            }
        });

        networkGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.tab_airtel) selectedNetwork = "AIRTEL";
                else if (checkedId == R.id.tab_glo) selectedNetwork = "GLO";
                else selectedNetwork = "MTN";
                applyFilters();
            }
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            public void afterTextChanged(Editable s) {}
        });

        btnSticky.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOnlyCheats = !showOnlyCheats;
                btnSticky.setText(showOnlyCheats ? "🔙 View All Diaries" : "✨ View Bonus Cheats");
                applyFilters();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DataItem item = filteredData.get(position);
                boolean isPremium = item.status.trim().equalsIgnoreCase("PREMIUM");
                
                if (isPremium && !isPaid) {
                    Intent intent = new Intent(MainActivity.this, PaymentActivity.class);
                    startActivity(intent);
                } else {
                    performDirectDial(item.code);
                }
            }
        });
    }

    private void showPrivacyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Privacy Policy");
        builder.setMessage("1. Data Collection: Awoof Diary does not collect or store personal user data on external servers.\n\n" +
                           "2. Permissions:\n" +
                           "- Call Phone: Used strictly to dial USSD codes when you tap the call icon.\n" +
                           "- Internet: Used to fetch updated codes and process payments.\n\n" +
                           "3. Payments: All transactions are processed securely via Paystack. We do not store card details.\n\n" +
                           "4. Offline Data: Cached data is stored locally on your device.");
        builder.setPositiveButton("I Understand", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void applyFilters() {
        filteredData.clear();
        String query = searchBar.getText().toString().toLowerCase().trim();

        for (DataItem item : allData) {
            boolean matchNetwork = item.network.isEmpty() || item.network.toUpperCase().contains(selectedNetwork);
            boolean matchQuery = item.title.toLowerCase().contains(query);
            boolean matchCheat = !showOnlyCheats || (item.title.toLowerCase().contains("cheat") || item.title.toLowerCase().contains("hack"));

            if (matchNetwork && matchQuery && matchCheat) {
                filteredData.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void performDirectDial(String code) {
        pendingCallCode = code.replace("#", "%23");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 100);
                return;
            }
        }
        executeDial();
    }

    private void executeDial() {
        if (pendingCallCode != null) {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + pendingCallCode));
            try {
                startActivity(intent);
            } catch (SecurityException e) {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
            }
            pendingCallCode = null;
        }
    }

    private String xorProcess(String text) {
        char[] key = XOR_KEY.toCharArray();
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            out.append((char) (text.charAt(i) ^ key[i % key.length]));
        }
        return out.toString();
    }

    private void loadOfflineData() {
        try {
            FileInputStream fis = openFileInput(CACHE_FILE);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = fis.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] resultBytes = buffer.toByteArray();
            fis.close();
            String rawData = new String(resultBytes);
            if (rawData.length() > 0) {
                String decrypted = xorProcess(rawData);
                parseCSV(decrypted);
            }
        } catch (Exception e) { }
    }

    private void fetchCloudData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(CSV_URL);
                    HttpURLConnection conn;
                    while (true) {
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setInstanceFollowRedirects(false); 
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(15000);
                        int status = conn.getResponseCode();
                        if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                            String location = conn.getHeaderField("Location");
                            url = new URL(url, location);
                            continue;
                        }
                        break; 
                    }
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine).append("\n");
                    }
                    in.close();
                    final String csvData = response.toString();
                    if (!csvData.contains(",") || csvData.length() < 50) return;
                    String encrypted = xorProcess(csvData);
                    File oldFile = new File(getFilesDir(), CACHE_FILE);
                    if (oldFile.exists()) oldFile.delete();
                    FileOutputStream fos = openFileOutput(CACHE_FILE, MODE_PRIVATE);
                    fos.write(encrypted.getBytes());
                    fos.flush(); 
                    fos.close();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            parseCSV(csvData);
                        }
                    });
                } catch (Exception e) { e.printStackTrace(); }
            }
        }).start();
    }

    private void parseCSV(String data) {
        if (data == null || data.isEmpty()) return;
        allData.clear();
        String[] lines = data.split("\n");
        boolean isFirst = true;
        for (String line : lines) {
            if (isFirst) { isFirst = false; continue; }
            String[] cols = line.split(",", -1);
            if (cols.length >= 3) {
                String cTitle = cols[0].replace("\"", "").trim();
                String cCode = cols[1].replace("\"", "").trim();
                String cStatus = cols[2].replace("\"", "").trim();
                String cNetwork = (cols.length >= 4) ? cols[3].replace("\"", "").trim() : "";
                if (!cTitle.isEmpty()) {
                    allData.add(new DataItem(cTitle, cCode, cNetwork, cStatus));
                }
            }
        }
        applyFilters();
    }
}
