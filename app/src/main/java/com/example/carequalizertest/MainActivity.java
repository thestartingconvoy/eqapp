package com.example.carequalizertest;

import android.app.Activity;
import android.graphics.Color;
import android.media.audiofx.BassBoost;
import android.media.audiofx.DynamicsProcessing;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {

    private static final int SESSION = 0;

    // Audio effects
    private Equalizer equalizer;
    private BassBoost bassBoost;
    private LoudnessEnhancer loudnessEnhancer;
    private Virtualizer virtualizer;
    private PresetReverb presetReverb;
    private DynamicsProcessing dynamicsProcessing;

    // Ready flags
    private boolean eqReady, bbReady, leReady, virtReady, reverbReady, dynReady;

    // EQ range, used for preamp clamping
    private short eqMin = -1200, eqMax = 1200;
    private int currentPreampMb = 0;

    // Limiter params (applied when sliders change)
    private float limThresholdDb = -20f;
    private float limRatio = 2f;
    private float limAttackMs = 50f;
    private float limReleaseMs = 200f;
    private float limOutputGainDb = 0f;

    // Reverb
    private static final short[] REVERB_PRESETS = {
        PresetReverb.PRESET_NONE,
        PresetReverb.PRESET_SMALLROOM,
        PresetReverb.PRESET_MEDIUMROOM,
        PresetReverb.PRESET_LARGEROOM,
        PresetReverb.PRESET_MEDIUMHALL,
        PresetReverb.PRESET_LARGEHALL,
        PresetReverb.PRESET_PLATE
    };
    private static final String[] REVERB_NAMES = {
        "None", "Small Room", "Med Room", "Large Room", "Med Hall", "Large Hall", "Plate"
    };
    private Button[] reverbButtons;
    private Button limOffBtn, limOnBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(8, 11, 15));
        getWindow().setNavigationBarColor(Color.rgb(8, 11, 15));
        setupEffects();
        buildUi();
    }

    @Override
    protected void onDestroy() {
        safeRelease(equalizer, bassBoost, loudnessEnhancer, virtualizer, presetReverb, dynamicsProcessing);
        super.onDestroy();
    }

    // ─── Effects setup ─────────────────────────────────────────────────────────

    private void setupEffects() {
        // Equalizer — used only as a preamp (global band offset); bands not shown in UI
        try {
            equalizer = new Equalizer(0, SESSION);
            equalizer.setEnabled(true);
            short[] r = equalizer.getBandLevelRange();
            eqMin = r[0];
            eqMax = r[1];
            eqReady = true;
        } catch (Throwable t) {
            eqReady = false;
        }

        try {
            bassBoost = new BassBoost(0, SESSION);
            bassBoost.setEnabled(true);
            bbReady = true;
        } catch (Throwable t) {
            bbReady = false;
        }

        try {
            loudnessEnhancer = new LoudnessEnhancer(SESSION);
            loudnessEnhancer.setEnabled(true);
            leReady = true;
        } catch (Throwable t) {
            leReady = false;
        }

        try {
            virtualizer = new Virtualizer(0, SESSION);
            virtualizer.setEnabled(true);
            virtReady = true;
        } catch (Throwable t) {
            virtReady = false;
        }

        try {
            presetReverb = new PresetReverb(0, SESSION);
            presetReverb.setPreset(PresetReverb.PRESET_NONE);
            presetReverb.setEnabled(false);
            reverbReady = true;
        } catch (Throwable t) {
            reverbReady = false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Try stereo first, fall back to mono
            dynReady = tryInitDynamics(2) || tryInitDynamics(1);
        }
    }

    private boolean tryInitDynamics(int channels) {
        try {
            DynamicsProcessing.Config cfg = new DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                channels, false, 0, false, 0, false, 0, true
            ).build();
            dynamicsProcessing = new DynamicsProcessing(0, SESSION, cfg);
            dynamicsProcessing.setEnabled(false);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private void safeRelease(android.media.audiofx.AudioEffect... effects) {
        for (android.media.audiofx.AudioEffect e : effects) {
            if (e != null) try { e.release(); } catch (Exception ignored) {}
        }
    }

    // ─── UI ────────────────────────────────────────────────────────────────────

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(8, 11, 15));
        root.setPadding(dp(20), dp(12), dp(20), dp(12));

        root.addView(buildHeader(), lp(MATCH, dp(46)));
        root.addView(buildStatusRow(), lp(MATCH, dp(38)));

        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(22, 32, 42));
        LinearLayout.LayoutParams divP = lp(MATCH, dp(1));
        divP.topMargin = dp(10);
        divP.bottomMargin = dp(10);
        root.addView(divider, divP);

        // Two-panel content area
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);

        ScrollView leftScroll = new ScrollView(this);
        leftScroll.addView(buildLeftPanel());
        content.addView(leftScroll, lp(0, MATCH, 1f));

        View vDiv = new View(this);
        vDiv.setBackgroundColor(Color.rgb(22, 32, 42));
        LinearLayout.LayoutParams vP = lp(dp(1), MATCH);
        vP.leftMargin = dp(14);
        vP.rightMargin = dp(14);
        content.addView(vDiv, vP);

        ScrollView rightScroll = new ScrollView(this);
        rightScroll.addView(buildRightPanel());
        content.addView(rightScroll, lp(0, MATCH, 1f));

        LinearLayout.LayoutParams contentP = lp(MATCH, 0);
        contentP.weight = 1f;
        root.addView(content, contentP);

        setContentView(root);
    }

    private View buildHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Car Audio Controls");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        row.addView(title, lp(0, MATCH, 1f));

        return row;
    }

    private View buildStatusRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        row.addView(statusChip("EQ / Preamp", eqReady), lp(0, MATCH, 1f));
        row.addView(statusChip("Bass Boost", bbReady), lp(0, MATCH, 1f));
        row.addView(statusChip("Loudness", leReady), lp(0, MATCH, 1f));
        row.addView(statusChip("Virtualizer", virtReady), lp(0, MATCH, 1f));
        row.addView(statusChip("Reverb", reverbReady), lp(0, MATCH, 1f));
        row.addView(statusChip("Dynamics", dynReady), lp(0, MATCH, 1f));

        return row;
    }

    private TextView statusChip(String name, boolean ok) {
        TextView v = new TextView(this);
        v.setText(name + (ok ? "  OK" : "  N/A"));
        v.setTextSize(13);
        v.setGravity(Gravity.CENTER);
        v.setTextColor(ok ? Color.rgb(120, 210, 150) : Color.rgb(200, 90, 90));
        v.setBackground(new RoundedBackground(Color.rgb(16, 23, 31), dp(8)));
        v.setPadding(dp(6), 0, dp(6), 0);
        LinearLayout.LayoutParams p = lp(0, MATCH, 1f);
        p.leftMargin = dp(4);
        p.rightMargin = dp(4);
        v.setLayoutParams(p);
        return v;
    }

    private LinearLayout buildLeftPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(0, 0, dp(6), dp(12));

        section(panel, "BASS  &  DYNAMICS");

        // Bass Boost: 0–1000
        panel.addView(slider("Bass Boost", 0, 1000, 0, bbReady, val -> {
            if (bbReady) bassBoost.setStrength((short) val);
            return String.valueOf(val);
        }));

        // Loudness Enhancer: -4800 to +4800 millibels
        panel.addView(slider("Loudness Gain", -4800, 4800, 0, leReady, val -> {
            if (leReady) loudnessEnhancer.setTargetGain(val);
            return fmtDb(val);
        }));

        // Preamp: -2400 to +2400 millibels, applied as uniform EQ band offset
        panel.addView(slider("Preamp", -2400, 2400, 0, eqReady, val -> {
            applyPreamp(val);
            return fmtDb(val);
        }));

        section(panel, "SPATIAL");

        // Virtualizer: 0–1000
        panel.addView(slider("Virtualizer", 0, 1000, 0, virtReady, val -> {
            if (virtReady) virtualizer.setStrength((short) val);
            return String.valueOf(val);
        }));

        section(panel, "REVERB");
        panel.addView(buildReverbPicker());

        return panel;
    }

    private LinearLayout buildRightPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(6), 0, 0, dp(12));

        section(panel, "LIMITER  (DynamicsProcessing API 28+)");
        panel.addView(buildLimiterToggle());

        // Threshold: -80 to 0 dB
        panel.addView(slider("Threshold", -8000, 0, -2000, dynReady, val -> {
            limThresholdDb = val / 100f;
            applyLimiter();
            return fmtDb(val);
        }));

        // Ratio: 1.0 to 50.0  (stored ×100 as int for integer seekbar)
        panel.addView(slider("Ratio", 100, 5000, 200, dynReady, val -> {
            limRatio = val / 100f;
            applyLimiter();
            return String.format(Locale.US, "%.1f : 1", limRatio);
        }));

        // Attack: 0–1000 ms
        panel.addView(slider("Attack", 0, 1000, 50, dynReady, val -> {
            limAttackMs = val;
            applyLimiter();
            return val + " ms";
        }));

        // Release: 0–5000 ms
        panel.addView(slider("Release", 0, 5000, 200, dynReady, val -> {
            limReleaseMs = val;
            applyLimiter();
            return val + " ms";
        }));

        // Output Gain: -40 to +40 dB (in millibels)
        panel.addView(slider("Output Gain", -4000, 4000, 0, dynReady, val -> {
            limOutputGainDb = val / 100f;
            applyLimiter();
            return fmtDb(val);
        }));

        return panel;
    }

    // ─── Slider factory ────────────────────────────────────────────────────────

    interface Callback { String apply(int val); }

    private View slider(String name, int min, int max, int def, boolean enabled, Callback cb) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = lp(MATCH, dp(54));
        rp.topMargin = dp(4);

        TextView label = new TextView(this);
        label.setText(name);
        label.setTextColor(enabled ? Color.rgb(185, 200, 215) : Color.rgb(85, 95, 105));
        label.setTextSize(15);
        label.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(label, lp(dp(125), MATCH));

        TextView valueView = new TextView(this);
        valueView.setTextColor(Color.WHITE);
        valueView.setTextSize(14);
        valueView.setGravity(Gravity.CENTER);
        valueView.setTypeface(android.graphics.Typeface.MONOSPACE);
        valueView.setBackground(new RoundedBackground(Color.rgb(16, 22, 30), dp(6)));
        row.addView(valueView, lp(dp(96), dp(40)));

        SeekBar bar = new SeekBar(this);
        bar.setMax(max - min);
        bar.setEnabled(enabled);
        LinearLayout.LayoutParams bp = lp(0, dp(44), 1f);
        bp.leftMargin = dp(10);
        row.addView(bar, bp);

        // Initialize with default (applies effect + sets display)
        valueView.setText(cb.apply(def));
        bar.setProgress(def - min);

        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar b, int p, boolean fromUser) {
                valueView.setText(cb.apply(min + p));
            }
            @Override public void onStartTrackingTouch(SeekBar b) {}
            @Override public void onStopTrackingTouch(SeekBar b) {}
        });

        return row;
    }

    // ─── Reverb picker ─────────────────────────────────────────────────────────

    private View buildReverbPicker() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = lp(MATCH, dp(50));
        rp.topMargin = dp(4);

        reverbButtons = new Button[REVERB_NAMES.length];
        for (int i = 0; i < REVERB_NAMES.length; i++) {
            final int idx = i;
            Button btn = new Button(this);
            btn.setText(REVERB_NAMES[i]);
            btn.setTextSize(12);
            btn.setAllCaps(false);
            btn.setTextColor(Color.WHITE);
            btn.setEnabled(reverbReady);
            btn.setBackground(new RoundedBackground(
                idx == 0 ? Color.rgb(26, 81, 128) : Color.rgb(18, 26, 36), dp(8)));
            btn.setOnClickListener(v -> selectReverb(idx));
            LinearLayout.LayoutParams bp = lp(0, dp(46), 1f);
            if (i > 0) bp.leftMargin = dp(5);
            row.addView(btn, bp);
            reverbButtons[i] = btn;
        }
        return row;
    }

    private void selectReverb(int idx) {
        if (reverbReady && presetReverb != null) {
            if (idx == 0) {
                presetReverb.setEnabled(false);
            } else {
                presetReverb.setEnabled(true);
                presetReverb.setPreset(REVERB_PRESETS[idx]);
            }
        }
        for (int i = 0; i < reverbButtons.length; i++) {
            reverbButtons[i].setBackground(new RoundedBackground(
                i == idx ? Color.rgb(26, 81, 128) : Color.rgb(18, 26, 36), dp(8)));
        }
    }

    // ─── Limiter toggle ────────────────────────────────────────────────────────

    private View buildLimiterToggle() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = lp(MATCH, dp(54));
        rp.topMargin = dp(4);
        rp.bottomMargin = dp(6);

        TextView label = new TextView(this);
        label.setText("Limiter");
        label.setTextColor(dynReady ? Color.rgb(185, 200, 215) : Color.rgb(85, 95, 105));
        label.setTextSize(15);
        label.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(label, lp(dp(125), MATCH));

        limOffBtn = new Button(this);
        limOffBtn.setText("OFF");
        limOffBtn.setAllCaps(false);
        limOffBtn.setTextSize(14);
        limOffBtn.setTextColor(Color.WHITE);
        limOffBtn.setEnabled(dynReady);
        limOffBtn.setBackground(new RoundedBackground(Color.rgb(26, 81, 128), dp(8)));

        limOnBtn = new Button(this);
        limOnBtn.setText("ON");
        limOnBtn.setAllCaps(false);
        limOnBtn.setTextSize(14);
        limOnBtn.setTextColor(Color.WHITE);
        limOnBtn.setEnabled(dynReady);
        limOnBtn.setBackground(new RoundedBackground(Color.rgb(18, 26, 36), dp(8)));

        limOffBtn.setOnClickListener(v -> {
            if (dynReady && dynamicsProcessing != null) dynamicsProcessing.setEnabled(false);
            limOffBtn.setBackground(new RoundedBackground(Color.rgb(26, 81, 128), dp(8)));
            limOnBtn.setBackground(new RoundedBackground(Color.rgb(18, 26, 36), dp(8)));
        });
        limOnBtn.setOnClickListener(v -> {
            if (dynReady && dynamicsProcessing != null) {
                applyLimiter();
                dynamicsProcessing.setEnabled(true);
            }
            limOnBtn.setBackground(new RoundedBackground(Color.rgb(26, 81, 128), dp(8)));
            limOffBtn.setBackground(new RoundedBackground(Color.rgb(18, 26, 36), dp(8)));
        });

        LinearLayout.LayoutParams offP = lp(dp(88), dp(44));
        LinearLayout.LayoutParams onP = lp(dp(88), dp(44));
        onP.leftMargin = dp(8);
        row.addView(limOffBtn, offP);
        row.addView(limOnBtn, onP);

        return row;
    }

    // ─── Effect helpers ────────────────────────────────────────────────────────

    private void applyPreamp(int newMb) {
        if (!eqReady || equalizer == null) return;
        int delta = newMb - currentPreampMb;
        currentPreampMb = newMb;
        short count = equalizer.getNumberOfBands();
        for (short i = 0; i < count; i++) {
            short cur = equalizer.getBandLevel(i);
            short next = (short) Math.max(eqMin, Math.min(eqMax, cur + delta));
            equalizer.setBandLevel(i, next);
        }
    }

    private void applyLimiter() {
        if (!dynReady || dynamicsProcessing == null) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return;
        try {
            DynamicsProcessing.Limiter limiter = new DynamicsProcessing.Limiter(
                true, true, 0,
                limAttackMs, limReleaseMs,
                limRatio, limThresholdDb, limOutputGainDb
            );
            dynamicsProcessing.setLimiterAllChannelsTo(limiter);
        } catch (Exception ignored) {}
    }

    // ─── Layout helpers ────────────────────────────────────────────────────────

    private void section(LinearLayout parent, String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.rgb(70, 110, 155));
        v.setTextSize(12);
        v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        v.setGravity(Gravity.BOTTOM);
        LinearLayout.LayoutParams p = lp(MATCH, dp(34));
        p.topMargin = dp(8);
        parent.addView(v, p);
    }

    private String fmtDb(int millibels) {
        return String.format(Locale.US, "%+.1f dB", millibels / 100f);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static final int MATCH = LinearLayout.LayoutParams.MATCH_PARENT;

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    private LinearLayout.LayoutParams lp(int w, int h, float weight) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.weight = weight;
        return p;
    }
}
