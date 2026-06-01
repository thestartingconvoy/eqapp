package com.example.carequalizertest;

import android.app.Activity;
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
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {
    private Equalizer equalizer;
    private EqualizerCurveView curveView;
    private LinearLayout sliders;
    private TextView presetText;
    private TextView statusText;
    private TextView rangeText;
    private TextView[] valueLabels = new TextView[0];
    private VerticalSeekBar[] seekBars = new VerticalSeekBar[0];
    private String[] bandLabels = new String[0];
    private short minBandLevel;
    private short maxBandLevel;
    private int presetIndex;
    private boolean equalizerReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(8, 11, 15));
        getWindow().setNavigationBarColor(Color.rgb(8, 11, 15));

        setupEqualizer();
        buildUi();
        refreshLabels();
    }

    @Override
    protected void onDestroy() {
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
            for (short band = 0; band < bandCount; band++) {
                bandLabels[band] = formatFrequency(equalizer.getCenterFreq(band));
            }
            equalizerReady = true;
        } catch (Throwable error) {
            equalizerReady = false;
            bandLabels = new String[]{"Band 1", "Band 2", "Band 3", "Band 4", "Band 5"};
            minBandLevel = -1200;
            maxBandLevel = 1200;
            statusText = null;
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
        header.addView(presetText, new LinearLayout.LayoutParams(dp(180), dp(48)));

        Button resetButton = makeButton("Reset");
        resetButton.setEnabled(equalizerReady);
        resetButton.setOnClickListener(v -> applyPreset(0));
        header.addView(resetButton, new LinearLayout.LayoutParams(dp(120), dp(48)));

        Button presetButton = makeButton("Preset");
        presetButton.setEnabled(equalizerReady);
        presetButton.setOnClickListener(v -> applyPreset((presetIndex + 1) % 4));
        LinearLayout.LayoutParams presetParams = new LinearLayout.LayoutParams(dp(130), dp(48));
        presetParams.leftMargin = dp(12);
        header.addView(presetButton, presetParams);

        root.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
        ));

        statusText = makeStatusText();
        statusText.setText(equalizerReady
                ? "Equalizer initialized on audio session 0 / global output mix"
                : "ERROR: Equalizer could not initialize on audio session 0 / global output mix");
        statusText.setTextColor(equalizerReady ? Color.rgb(164, 232, 191) : Color.rgb(255, 157, 157));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42)
        );
        statusParams.topMargin = dp(12);
        root.addView(statusText, statusParams);

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

        String bandText = String.format(Locale.US, "Bands %d", bandLabels.length);
        rangeText = makeChip(bandText);
        info.addView(rangeText, chipParams());
        info.addView(makeChip("Session 0"), chipParams());
        info.addView(makeChip(equalizerReady ? "Effect On" : "Effect Off"), chipParams());
        info.addView(makeChip(formatDb(minBandLevel) + " to " + formatDb(maxBandLevel)), chipParams());
        leftPanel.addView(info, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
        ));

        sliders = new LinearLayout(this);
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

        if (equalizerReady) {
            applyPreset(2);
        } else {
            presetText.setText("Unavailable");
        }
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
                short level = progressToLevel(progress);
                equalizer.setBandLevel((short) index, level);
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

    private LinearLayout.LayoutParams chipParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1);
        params.leftMargin = dp(6);
        params.rightMargin = dp(6);
        return params;
    }

    private void applyPreset(int index) {
        if (!equalizerReady) {
            return;
        }
        presetIndex = index;
        String[] names = {"Flat", "Vocal", "Road", "Bass"};
        float[][] presets = {
                {0f, 0f, 0f, 0f, 0f},
                {-0.20f, 0.10f, 0.40f, 0.20f, -0.10f},
                {0.30f, 0.10f, -0.10f, 0.20f, 0.40f},
                {0.55f, 0.35f, 0f, -0.10f, 0.20f}
        };
        for (short band = 0; band < bandLabels.length; band++) {
            float shape = presets[index][Math.min(band, presets[index].length - 1)];
            equalizer.setBandLevel(band, scaledLevel(shape));
        }
        presetText.setText(names[index]);
        refreshLabels();
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

    private short scaledLevel(float normalized) {
        float target = normalized >= 0
                ? normalized * maxBandLevel
                : -normalized * minBandLevel;
        return clampLevel(Math.round(target));
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

    private String formatFrequency(int milliHertz) {
        float hz = milliHertz / 1000f;
        if (hz >= 1000f) {
            return String.format(Locale.US, "%.1f kHz", hz / 1000f);
        }
        return String.format(Locale.US, "%.0f Hz", hz);
    }

    private String formatDb(int milliBel) {
        return String.format(Locale.US, "%+.1f dB", milliBel / 100f);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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

            if (bands.length > 0) {
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
