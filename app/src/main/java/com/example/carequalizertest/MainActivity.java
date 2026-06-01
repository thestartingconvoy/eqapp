package com.example.carequalizertest;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final String[] BAND_LABELS = {"60", "230", "910", "4K", "14K"};
    private static final int MIN_DB = -12;
    private static final int MAX_DB = 12;
    private static final int RANGE_DB = MAX_DB - MIN_DB;

    private final int[] bandValues = {3, 1, 0, 2, 4};
    private EqualizerCurveView curveView;
    private TextView presetText;
    private TextView[] valueLabels;
    private VerticalSeekBar[] seekBars;
    private int presetIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(8, 11, 15));
        getWindow().setNavigationBarColor(Color.rgb(8, 11, 15));

        curveView = new EqualizerCurveView(this);
        valueLabels = new TextView[BAND_LABELS.length];
        seekBars = new VerticalSeekBar[BAND_LABELS.length];

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
        resetButton.setOnClickListener(v -> applyPreset(0));
        header.addView(resetButton, new LinearLayout.LayoutParams(dp(120), dp(48)));

        Button presetButton = makeButton("Preset");
        presetButton.setOnClickListener(v -> applyPreset((presetIndex + 1) % 4));
        LinearLayout.LayoutParams presetParams = new LinearLayout.LayoutParams(dp(130), dp(48));
        presetParams.leftMargin = dp(12);
        header.addView(presetButton, presetParams);

        root.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
        ));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        );
        contentParams.topMargin = dp(18);

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

        LinearLayout status = new LinearLayout(this);
        status.setOrientation(LinearLayout.HORIZONTAL);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(12), 0, 0);
        String[] statusItems = {"Volume 24", "Balance C", "Fader C", "Loudness On"};
        for (String item : statusItems) {
            TextView chip = new TextView(this);
            chip.setText(item);
            chip.setTextColor(Color.rgb(215, 224, 232));
            chip.setTextSize(15);
            chip.setGravity(Gravity.CENTER);
            chip.setBackground(new RoundedBackground(Color.rgb(23, 31, 40), dp(8)));
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(0, dp(42), 1);
            chipParams.leftMargin = dp(6);
            chipParams.rightMargin = dp(6);
            status.addView(chip, chipParams);
        }
        leftPanel.addView(status, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
        ));

        LinearLayout sliders = new LinearLayout(this);
        sliders.setOrientation(LinearLayout.HORIZONTAL);
        sliders.setGravity(Gravity.CENTER);
        sliders.setPadding(dp(16), dp(8), dp(16), dp(8));
        sliders.setBackground(new RoundedBackground(Color.rgb(16, 22, 29), dp(8)));

        for (int i = 0; i < BAND_LABELS.length; i++) {
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
        applyPreset(2);
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
        seekBars[index].setMax(RANGE_DB);
        seekBars[index].setProgress(dbToProgress(bandValues[index]));
        seekBars[index].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                bandValues[index] = progressToDb(progress);
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
        label.setText(BAND_LABELS[index] + " Hz");
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

    private void applyPreset(int index) {
        presetIndex = index;
        String[] names = {"Flat", "Vocal", "Road", "Bass"};
        int[][] presets = {
                {0, 0, 0, 0, 0},
                {-2, 1, 4, 2, -1},
                {3, 1, -1, 2, 4},
                {6, 4, 0, -1, 2}
        };
        System.arraycopy(presets[index], 0, bandValues, 0, bandValues.length);
        presetText.setText(names[index]);
        refreshLabels();
    }

    private void refreshLabels() {
        for (int i = 0; i < valueLabels.length; i++) {
            valueLabels[i].setText(String.format(Locale.US, "%+d dB", bandValues[i]));
            if (seekBars[i] != null && seekBars[i].getProgress() != dbToProgress(bandValues[i])) {
                seekBars[i].setProgress(dbToProgress(bandValues[i]));
            }
        }
        curveView.setBands(bandValues);
    }

    private int dbToProgress(int db) {
        return db - MIN_DB;
    }

    private int progressToDb(int progress) {
        return progress + MIN_DB;
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
        private final int[] bands = {0, 0, 0, 0, 0};

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

        void setBands(int[] values) {
            System.arraycopy(values, 0, bands, 0, bands.length);
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
            for (int i = 0; i < bands.length; i++) {
                float x = chart.left + chart.width() * i / (bands.length - 1f);
                canvas.drawLine(x, chart.top, x, chart.bottom, gridPaint);
            }

            curvePath.reset();
            fillPath.reset();
            for (int i = 0; i < bands.length; i++) {
                float x = chart.left + chart.width() * i / (bands.length - 1f);
                float normalized = (bands[i] - MIN_DB) / (float) RANGE_DB;
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

            canvas.drawText("+12 dB", chart.left, chart.top + 30, textPaint);
            canvas.drawText("0 dB", chart.left, chart.centerY() - 8, textPaint);
            canvas.drawText("-12 dB", chart.left, chart.bottom - 12, textPaint);
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
