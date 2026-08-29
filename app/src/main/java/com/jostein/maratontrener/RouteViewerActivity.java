package com.jostein.maratontrener;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RouteViewerActivity extends AppCompatActivity {

    private WebView webViewMap;
    private LinearLayout chartElevationContainer;
    private TextView textRouteDistance, textRouteGain, textRouteSlope;
    private LineChart lineChart;
    private List<double[]> routePoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_viewer);

        ImageButton btnBack = findViewById(R.id.btnBackRouteViewer);
        btnBack.setOnClickListener(v -> finish());

        webViewMap = findViewById(R.id.webviewMap);
        chartElevationContainer = findViewById(R.id.chartElevationContainer);
        textRouteDistance = findViewById(R.id.textRouteDistance);
        textRouteGain = findViewById(R.id.textRouteGain);
        textRouteSlope = findViewById(R.id.textRouteSlope);

        loadGpxRouteData();
    }

    private void loadGpxRouteData() {
        SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
        String gpxPoints = prefs.getString("gpxRoutePoints", null);
        String gpxName = prefs.getString("gpxRouteName", "Route");
        double gpxDistance = Double.longBitsToDouble(prefs.getLong("gpxDistance", Double.doubleToRawLongBits(0.0)));
        double gpxElevationGain = Double.longBitsToDouble(prefs.getLong("gpxElevationGain", Double.doubleToRawLongBits(0.0)));
        double gpxAvgSlope = Double.longBitsToDouble(prefs.getLong("gpxAvgSlope", Double.doubleToRawLongBits(0.0)));

        if (gpxPoints == null || gpxPoints.isEmpty()) {
            finish();
            return;
        }

        textRouteDistance.setText(String.format(Locale.getDefault(), "%.2f km", gpxDistance));
        textRouteGain.setText(String.format(Locale.getDefault(), "%.1f m", gpxElevationGain));
        textRouteSlope.setText(String.format(Locale.getDefault(), "%.2f%%", gpxAvgSlope));

        // Parse points string back to List of points: lat, lon, ele, dist
        try {
            String clean = gpxPoints.substring(2, gpxPoints.length() - 2); // remove outer [[ and ]]
            String[] groups = clean.split("\\],\\s*\\[");
            for (String grp : groups) {
                String[] vals = grp.split(",");
                routePoints.add(new double[]{
                        Double.parseDouble(vals[0].trim()),
                        Double.parseDouble(vals[1].trim()),
                        Double.parseDouble(vals[2].trim()),
                        Double.parseDouble(vals[3].trim())
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            finish();
            return;
        }

        setupWebViewMap();
        setupElevationChart();
    }

    private void setupWebViewMap() {
        WebSettings webSettings = webViewMap.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webViewMap.setWebViewClient(new WebViewClient());

        // Construct coordinates coordinates string for javascript
        StringBuilder sbCoords = new StringBuilder();
        sbCoords.append("[");
        for (int i = 0; i < routePoints.size(); i++) {
            double[] p = routePoints.get(i);
            sbCoords.append(String.format(Locale.US, "[%.6f,%.6f]", p[0], p[1]));
            if (i < routePoints.size() - 1) sbCoords.append(",");
        }
        sbCoords.append("]");

        String htmlContent = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n" +
                "    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />\n" +
                "    <script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>\n" +
                "    <style>\n" +
                "        body, html, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #121212; }\n" +
                "        .leaflet-container { background: #121212 !important; }\n" +
                "        .leaflet-bar a { background-color: #222 !important; color: #fff !important; border-bottom: 1px solid #333 !important; }\n" +
                "        .leaflet-bar a:hover { background-color: #333 !important; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id=\"map\"></div>\n" +
                "    <script>\n" +
                "        var map = null;\n" +
                "        var polyline = null;\n" +
                "        var marker = null;\n" +
                "        var coords = " + sbCoords.toString() + ";\n" +
                "        \n" +
                "        function initMap() {\n" +
                "            map = L.map('map').setView(coords[0], 13);\n" +
                "            L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {\n" +
                "                maxZoom: 19\n" +
                "            }).addTo(map);\n" +
                "            \n" +
                "            polyline = L.polyline(coords, {\n" +
                "                color: '#ccff00',\n" +
                "                weight: 4,\n" +
                "                opacity: 0.85\n" +
                "            }).addTo(map);\n" +
                "            \n" +
                "            map.fitBounds(polyline.getBounds(), { padding: [15, 15] });\n" +
                "            \n" +
                "            L.circleMarker(coords[0], { radius: 6, color: '#4ade80', fillColor: '#111', fillOpacity: 0.9, weight: 3 }).addTo(map);\n" +
                "            L.circleMarker(coords[coords.length - 1], { radius: 6, color: '#f87171', fillColor: '#111', fillOpacity: 0.9, weight: 3 }).addTo(map);\n" +
                "            \n" +
                "            marker = L.circleMarker(coords[0], { \n" +
                "                radius: 8,\n" +
                "                color: '#00e5ff',\n" +
                "                fillColor: '#fff',\n" +
                "                fillOpacity: 1.0,\n" +
                "                weight: 3\n" +
                "            }).addTo(map);\n" +
                "        }\n" +
                "        \n" +
                "        function updateMarker(idx) {\n" +
                "            if (marker && coords[idx]) {\n" +
                "                var pt = coords[idx];\n" +
                "                marker.setLatLng(pt);\n" +
                "                if (map && !map.getBounds().contains(marker.getLatLng())) {\n" +
                "                    map.panTo(pt);\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        \n" +
                "        initMap();\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";

        webViewMap.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
    }

    private void setupElevationChart() {
        lineChart = new LineChart(this);

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < routePoints.size(); i++) {
            double[] p = routePoints.get(i);
            Entry entry = new Entry((float) p[3], (float) p[2]);
            entry.setData(i); // Store index for highlighting callback
            entries.add(entry);
        }

        LineDataSet dataSet = new LineDataSet(entries, "Elevation");
        dataSet.setColor(Color.parseColor("#00e5ff")); // var(--android-pace) equivalent
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setHighLightColor(Color.WHITE);
        dataSet.setHighlightLineWidth(1.5f);
        dataSet.setDrawHorizontalHighlightIndicator(false);

        // Gradient Fill
        dataSet.setDrawFilled(true);
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#4400e5ff"), Color.TRANSPARENT}
        );
        dataSet.setFillDrawable(gd);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // Styling
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.setDrawGridBackground(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f km", value);
            }
        });

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#22FFFFFF"));
        leftAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f m", value);
            }
        });

        lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (e.getData() instanceof Integer) {
                    int idx = (Integer) e.getData();
                    webViewMap.evaluateJavascript("javascript:updateMarker(" + idx + ")", null);
                }
            }

            @Override
            public void onNothingSelected() {}
        });

        lineChart.animateX(800);
        chartElevationContainer.addView(lineChart, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
    }
}
