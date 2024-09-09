package com.duymanh.voicerecorder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecordingAdapter recordingAdapter;
    private Button btnStart, btnStop, btnPlay;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String fileName;
    private TextView tvTimer;

    private Handler handler;
    private Runnable updateTimer;
    private long startTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<String> recordings = getRecordingFiles();
        recordingAdapter = new RecordingAdapter(recordings, new RecordingAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String filePath) {
                playRecording(filePath);
            }
        });
        recyclerView.setAdapter(recordingAdapter);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnPlay = findViewById(R.id.btnPlay);
        tvTimer = findViewById(R.id.tvTimer);

        handler = new Handler();

        // Kiểm tra và yêu cầu quyền ghi âm và lưu trữ nếu chưa được cấp
        if (checkPermissions()) {
            initializeUI();
        } else {
            requestPermissions();
        }
    }

    private void initializeUI() {
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecording();
                btnStart.setEnabled(false);
                btnStop.setEnabled(true);
                btnPlay.setEnabled(false);
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecording();
                btnStart.setEnabled(true);
                btnStop.setEnabled(false);
                btnPlay.setEnabled(true);
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRecordingList();
            }
        });
    }


    private void showRecordingList() {
        List<String> recordings = getRecordingFiles();
        if (recordings.isEmpty()) {
            Toast.makeText(this, "Không có file ghi âm nào.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn file để phát");

        // Tạo danh sách để hiển thị trong dialog
        CharSequence[] items = new CharSequence[recordings.size()];
        for (int i = 0; i < recordings.size(); i++) {
            items[i] = new File(recordings.get(i)).getName();
        }

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                playRecording(recordings.get(which));
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void startRecording() {
        // Sử dụng thư mục riêng của ứng dụng để lưu trữ
        String timestamp = String.valueOf(System.currentTimeMillis());
        File outputDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (outputDir != null) {
            fileName = outputDir.getAbsolutePath() + "/recordingxx_" + timestamp + ".3gp";
        }

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(fileName);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            startTimer();
            Toast.makeText(this, "Bắt đầu ghi âm", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<String> getRecordingFiles() {
        List<String> recordingFiles = new ArrayList<>();
        File outputDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (outputDir != null) {
            File[] files = outputDir.listFiles((dir, name) -> name.endsWith(".3gp"));
            if (files != null) {
                for (File file : files) {
                    recordingFiles.add(file.getAbsolutePath());
                }
            }
        }
        return recordingFiles;
    }


    private void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            stopTimer();
            Toast.makeText(this, "Đã lưu ghi âm: " + fileName, Toast.LENGTH_SHORT).show();
        }
    }

    private void playRecording(String filePath) {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            startTimer();
            Toast.makeText(this, "Đang phát ghi âm", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopTimer();
                Toast.makeText(MainActivity.this, "Đã phát xong", Toast.LENGTH_SHORT).show();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        });
    }

    private void startTimer() {
        startTime = System.currentTimeMillis();
        updateTimer = new Runnable() {
            @Override
            public void run() {
                long elapsedTime = System.currentTimeMillis() - startTime;
                int seconds = (int) (elapsedTime / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;

                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateTimer);
    }

    private void stopTimer() {
        handler.removeCallbacks(updateTimer);
        tvTimer.setText("00:00");
    }

    // Kiểm tra quyền
    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    // Yêu cầu quyền
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, 1000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeUI();
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền để sử dụng ứng dụng", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
