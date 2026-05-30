package com.joel.medicalrecordbook;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends ComponentActivity {

    private static final String DEFAULT_DISEASE_NAME = "默认病程";
    private static final int RANGE_WEEK = 7;
    private static final int RANGE_MONTH = 30;
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    private RecyclerView rvTimeline;
    private TextView tvPageSubtitle;
    private TextView tvDateRange;
    private MaterialButtonToggleGroup toggleRangeMode;
    private TimelineAdapter timelineAdapter;
    private DiseaseViewModel diseaseViewModel;
    private LiveData<List<DiseaseNodeEntity>> currentNodesLiveData;
    private GestureDetector swipeDetector;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private DialogState activeDialogState;

    private String currentDiseaseName = DEFAULT_DISEASE_NAME;
    private Calendar windowAnchor = Calendar.getInstance();
    private int rangeDays = RANGE_MONTH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerImagePicker();
        setupStatusBar();
        initViews();
        initViewModel();
    }

    private void registerImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (activeDialogState == null || uris == null || uris.isEmpty()) {
                        return;
                    }

                    for (Uri uri : uris) {
                        String savedPath = copyUriToPrivateFile(uri);
                        if (!isBlank(savedPath)) {
                            activeDialogState.selectedImagePaths.add(savedPath);
                        }
                    }
                    refreshImagePreview(activeDialogState);
                }
        );
    }

    private void setupStatusBar() {
        Window window = getWindow();
        window.setStatusBarColor(getColor(android.R.color.white));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private void initViews() {
        rvTimeline = findViewById(R.id.rvTimeline);
        tvPageSubtitle = findViewById(R.id.tvPageSubtitle);
        tvDateRange = findViewById(R.id.tvDateRange);
        toggleRangeMode = findViewById(R.id.toggleRangeMode);
        FloatingActionButton fabAddNode = findViewById(R.id.fabAddNode);

        timelineAdapter = new TimelineAdapter(new TimelineAdapter.OnTimelineItemActionListener() {
            @Override
            public void onEdit(DiseaseNodeEntity node) {
                showNodeDialog(node);
            }

            @Override
            public void onDelete(DiseaseNodeEntity node) {
                confirmDelete(node);
            }
        });

        rvTimeline.setLayoutManager(new LinearLayoutManager(this));
        rvTimeline.setHasFixedSize(false);
        rvTimeline.setAdapter(timelineAdapter);

        swipeDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) {
                    return false;
                }
                float diffX = e1.getX() - e2.getX();
                float diffY = e1.getY() - e2.getY();
                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 120 && Math.abs(velocityX) > 120) {
                    if (diffX > 0) {
                        shiftWindow(1);
                    } else {
                        shiftWindow(-1);
                    }
                    return true;
                }
                return false;
            }
        });

        rvTimeline.setOnTouchListener((v, event) -> {
            swipeDetector.onTouchEvent(event);
            return false;
        });

        toggleRangeMode.check(R.id.btnRangeMonth);
        toggleRangeMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.btnRangeWeek) {
                rangeDays = RANGE_WEEK;
            } else {
                rangeDays = RANGE_MONTH;
            }
            windowAnchor = Calendar.getInstance();
            refreshCurrentWindow();
        });

        fabAddNode.setOnClickListener(v -> showNodeDialog(null));
    }

    private void initViewModel() {
        diseaseViewModel = new ViewModelProvider(this).get(DiseaseViewModel.class);
        refreshCurrentWindow();
    }

    private void refreshCurrentWindow() {
        if (diseaseViewModel == null) {
            return;
        }

        Calendar startCalendar;
        Calendar endCalendar;

        if (rangeDays == RANGE_WEEK) {
            startCalendar = startOfWeek(windowAnchor);
            endCalendar = (Calendar) startCalendar.clone();
            endCalendar.add(Calendar.DAY_OF_YEAR, 6);
        } else {
            endCalendar = (Calendar) windowAnchor.clone();
            startCalendar = (Calendar) endCalendar.clone();
            startCalendar.add(Calendar.DAY_OF_YEAR, -(RANGE_MONTH - 1));
        }

        Calendar today = startOfDay(Calendar.getInstance());
        startCalendar = startOfDay(startCalendar);
        endCalendar = startOfDay(endCalendar);
        if (startCalendar.after(today)) {
            Toast.makeText(this, "不能查看未来日期的病历记录", Toast.LENGTH_SHORT).show();
            windowAnchor = today;
            refreshCurrentWindow();
            return;
        }
        if (endCalendar.after(today)) {
            endCalendar = today;
        }

        String startDate = formatDate(startCalendar);
        String endDate = formatDate(endCalendar);

        tvPageSubtitle.setText("所有病历 · 疾病全周期记录");
        tvDateRange.setText((rangeDays == RANGE_WEEK ? "按周" : "按月") + " · " + startDate + " 至 " + endDate);

        if (currentNodesLiveData != null) {
            currentNodesLiveData.removeObservers(this);
        }

        currentNodesLiveData = diseaseViewModel.getNodesByDateRange(startDate, endDate);
        currentNodesLiveData.observe(this, nodes -> timelineAdapter.submitList(nodes));
    }

    private void shiftWindow(int direction) {
        windowAnchor.add(Calendar.DAY_OF_YEAR, direction * rangeDays);
        refreshCurrentWindow();
    }

    private Calendar startOfDay(Calendar source) {
        Calendar calendar = (Calendar) source.clone();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private Calendar startOfWeek(Calendar source) {
        Calendar calendar = (Calendar) source.clone();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        return calendar;
    }

    private void showNodeDialog(DiseaseNodeEntity editingNode) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_add_disease_node, null, false);

        EditText etDiseaseName = dialogView.findViewById(R.id.etDiseaseName);
        EditText etDate = dialogView.findViewById(R.id.etDate);
        CheckBox cbKeyNode = dialogView.findViewById(R.id.cbKeyNode);
        EditText etSymptoms = dialogView.findViewById(R.id.etSymptoms);
        MaterialButton btnPickImages = dialogView.findViewById(R.id.btnPickImages);
        TextView tvImageSummary = dialogView.findViewById(R.id.tvImageSummary);
        LinearLayout layoutImagePreview = dialogView.findViewById(R.id.layoutImagePreview);
        EditText etDiagnosis = dialogView.findViewById(R.id.etDiagnosis);
        EditText etMedications = dialogView.findViewById(R.id.etMedications);
        EditText etNotes = dialogView.findViewById(R.id.etNotes);

        DialogState dialogState = new DialogState();
        dialogState.tvImageSummary = tvImageSummary;
        dialogState.layoutImagePreview = layoutImagePreview;
        dialogState.selectedImagePaths = new ArrayList<>();
        activeDialogState = dialogState;

        if (editingNode == null) {
            etDiseaseName.setText(currentDiseaseName);
            etDate.setText(today());
        } else {
            etDiseaseName.setText(editingNode.getDiseaseName());
            etDate.setText(editingNode.getDate());
            cbKeyNode.setChecked(editingNode.isKeyNode());
            etSymptoms.setText(editingNode.getSymptoms());
            etDiagnosis.setText(editingNode.getDiagnosis());
            etMedications.setText(editingNode.getMedications());
            etNotes.setText(editingNode.getNotes());
            dialogState.selectedImagePaths.addAll(parseImagePaths(editingNode.getImagePath()));
            refreshImagePreview(dialogState);
        }

        btnPickImages.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editingNode == null ? "添加病历记录" : "编辑病历记录")
                .setView(dialogView)
                .setNegativeButton("取消", (d, which) -> activeDialogState = null)
                .setPositiveButton("保存", null)
                .create();

        dialog.setOnDismissListener(d -> {
            if (activeDialogState == dialogState) {
                activeDialogState = null;
            }
        });

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String diseaseName = readText(etDiseaseName);
                    String date = readText(etDate);
                    String symptoms = readText(etSymptoms);

                    if (diseaseName.isEmpty()) {
                        etDiseaseName.setError("请填写疾病名称");
                        return;
                    }

                    if (date.isEmpty()) {
                        etDate.setError("请填写记录日期");
                        return;
                    }
                    if (!isValidRecordDate(date)) {
                        etDate.setError("记录日期不能晚于今天");
                        return;
                    }

                    if (symptoms.isEmpty()) {
                        etSymptoms.setError("请填写症状描述");
                        return;
                    }

                    String imagePath = joinImagePaths(dialogState.selectedImagePaths);

                    if (editingNode == null) {
                        DiseaseNodeEntity node = new DiseaseNodeEntity(
                                diseaseName,
                                date,
                                cbKeyNode.isChecked(),
                                symptoms,
                                imagePath,
                                readText(etDiagnosis),
                                readText(etMedications),
                                readText(etNotes)
                        );
                        diseaseViewModel.insert(node);
                    } else {
                        editingNode.setDiseaseName(diseaseName);
                        editingNode.setDate(date);
                        editingNode.setKeyNode(cbKeyNode.isChecked());
                        editingNode.setSymptoms(symptoms);
                        editingNode.setImagePath(imagePath);
                        editingNode.setDiagnosis(readText(etDiagnosis));
                        editingNode.setMedications(readText(etMedications));
                        editingNode.setNotes(readText(etNotes));
                        diseaseViewModel.update(editingNode);
                    }

                    currentDiseaseName = diseaseName;
                    refreshCurrentWindow();
                    Toast.makeText(this, "已保存到本地病历本", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }));

        dialog.show();
    }

    private void refreshImagePreview(DialogState state) {
        state.tvImageSummary.setText(state.selectedImagePaths.isEmpty()
                ? "未选择图片"
                : "已选择 " + state.selectedImagePaths.size() + " 张图片");

        state.layoutImagePreview.removeAllViews();
        for (String path : state.selectedImagePaths) {
            View imageFrame = LayoutInflater.from(this).inflate(R.layout.item_image_preview, state.layoutImagePreview, false);
            android.widget.ImageView imageView = imageFrame.findViewById(R.id.ivPreview);
            Object loadTarget = path.startsWith("content:") || path.startsWith("file:")
                    ? Uri.parse(path)
                    : new File(path);
            Glide.with(this).load(loadTarget).centerCrop().into(imageView);
            state.layoutImagePreview.addView(imageFrame);
        }
    }

    private void confirmDelete(DiseaseNodeEntity node) {
        new AlertDialog.Builder(this)
                .setTitle("删除病历记录")
                .setMessage("确定删除这条记录吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    diseaseViewModel.delete(node);
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private String copyUriToPrivateFile(Uri uri) {
        File dir = new File(getFilesDir(), "disease_images");
        if (!dir.exists() && !dir.mkdirs()) {
            Toast.makeText(this, "图片目录创建失败", Toast.LENGTH_SHORT).show();
            return "";
        }

        String extension = resolveExtension(uri);
        String fileName = "img_" + timestamp() + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        File outFile = new File(dir, fileName);

        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                return "";
            }

            try (FileOutputStream outputStream = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
                outputStream.flush();
                return outFile.getAbsolutePath();
            }
        } catch (IOException e) {
            Toast.makeText(this, "图片保存失败", Toast.LENGTH_SHORT).show();
            return "";
        }
    }

    private String resolveExtension(Uri uri) {
        String type = getContentResolver().getType(uri);
        if (type != null) {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
            if (ext != null) {
                return ext;
            }
        }
        return "jpg";
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

    private String joinImagePaths(List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (String path : paths) {
            if (isBlank(path)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(path);
        }
        return builder.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String readText(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private String today() {
        return formatDate(Calendar.getInstance());
    }

    private String timestamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(new Date());
    }

    private String formatDate(Calendar calendar) {
        return new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(calendar.getTime());
    }

    private boolean isValidRecordDate(String dateText) {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault());
        formatter.setLenient(false);
        try {
            Date parsedDate = formatter.parse(dateText);
            return parsedDate != null && !parsedDate.after(startOfDay(Calendar.getInstance()).getTime());
        } catch (ParseException e) {
            return false;
        }
    }

    private static class DialogState {
        TextView tvImageSummary;
        LinearLayout layoutImagePreview;
        List<String> selectedImagePaths;
    }
}
