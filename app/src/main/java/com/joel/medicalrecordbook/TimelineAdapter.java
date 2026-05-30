package com.joel.medicalrecordbook;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TimelineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_GROUP = 1;
    private static final int TYPE_NODE = 2;
    private static final int COLOR_NORMAL_DOT = Color.parseColor("#8FA1B7");
    private static final int COLOR_KEY_DOT = Color.parseColor("#E53935");
    private static final int COLOR_NORMAL_CARD = Color.WHITE;
    private static final int COLOR_KEY_CARD = Color.parseColor("#FFF7F7");

    private final List<RowItem> rows = new ArrayList<>();
    private final List<DiseaseNodeEntity> sourceNodes = new ArrayList<>();
    private final Set<String> collapsedDiseaseNames = new LinkedHashSet<>();
    private final OnTimelineItemActionListener actionListener;

    public TimelineAdapter(OnTimelineItemActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_GROUP) {
            View view = inflater.inflate(R.layout.item_disease_group_header, parent, false);
            return new GroupViewHolder(view);
        }

        View view = inflater.inflate(R.layout.item_timeline_node, parent, false);
        return new TimelineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RowItem row = rows.get(position);
        if (holder instanceof GroupViewHolder) {
            bindGroup((GroupViewHolder) holder, row.group);
        } else if (holder instanceof TimelineViewHolder) {
            bindNode((TimelineViewHolder) holder, row.node, row.nodeIndex, row.nodeCount);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    public void submitList(List<DiseaseNodeEntity> newNodes) {
        sourceNodes.clear();
        if (newNodes != null) {
            sourceNodes.addAll(newNodes);
        }
        rebuildRows();
    }

    private void rebuildRows() {
        rows.clear();

        Map<String, DiseaseGroup> groups = new LinkedHashMap<>();
        for (DiseaseNodeEntity node : sourceNodes) {
            DiseaseGroup group = groups.get(node.getDiseaseName());
            if (group == null) {
                group = new DiseaseGroup(node.getDiseaseName());
                groups.put(node.getDiseaseName(), group);
            }
            group.nodes.add(node);
            if (group.startDate == null || node.getDate().compareTo(group.startDate) < 0) {
                group.startDate = node.getDate();
            }
            if (group.endDate == null || node.getDate().compareTo(group.endDate) > 0) {
                group.endDate = node.getDate();
            }
        }

        for (DiseaseGroup group : groups.values()) {
            group.collapsed = collapsedDiseaseNames.contains(group.diseaseName);
            rows.add(RowItem.group(group));
            if (!group.collapsed) {
                for (int i = 0; i < group.nodes.size(); i++) {
                    rows.add(RowItem.node(group.nodes.get(i), i, group.nodes.size()));
                }
            }
        }

        notifyDataSetChanged();
    }

    private void bindGroup(GroupViewHolder holder, DiseaseGroup group) {
        holder.tvDiseaseName.setText(group.diseaseName);
        holder.tvDiseaseSummary.setText(group.startDate + " 至 " + group.endDate + " · " + group.nodes.size() + " 条记录");
        holder.tvExpandState.setText(group.collapsed ? "展开" : "收起");

        holder.itemView.setOnClickListener(v -> {
            if (collapsedDiseaseNames.contains(group.diseaseName)) {
                collapsedDiseaseNames.remove(group.diseaseName);
            } else {
                collapsedDiseaseNames.add(group.diseaseName);
            }
            rebuildRows();
        });
    }

    private void bindNode(TimelineViewHolder holder, DiseaseNodeEntity node, int nodeIndex, int nodeCount) {
        Context context = holder.itemView.getContext();

        holder.tvDate.setText(node.getDate());
        holder.tvDayCount.setText("病程第 " + (nodeIndex + 1) + " 条");
        holder.tvSymptoms.setText(node.getSymptoms());

        holder.viewLineTop.setVisibility(nodeIndex == 0 ? View.INVISIBLE : View.VISIBLE);
        holder.viewLineBottom.setVisibility(nodeIndex == nodeCount - 1 ? View.INVISIBLE : View.VISIBLE);

        if (node.isKeyNode()) {
            holder.viewNodeDot.setBackground(createCircleDrawable(COLOR_KEY_DOT, Color.WHITE, dp(context, 3)));
            holder.cardTimeline.setCardBackgroundColor(COLOR_KEY_CARD);
            holder.tvKeyNodeTag.setVisibility(View.VISIBLE);
            holder.tvDate.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        } else {
            holder.viewNodeDot.setBackground(createCircleDrawable(COLOR_NORMAL_DOT, Color.WHITE, dp(context, 3)));
            holder.cardTimeline.setCardBackgroundColor(COLOR_NORMAL_CARD);
            holder.tvKeyNodeTag.setVisibility(View.GONE);
            holder.tvDate.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        bindImages(holder, parseImagePaths(node.getImagePath()));
        bindOptionalText(holder.layoutDiagnosis, holder.tvDiagnosis, node.getDiagnosis());
        bindOptionalText(holder.layoutMedications, holder.tvMedications, node.getMedications());
        bindNotes(holder.tvNotes, node.getNotes());

        holder.itemView.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onEdit(node);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDelete(node);
            }
            return true;
        });
    }

    private void bindImages(TimelineViewHolder holder, List<String> images) {
        holder.layoutImages.removeAllViews();

        if (images == null || images.isEmpty()) {
            holder.hsvImages.setVisibility(View.GONE);
            return;
        }

        holder.hsvImages.setVisibility(View.VISIBLE);
        Context context = holder.itemView.getContext();

        for (String imagePath : images) {
            ImageView imageView = new ImageView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(context, 96), dp(context, 96));
            params.setMarginEnd(dp(context, 10));
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundColor(Color.parseColor("#E6EAF0"));

            Object loadTarget = imagePath.startsWith("content:")
                    || imagePath.startsWith("file:")
                    ? Uri.parse(imagePath)
                    : new File(imagePath);

            Glide.with(context)
                    .load(loadTarget)
                    .placeholder(new ColorDrawable(Color.parseColor("#D9E2EC")))
                    .error(new ColorDrawable(Color.parseColor("#D9E2EC")))
                    .centerCrop()
                    .into(imageView);

            holder.layoutImages.addView(imageView);
        }
    }

    private void bindOptionalText(View container, TextView textView, String value) {
        if (isBlank(value)) {
            container.setVisibility(View.GONE);
            textView.setText("");
            return;
        }

        container.setVisibility(View.VISIBLE);
        textView.setText(value);
    }

    private void bindNotes(TextView textView, String value) {
        if (isBlank(value)) {
            textView.setVisibility(View.GONE);
            textView.setText("");
            return;
        }

        textView.setVisibility(View.VISIBLE);
        textView.setText("备注：" + value);
    }

    private List<String> parseImagePaths(String imagePath) {
        List<String> result = new ArrayList<>();
        if (isBlank(imagePath)) {
            return result;
        }

        String[] paths = imagePath.split(",");
        for (String path : paths) {
            String trimmed = path.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private GradientDrawable createCircleDrawable(int fillColor, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(fillColor);
        drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    private int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public interface OnTimelineItemActionListener {
        void onEdit(DiseaseNodeEntity node);

        void onDelete(DiseaseNodeEntity node);
    }

    private static class DiseaseGroup {
        final String diseaseName;
        final List<DiseaseNodeEntity> nodes = new ArrayList<>();
        String startDate;
        String endDate;
        boolean collapsed;

        DiseaseGroup(String diseaseName) {
            this.diseaseName = diseaseName;
        }
    }

    private static class RowItem {
        final int type;
        final DiseaseGroup group;
        final DiseaseNodeEntity node;
        final int nodeIndex;
        final int nodeCount;

        private RowItem(int type, DiseaseGroup group, DiseaseNodeEntity node, int nodeIndex, int nodeCount) {
            this.type = type;
            this.group = group;
            this.node = node;
            this.nodeIndex = nodeIndex;
            this.nodeCount = nodeCount;
        }

        static RowItem group(DiseaseGroup group) {
            return new RowItem(TYPE_GROUP, group, null, 0, 0);
        }

        static RowItem node(DiseaseNodeEntity node, int nodeIndex, int nodeCount) {
            return new RowItem(TYPE_NODE, null, node, nodeIndex, nodeCount);
        }
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView tvDiseaseName;
        TextView tvDiseaseSummary;
        TextView tvExpandState;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDiseaseName = itemView.findViewById(R.id.tvDiseaseName);
            tvDiseaseSummary = itemView.findViewById(R.id.tvDiseaseSummary);
            tvExpandState = itemView.findViewById(R.id.tvExpandState);
        }
    }

    static class TimelineViewHolder extends RecyclerView.ViewHolder {
        View viewLineTop;
        View viewLineBottom;
        View viewNodeDot;
        CardView cardTimeline;
        TextView tvDate;
        TextView tvDayCount;
        TextView tvKeyNodeTag;
        TextView tvSymptoms;
        HorizontalScrollView hsvImages;
        LinearLayout layoutImages;
        LinearLayout layoutDiagnosis;
        TextView tvDiagnosis;
        LinearLayout layoutMedications;
        TextView tvMedications;
        TextView tvNotes;

        TimelineViewHolder(@NonNull View itemView) {
            super(itemView);
            viewLineTop = itemView.findViewById(R.id.viewLineTop);
            viewLineBottom = itemView.findViewById(R.id.viewLineBottom);
            viewNodeDot = itemView.findViewById(R.id.viewNodeDot);
            cardTimeline = itemView.findViewById(R.id.cardTimeline);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDayCount = itemView.findViewById(R.id.tvDayCount);
            tvKeyNodeTag = itemView.findViewById(R.id.tvKeyNodeTag);
            tvSymptoms = itemView.findViewById(R.id.tvSymptoms);
            hsvImages = itemView.findViewById(R.id.hsvImages);
            layoutImages = itemView.findViewById(R.id.layoutImages);
            layoutDiagnosis = itemView.findViewById(R.id.layoutDiagnosis);
            tvDiagnosis = itemView.findViewById(R.id.tvDiagnosis);
            layoutMedications = itemView.findViewById(R.id.layoutMedications);
            tvMedications = itemView.findViewById(R.id.tvMedications);
            tvNotes = itemView.findViewById(R.id.tvNotes);
        }
    }
}
