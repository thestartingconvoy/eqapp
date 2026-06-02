package com.example.carequalizertest;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PRESETS_URL = "https://eqapp-admin.vercel.app/api/presets";
    private static final String PREFS_NAME = "car_eq_prefs";
    private static final String KEY_PRESETS_JSON = "presets_json";
    private static final String KEY_SELECTED_PRESET_ID = "selected_preset_id";

    private Equalizer equalizer;
    private EqualizerCurveView curveView;
    private TextView presetText;
    private TextView statusText;
    private TextView syncText;
    private TextView[] valueLabels = new TextView[0];
    private VerticalSeekBar[] seekBars = new VerticalSeekBar[0];
    private String[] bandLabels = new String[0];
    private int[] bandCenterHz = new int[0];
    private short minBandLevel;
    private short maxBandLevel;
    private boolean equalizerReady;
    private SharedPreferences prefs;
    private ArrayList<EqPreset> presets = new ArrayList<>();
    private String selectedPresetId;
    private Dialog presetDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        selectedPresetId = prefs.getString(KEY_SELECTED_PRESET_ID, null);

        getWindow().setStatusBarColor(Color.rgb(8, 11, 15));
        getWindow().setNavigationBarColor(Color.rgb(8, 11, 15));

        setupEqualizer();
        loadCachedPresets();
        buildUi();
        applyLastSelectedPreset();
        refreshLabels();
    }

    @Override
    protected void onDestroy() {
        if (presetDialog != null) {
            presetDialog.dismiss();
        }
        if (equalizer != null) {
            equalizer.release();
            equalizer = null;
        }
        super.onDestroy();
    }

    private void setupEqualizer() {
        try {
            equalizer = new Equalizer(0, 0);
            equalizer.setEnabled(true);

            short[] range = equalizer.getBandLevelRange();
            minBandLevel = range[0];
            maxBandLevel = range[1];

            short bandCount = equalizer.getNumberOfBands();
            bandLabels = new String[bandCount];
            bandCenterHz = new int[bandCount];
            for (short band = 0; band < bandCount; band++) {
                int centerHz = equalizer.getCenterFreq(band) / 1000;
                bandCenterHz[band] = centerHz;
                bandLabels[band] = formatFrequency(centerHz);
            }
            equalizerReady = true;
        } catch (Throwable error) {
            if (equalizer != null) {
                equalizer.release();
                equalizer = null;
            }
            equalizerReady = false;
            bandLabels = new String[]{"Band 1", "Band 2", "Band 3", "Band 4", "Band 5"};
            bandCenterHz = new int[]{60, 230, 910, 4000, 14000};
            minBandLevel = -1200;
            maxBandLevel = 1200;
        }
    }

    private void buildUi() {
        curveView = new EqualizerCurveView(this);
        valueLabels = new TextView[bandLabels.length];
        seekBars = new VerticalSeekBar[bandLabels.length];

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(32), dp(22), dp(32), dp(22));
        root.setBackgroundColor(Color.rgb(8, 11, 15));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("Car Equalizer Test");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));

        presetText = new TextView(this);
        presetText.setTextColor(Color.rgb(153, 204, 255));
        presetText.setTextSize(18);
        presetText.setGravity(Gravity.CENTER);
        header.addView(presetText, new LinearLayout.LayoutParams(dp(200), dp(48)));

        Button presetsButton = makeButton("Presets");
        presetsButton.setOnClickListener(v -> showPresetDialog());
        header.addView(presetsButton, new LinearLayout.LayoutParams(dp(130), dp(48)));

        Button syncButton = makeButton("Sync Presets");
        syncButton.setOnClickListener(v -> syncPresets());
        LinearLayout.LayoutParams syncParams = new LinearLayout.LayoutParams(dp(170), dp(48));
        syncParams.leftMargin = dp(12);
        header.addView(syncButton, syncParams);

        root.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
        ));

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams statusRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42)
        );
        statusRowParams.topMargin = dp(12);

        statusText = makeStatusText();
        statusText.setText(equalizerReady
                ? "Equalizer initialized on audio session 0 / global output mix"
                : "ERROR: Equalizer could not initialize on audio session 0 / global output mix");
        statusText.setTextColor(equalizerReady ? Color.rgb(164, 232, 191) : Color.rgb(255, 157, 157));
        statusRow.addView(statusText, new LinearLayout.LayoutParams(0, dp(42), 1));

        syncText = makeStatusText();
        syncText.setText(presets.isEmpty() ? "No cached admin presets" : presets.size() + " cached presets");
        syncText.setTextColor(Color.rgb(215, 224, 232));
        LinearLayout.LayoutParams syncTextParams = new LinearLayout.LayoutParams(dp(250), dp(42));
        syncTextParams.leftMargin = dp(12);
        statusRow.addView(syncText, syncTextParams);
        root.addView(statusRow, statusRowParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        );
        contentParams.topMargin = dp(14);

        LinearLayout leftPanel = new LinearLayout(this);
        leftPanel.setOrientation(LinearLayout.VERTICAL);
        leftPanel.setPadding(0, 0, dp(24), 0);
        content.addView(leftPanel, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1.15f));

        leftPanel.addView(curveView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.HORIZONTAL);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, dp(12), 0, 0);
        info.addView(makeChip(String.format(Locale.US, "Bands %d", bandLabels.length)), chipParams());
        info.addView(makeChip("Session 0"), chipParams());
        info.addView(makeChip(equalizerReady ? "Effect On" : "Effect Off"), chipParams());
        info.addView(makeChip(formatDb(minBandLevel) + " to " + formatDb(maxBandLevel)), chipParams());
        leftPanel.addView(info, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
        ));

        LinearLayout sliders = new LinearLayout(this);
        sliders.setOrientation(LinearLayout.HORIZONTAL);
        sliders.setGravity(Gravity.CENTER);
        sliders.setPadding(dp(16), dp(8), dp(16), dp(8));
        sliders.setBackground(new RoundedBackground(Color.rgb(16, 22, 29), dp(8)));

        for (int i = 0; i < bandLabels.length; i++) {
            sliders.addView(makeBandControl(i), new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
            ));
        }
        content.addView(sliders, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1.0f));

        root.addView(content, contentParams);
        setContentView(root);
    }

    private LinearLayout makeBandControl(int index) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);

        valueLabels[index] = new TextView(this);
        valueLabels[index].setTextColor(Color.WHITE);
        valueLabels[index].setTextSize(18);
        valueLabels[index].setGravity(Gravity.CENTER);
        valueLabels[index].setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        column.addView(valueLabels[index], new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(32)
        ));

        seekBars[index] = new VerticalSeekBar(this);
        seekBars[index].setEnabled(equalizerReady);
        seekBars[index].setMax(maxBandLevel - minBandLevel);
        seekBars[index].setProgress(levelToProgress(getBandLevel(index)));
        seekBars[index].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!equalizerReady) {
                    return;
                }
                equalizer.setBandLevel((short) index, progressToLevel(progress));
                presetText.setText(selectedPresetName());
                refreshLabels();
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(dp(72), 0, 1);
        column.addView(seekBars[index], seekParams);

        TextView label = new TextView(this);
        label.setText(bandLabels[index]);
        label.setTextColor(Color.rgb(166, 179, 190));
        label.setTextSize(15);
        label.setGravity(Gravity.CENTER);
        column.addView(label, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(32)
        ));

        return column;
    }

    private void syncPresets() {
        syncText.setText("Syncing...");
        syncText.setTextColor(Color.rgb(153, 204, 255));
        new Thread(() -> {
            try {
                String json = fetchText(PRESETS_URL);
                ArrayList<EqPreset> parsedPresets = parsePresets(json);
                prefs.edit().putString(KEY_PRESETS_JSON, json).apply();
                runOnUiThread(() -> {
                    presets = parsedPresets;
                    syncText.setText(presets.size() + " synced presets");
                    syncText.setTextColor(Color.rgb(164, 232, 191));
                    if (presetDialog != null && presetDialog.isShowing()) {
                        showPresetDialog();
                    }
                    applyLastSelectedPreset();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    syncText.setText("Sync failed; using cache");
                    syncText.setTextColor(Color.rgb(255, 157, 157));
                });
            }
        }).start();
    }

    private String fetchText(String urlText) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestMethod("GET");
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        reader.close();
        connection.disconnect();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("Preset API returned HTTP " + status);
        }
        return result.toString();
    }

    private void showPresetDialog() {
        if (presetDialog != null) {
            presetDialog.dismiss();
        }

        Dialog dialog = new Dialog(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(22), dp(18), dp(22), dp(18));
        panel.setBackground(new RoundedBackground(Color.rgb(13, 18, 24), dp(8)));

        TextView title = new TextView(this);
        title.setText("Presets");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        panel.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42)
        ));

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        if (presets.isEmpty()) {
            TextView empty = makeDialogText("No cached presets. Tap Sync Presets.");
            list.addView(empty, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(52)
            ));
        } else {
            for (EqPreset preset : presets) {
                Button button = makeButton(preset.name);
                button.setGravity(Gravity.CENTER_VERTICAL);
                button.setOnClickListener(v -> applyPreset(preset));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(52)
                );
                params.topMargin = dp(8);
                list.addView(button, params);
            }
        }

        scroll.addView(list);
        panel.addView(scroll, new LinearLayout.LayoutParams(
                dp(420),
                dp(280)
        ));

        Button close = makeButton("Close");
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
        );
        closeParams.topMargin = dp(14);
        panel.addView(close, closeParams);

        dialog.setContentView(panel);
        presetDialog = dialog;
        dialog.show();
    }

    private void loadCachedPresets() {
        String json = prefs.getString(KEY_PRESETS_JSON, null);
        if (json == null || json.trim().isEmpty()) {
            return;
        }
        try {
            presets = parsePresets(json);
        } catch (Exception ignored) {
            presets = new ArrayList<>();
        }
    }

    private ArrayList<EqPreset> parsePresets(String json) throws Exception {
        ArrayList<EqPreset> result = new ArrayList<>();
        JSONArray presetArray = new JSONObject(json).optJSONArray("presets");
        if (presetArray == null) {
            return result;
        }
        for (int i = 0; i < presetArray.length(); i++) {
            JSONObject item = presetArray.optJSONObject(i);
            if (item == null) {
                continue;
            }
            JSONObject config = item.optJSONObject("config");
            JSONArray bands = config == null ? null : config.optJSONArray("targetBands");
            if (bands == null) {
                continue;
            }

            EqPreset preset = new EqPreset();
            preset.id = item.optString("id", "preset-" + i);
            preset.name = item.optString("name", "Preset " + (i + 1));
            for (int bandIndex = 0; bandIndex < bands.length(); bandIndex++) {
                JSONObject band = bands.optJSONObject(bandIndex);
                if (band == null || !band.has("hz") || !band.has("gainDb")) {
                    continue;
                }
                preset.bands.add(new JsonBand(band.optDouble("hz"), band.optDouble("gainDb")));
            }
            if (!preset.bands.isEmpty()) {
                result.add(preset);
            }
        }
        return result;
    }

    private void applyLastSelectedPreset() {
        if (!equalizerReady || presets.isEmpty()) {
            presetText.setText(selectedPresetName());
            return;
        }
        EqPreset selected = findPresetById(selectedPresetId);
        if (selected != null) {
            applyPreset(selected);
        } else {
            presetText.setText("Manual");
        }
    }

    private void applyPreset(EqPreset preset) {
        if (!equalizerReady) {
            return;
        }
        int bandCount = bandLabels.length;
        double[] sums = new double[bandCount];
        int[] counts = new int[bandCount];

        for (JsonBand jsonBand : preset.bands) {
            int androidBand = nearestBand(jsonBand.hz);
            sums[androidBand] += jsonBand.gainDb;
            counts[androidBand]++;
        }

        for (short band = 0; band < bandCount; band++) {
            if (counts[band] == 0) {
                continue;
            }
            double averageGainDb = sums[band] / counts[band];
            equalizer.setBandLevel(band, clampLevel((int) Math.round(averageGainDb * 100.0)));
        }

        selectedPresetId = preset.id;
        prefs.edit().putString(KEY_SELECTED_PRESET_ID, selectedPresetId).apply();
        presetText.setText(preset.name);
        refreshLabels();
    }

    private int nearestBand(double hz) {
        int nearest = 0;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < bandCenterHz.length; i++) {
            double distance = Math.abs(hz - bandCenterHz[i]);
            if (distance < nearestDistance) {
                nearest = i;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private EqPreset findPresetById(String id) {
        if (id == null) {
            return null;
        }
        for (EqPreset preset : presets) {
            if (id.equals(preset.id)) {
                return preset;
            }
        }
        return null;
    }

    private String selectedPresetName() {
        EqPreset selected = findPresetById(selectedPresetId);
        return selected == null ? "Manual" : selected.name;
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(15);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setBackground(new RoundedBackground(Color.rgb(26, 81, 128), dp(8)));
        return button;
    }

    private TextView makeStatusText() {
        TextView text = new TextView(this);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setTextSize(16);
        text.setPadding(dp(14), 0, dp(14), 0);
        text.setBackground(new RoundedBackground(Color.rgb(16, 22, 29), dp(8)));
        return text;
    }

    private TextView makeChip(String text) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextColor(Color.rgb(215, 224, 232));
        chip.setTextSize(15);
        chip.setGravity(Gravity.CENTER);
        chip.setBackground(new RoundedBackground(Color.rgb(23, 31, 40), dp(8)));
        return chip;
    }

    private TextView makeDialogText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.rgb(215, 224, 232));
        view.setTextSize(16);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private LinearLayout.LayoutParams chipParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1);
        params.leftMargin = dp(6);
        params.rightMargin = dp(6);
        return params;
    }

    private void refreshLabels() {
        int[] levels = new int[bandLabels.length];
        for (int i = 0; i < valueLabels.length; i++) {
            short level = getBandLevel(i);
            levels[i] = level;
            valueLabels[i].setText(formatDb(level));
            if (seekBars[i] != null && seekBars[i].getProgress() != levelToProgress(level)) {
                seekBars[i].setProgress(levelToProgress(level));
            }
        }
        curveView.setBands(levels, minBandLevel, maxBandLevel);
    }

    private short getBandLevel(int index) {
        if (!equalizerReady) {
            return 0;
        }
        return equalizer.getBandLevel((short) index);
    }

    private short progressToLevel(int progress) {
        return clampLevel(minBandLevel + progress);
    }

    private int levelToProgress(short level) {
        return level - minBandLevel;
    }

    private short clampLevel(int level) {
        return (short) Math.max(minBandLevel, Math.min(maxBandLevel, level));
    }

    private String formatFrequency(int hz) {
        if (hz >= 1000) {
            return String.format(Locale.US, "%.1f kHz", hz / 1000f);
        }
        return String.format(Locale.US, "%d Hz", hz);
    }

    private String formatDb(int milliBel) {
        return String.format(Locale.US, "%+.1f dB", milliBel / 100f);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class EqPreset {
        String id;
        String name;
        ArrayList<JsonBand> bands = new ArrayList<>();
    }

    private static class JsonBand {
        final double hz;
        final double gainDb;

        JsonBand(double hz, double gainDb) {
            this.hz = hz;
            this.gainDb = gainDb;
        }
    }

    private static class EqualizerCurveView extends View {
        private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint curvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path curvePath = new Path();
        private final Path fillPath = new Path();
        private int[] bands = new int[0];
        private int minLevel = -1200;
        private int maxLevel = 1200;

        EqualizerCurveView(android.content.Context context) {
            super(context);
            gridPaint.setColor(Color.rgb(44, 56, 68));
            gridPaint.setStrokeWidth(1.5f);
            curvePaint.setColor(Color.rgb(54, 179, 255));
            curvePaint.setStyle(Paint.Style.STROKE);
            curvePaint.setStrokeCap(Paint.Cap.ROUND);
            curvePaint.setStrokeJoin(Paint.Join.ROUND);
            curvePaint.setStrokeWidth(7f);
            textPaint.setColor(Color.rgb(146, 162, 176));
            textPaint.setTextSize(28f);
            textPaint.setTextAlign(Paint.Align.LEFT);
        }

        void setBands(int[] values, int min, int max) {
            bands = values;
            minLevel = min;
            maxLevel = max;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            RectF chart = new RectF(22, 20, width - 22, height - 54);

            Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
            bg.setColor(Color.rgb(13, 18, 24));
            canvas.drawRoundRect(new RectF(0, 0, width, height), 18, 18, bg);

            for (int i = 0; i <= 4; i++) {
                float y = chart.top + chart.height() * i / 4f;
                canvas.drawLine(chart.left, y, chart.right, y, gridPaint);
            }
            for (int i = 0; i < Math.max(1, bands.length); i++) {
                float x = chart.left + chart.width() * i / Math.max(1f, bands.length - 1f);
                canvas.drawLine(x, chart.top, x, chart.bottom, gridPaint);
            }

            if (bands.length > 0 && maxLevel != minLevel) {
                curvePath.reset();
                fillPath.reset();
                for (int i = 0; i < bands.length; i++) {
                    float x = chart.left + chart.width() * i / Math.max(1f, bands.length - 1f);
                    float normalized = (bands[i] - minLevel) / (float) (maxLevel - minLevel);
                    float y = chart.bottom - chart.height() * normalized;
                    if (i == 0) {
                        curvePath.moveTo(x, y);
                        fillPath.moveTo(x, chart.bottom);
                        fillPath.lineTo(x, y);
                    } else {
                        curvePath.lineTo(x, y);
                        fillPath.lineTo(x, y);
                    }
                }
                fillPath.lineTo(chart.right, chart.bottom);
                fillPath.close();

                fillPaint.setShader(new LinearGradient(
                        0, chart.top, 0, chart.bottom,
                        Color.argb(100, 54, 179, 255),
                        Color.argb(4, 54, 179, 255),
                        Shader.TileMode.CLAMP
                ));
                canvas.drawPath(fillPath, fillPaint);
                fillPaint.setShader(null);
                canvas.drawPath(curvePath, curvePaint);
            }

            canvas.drawText(formatAxis(maxLevel), chart.left, chart.top + 30, textPaint);
            canvas.drawText("0.0 dB", chart.left, chart.centerY() - 8, textPaint);
            canvas.drawText(formatAxis(minLevel), chart.left, chart.bottom - 12, textPaint);
        }

        private String formatAxis(int milliBel) {
            return String.format(Locale.US, "%+.1f dB", milliBel / 100f);
        }
    }

    private static class VerticalSeekBar extends SeekBar {
        VerticalSeekBar(android.content.Context context) {
            super(context);
        }

        @Override
        protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(heightMeasureSpec, widthMeasureSpec);
            setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.rotate(-90);
            canvas.translate(-getHeight(), 0);
            super.onDraw(canvas);
        }

        @Override
        public boolean onTouchEvent(android.view.MotionEvent event) {
            if (!isEnabled()) {
                return false;
            }
            int progress = getMax() - (int) (getMax() * event.getY() / getHeight());
            setProgress(Math.max(0, Math.min(getMax(), progress)));
            onSizeChanged(getWidth(), getHeight(), 0, 0);
            return true;
        }
    }
}
