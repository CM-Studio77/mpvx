package com.cmstudio.player.codec;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cmstudio.player.AppTheme;
import com.cmstudio.player.R;

import java.util.ArrayList;
import java.util.List;

public class CodecCapabilitiesActivity extends AppCompatActivity {

    private static final int FILTER_ALL = 0;
    private static final int FILTER_HW = 1;
    private static final int FILTER_SW = 2;
    private static final int FILTER_VIDEO = 3;
    private static final int FILTER_AUDIO = 4;

    private int currentFilter = FILTER_ALL;
    private CodecInspector.InspectionResult inspectionResult;
    private final List<CodecCapabilitiesInfo> displayedList = new ArrayList<>();
    private CodecAdapter adapter;

    private TextView filterAll;
    private TextView filterHw;
    private TextView filterSw;
    private TextView filterVideo;
    private TextView filterAudio;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppTheme.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_codec_capabilities);

        final View rootView = findViewById(R.id.codec_root);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
                final Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                return WindowInsetsCompat.CONSUMED;
            });
        }

        final Toolbar toolbar = findViewById(R.id.codec_toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Hardware Decoders & Codecs");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        filterAll = findViewById(R.id.filter_all);
        filterHw = findViewById(R.id.filter_hw);
        filterSw = findViewById(R.id.filter_sw);
        filterVideo = findViewById(R.id.filter_video);
        filterAudio = findViewById(R.id.filter_audio);

        filterAll.setOnClickListener(v -> setFilter(FILTER_ALL));
        filterHw.setOnClickListener(v -> setFilter(FILTER_HW));
        filterSw.setOnClickListener(v -> setFilter(FILTER_SW));
        filterVideo.setOnClickListener(v -> setFilter(FILTER_VIDEO));
        filterAudio.setOnClickListener(v -> setFilter(FILTER_AUDIO));

        final RecyclerView recyclerView = findViewById(R.id.codec_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CodecAdapter(displayedList);
        recyclerView.setAdapter(adapter);

        // Run inspection in background
        new Thread(() -> {
            final CodecInspector.InspectionResult result = CodecInspector.inspect(this);
            runOnUiThread(() -> {
                inspectionResult = result;
                populateMetrics(result);
                populateKeyHighlights(result);
                applyCurrentFilter();
            });
        }).start();
    }

    private void populateMetrics(CodecInspector.InspectionResult result) {
        final TextView totalView = findViewById(R.id.metric_total_count);
        final TextView hwView = findViewById(R.id.metric_hw_count);
        final TextView swView = findViewById(R.id.metric_sw_count);

        if (totalView != null) totalView.setText(String.valueOf(result.totalCount));
        if (hwView != null) hwView.setText(String.valueOf(result.hardwareCount));
        if (swView != null) swView.setText(String.valueOf(result.softwareCount));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(result.hardwareCount + " Hardware Accelerated • Android " + Build.VERSION.RELEASE);
        }
    }

    private void populateKeyHighlights(CodecInspector.InspectionResult result) {
        final LinearLayout row = findViewById(R.id.key_codecs_row);
        if (row == null) return;
        row.removeAllViews();

        final LayoutInflater inflater = LayoutInflater.from(this);
        for (CodecInspector.KeyCodecSummary summary : result.keySummaries) {
            final View chip = inflater.inflate(R.layout.item_codec_highlight_chip, row, false);
            final TextView nameText = chip.findViewById(R.id.highlight_name);
            final TextView statusText = chip.findViewById(R.id.highlight_status);
            final TextView resText = chip.findViewById(R.id.highlight_res);

            nameText.setText(summary.formatName);

            if (summary.hasHardware) {
                statusText.setText("HW DECODER");
                statusText.setTextColor(0xFF00E5FF);
            } else if (summary.hasSoftware) {
                statusText.setText("SW ONLY");
                statusText.setTextColor(0xFF94A3B8);
            } else {
                statusText.setText("NOT SUPPORTED");
                statusText.setTextColor(0xFFE57373);
            }

            if (summary.maxResolution != null) {
                resText.setVisibility(View.VISIBLE);
                resText.setText(summary.maxResolution);
            } else {
                resText.setVisibility(View.GONE);
            }

            row.addView(chip);
        }
    }

    private void setFilter(int filter) {
        currentFilter = filter;
        updateFilterTabs();
        applyCurrentFilter();
    }

    private void updateFilterTabs() {
        updateTabStyle(filterAll, currentFilter == FILTER_ALL);
        updateTabStyle(filterHw, currentFilter == FILTER_HW);
        updateTabStyle(filterSw, currentFilter == FILTER_SW);
        updateTabStyle(filterVideo, currentFilter == FILTER_VIDEO);
        updateTabStyle(filterAudio, currentFilter == FILTER_AUDIO);
    }

    private void updateTabStyle(TextView tab, boolean isSelected) {
        if (tab == null) return;
        tab.setBackgroundResource(isSelected ? R.drawable.quick_pill_selected : R.drawable.quick_pill_unselected);
        tab.setTextColor(isSelected ? Color.WHITE : getResources().getColor(R.color.kr_text_secondary));
    }

    private void applyCurrentFilter() {
        if (inspectionResult == null) return;
        displayedList.clear();

        for (CodecCapabilitiesInfo item : inspectionResult.codecs) {
            boolean keep = false;
            switch (currentFilter) {
                case FILTER_ALL:
                    keep = true;
                    break;
                case FILTER_HW:
                    keep = item.isHardware;
                    break;
                case FILTER_SW:
                    keep = !item.isHardware;
                    break;
                case FILTER_VIDEO:
                    keep = item.isVideo;
                    break;
                case FILTER_AUDIO:
                    keep = !item.isVideo;
                    break;
            }
            if (keep) {
                displayedList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 101, 0, "Copy Full Report")
                .setIcon(R.drawable.ic_content_copy_24dp)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 101) {
            copyFullReport();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void copyFullReport() {
        if (inspectionResult == null) return;
        final StringBuilder sb = new StringBuilder();
        sb.append("=== MPVX HARDWARE CODEC CAPABILITIES REPORT ===\n");
        sb.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append(" (Android ").append(Build.VERSION.RELEASE).append(")\n");
        sb.append("Total Decoders: ").append(inspectionResult.totalCount)
          .append(" | HW: ").append(inspectionResult.hardwareCount)
          .append(" | SW: ").append(inspectionResult.softwareCount).append("\n\n");

        sb.append("--- KEY CODECS ---\n");
        for (CodecInspector.KeyCodecSummary summary : inspectionResult.keySummaries) {
            sb.append(summary.formatName).append(": ")
              .append(summary.hasHardware ? "Hardware" : (summary.hasSoftware ? "Software Only" : "Unsupported"))
              .append(summary.maxResolution != null ? " (" + summary.maxResolution + ")" : "")
              .append(summary.isHdrSupported ? " [HDR]" : "")
              .append("\n");
        }

        sb.append("\n--- ALL DECODERS (").append(inspectionResult.codecs.size()).append(") ---\n");
        for (CodecCapabilitiesInfo c : inspectionResult.codecs) {
            sb.append("[").append(c.isHardware ? "HW" : "SW").append("] ")
              .append(c.name).append(" -> ").append(c.formatName).append(" (").append(c.mimeType).append(")\n");
            if (c.maxResolution != null) sb.append("   Max Res: ").append(c.maxResolution).append("\n");
            if (c.bitrateRange != null) sb.append("   Bitrate: ").append(c.bitrateRange).append("\n");
            if (!c.profilesAndLevels.isEmpty()) sb.append("   Profiles: ").append(c.profilesAndLevels).append("\n");
            if (!c.features.isEmpty()) sb.append("   Features: ").append(c.features).append("\n");
        }

        final ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cb != null) {
            cb.setPrimaryClip(ClipData.newPlainText("Codec Report", sb.toString()));
            Toast.makeText(this, "Hardware Codec report copied to clipboard!", Toast.LENGTH_SHORT).show();
        }
    }

    // RecyclerView Adapter
    private static class CodecAdapter extends RecyclerView.Adapter<CodecViewHolder> {
        private final List<CodecCapabilitiesInfo> list;
        private int expandedPosition = -1;

        CodecAdapter(List<CodecCapabilitiesInfo> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public CodecViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_codec_card, parent, false);
            return new CodecViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CodecViewHolder holder, int position) {
            final CodecCapabilitiesInfo item = list.get(position);
            holder.bind(item, position == expandedPosition, () -> {
                int prev = expandedPosition;
                expandedPosition = (expandedPosition == position) ? -1 : position;
                if (prev != -1) notifyItemChanged(prev);
                notifyItemChanged(position);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    private static class CodecViewHolder extends RecyclerView.ViewHolder {
        private final TextView formatName;
        private final TextView hwBadge;
        private final TextView decoderName;
        private final TextView badgeResolution;
        private final TextView badgeHdr;
        private final TextView badge10Bit;
        private final View detailsSection;
        private final TextView profilesText;
        private final TextView colorsText;
        private final TextView featuresText;
        private final View colorsLabel;
        private final View featuresLabel;

        CodecViewHolder(@NonNull View itemView) {
            super(itemView);
            formatName = itemView.findViewById(R.id.card_format_name);
            hwBadge = itemView.findViewById(R.id.card_hw_badge);
            decoderName = itemView.findViewById(R.id.card_decoder_name);
            badgeResolution = itemView.findViewById(R.id.badge_resolution);
            badgeHdr = itemView.findViewById(R.id.badge_hdr);
            badge10Bit = itemView.findViewById(R.id.badge_10bit);
            detailsSection = itemView.findViewById(R.id.card_details_section);
            profilesText = itemView.findViewById(R.id.card_profiles_text);
            colorsText = itemView.findViewById(R.id.card_colors_text);
            featuresText = itemView.findViewById(R.id.card_features_text);
            colorsLabel = itemView.findViewById(R.id.card_colors_label);
            featuresLabel = itemView.findViewById(R.id.card_features_label);
        }

        void bind(CodecCapabilitiesInfo item, boolean isExpanded, Runnable onClick) {
            formatName.setText(item.formatName);
            decoderName.setText(item.name);

            if (item.isHardware) {
                hwBadge.setText("HARDWARE");
                hwBadge.setTextColor(0xFF00E5FF);
                hwBadge.setBackgroundResource(R.drawable.neo_badge_pill);
            } else {
                hwBadge.setText("SOFTWARE");
                hwBadge.setTextColor(0xFF94A3B8);
                hwBadge.setBackgroundResource(R.drawable.neo_segment_container);
            }

            if (item.maxResolution != null) {
                badgeResolution.setVisibility(View.VISIBLE);
                badgeResolution.setText(item.maxResolution);
            } else if (item.maxChannels != null && item.maxChannels > 0) {
                badgeResolution.setVisibility(View.VISIBLE);
                badgeResolution.setText(item.maxChannels + " Channels Audio");
            } else {
                badgeResolution.setVisibility(View.GONE);
            }

            badgeHdr.setVisibility(item.isHdrSupported ? View.VISIBLE : View.GONE);
            badge10Bit.setVisibility(item.is10BitSupported ? View.VISIBLE : View.GONE);

            // Expandable details
            detailsSection.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            if (isExpanded) {
                if (!item.profilesAndLevels.isEmpty()) {
                    profilesText.setText(String.join(" • ", item.profilesAndLevels));
                } else {
                    profilesText.setText("Standard Profile");
                }

                if (!item.colorFormats.isEmpty()) {
                    colorsLabel.setVisibility(View.VISIBLE);
                    colorsText.setVisibility(View.VISIBLE);
                    colorsText.setText(String.join(", ", item.colorFormats));
                } else if (!item.sampleRates.isEmpty()) {
                    colorsLabel.setVisibility(View.VISIBLE);
                    colorsText.setVisibility(View.VISIBLE);
                    colorsText.setText(String.join(", ", item.sampleRates));
                } else {
                    colorsLabel.setVisibility(View.GONE);
                    colorsText.setVisibility(View.GONE);
                }

                if (!item.features.isEmpty()) {
                    featuresLabel.setVisibility(View.VISIBLE);
                    featuresText.setVisibility(View.VISIBLE);
                    featuresText.setText(String.join(" • ", item.features));
                } else {
                    featuresLabel.setVisibility(View.GONE);
                    featuresText.setVisibility(View.GONE);
                }
            }

            itemView.setOnClickListener(v -> onClick.run());
        }
    }
}
