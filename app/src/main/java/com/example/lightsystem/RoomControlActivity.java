package com.example.lightsystem;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class RoomControlActivity extends Activity {
    private TextView roomNameText;
    private Button offBtn, setTimerBtn, emergencyBtn, bluetoothButton;
    private SeekBar brightnessSeekBar;
    private EditText timerInput;
    private Handler timerHandler;
    private Handler emergencyHandler;
    private int roomId;
    private ColorPickerView colorPickerView;
    private boolean isEmergencyMode = false;
    private Runnable emergencyBlinkRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.room_control_activity);

        // Get room ID and name from intent
        roomId = getIntent().getIntExtra("ROOM_ID", 0);
        String roomName = getIntent().getStringExtra("ROOM_NAME");

        // Initialize views
        roomNameText = findViewById(R.id.roomNameText);
        offBtn = findViewById(R.id.offBtn);
        brightnessSeekBar = findViewById(R.id.brightnessSeekBar);
        timerInput = findViewById(R.id.timerInput);
        setTimerBtn = findViewById(R.id.setTimerBtn);
        colorPickerView = findViewById(R.id.colorPickerView);
        emergencyBtn = findViewById(R.id.emergencyBtn);
        bluetoothButton = findViewById(R.id.bluetoothButton);

        roomNameText.setText(roomName);
        timerHandler = new Handler();
        emergencyHandler = new Handler();

        // Set initial brightness to 100%
        brightnessSeekBar.setProgress(255);
        sendCommand("BRIGHTNESS:255:" + roomId);

        // Set color picker listener
        colorPickerView.setOnColorSelectedListener(color -> {
            if (!isEmergencyMode) {
                int red = Color.red(color);
                int green = Color.green(color);
                int blue = Color.blue(color);
                // Renkleri tersine çeviriyoruz (255 - renk)
                sendCommand(String.format("RGB:%d:%d:%d:%d", 255 - red, 255 - green, 255 - blue, roomId));
            }
        });

        // Emergency button listener
        emergencyBtn.setOnClickListener(v -> {
            isEmergencyMode = !isEmergencyMode;
            if (isEmergencyMode) {
                emergencyBtn.setText("Emergency Stop");
                startEmergencyMode();
            } else {
                emergencyBtn.setText("Emergency");
                stopEmergencyMode();
            }
        });

        // Bluetooth connection status
        if (MainActivity.outputStream != null) {
            bluetoothButton.setText("Bluetooth Connected");
        }

        offBtn.setOnClickListener(v -> {
            if (!isEmergencyMode) {
                sendCommand("OFF" + roomId);
            }
        });

        brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !isEmergencyMode) {
                    sendCommand("BRIGHTNESS:" + progress + ":" + roomId);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        setTimerBtn.setOnClickListener(v -> {
            if (!isEmergencyMode) {
                String timerValue = timerInput.getText().toString();
                if (!timerValue.isEmpty()) {
                    int minutes = Integer.parseInt(timerValue);
                    startTimer(minutes);
                }
            }
        });
    }

    private void startEmergencyMode() {
        emergencyBlinkRunnable = new Runnable() {
            boolean isLedOn = true;

            @Override
            public void run() {
                if (isEmergencyMode) {
                    if (isLedOn) {
                        sendCommand(String.format("RGB:%d:%d:%d:%d", 255, 0, 0, roomId)); // Kırmızı
                    } else {
                        sendCommand("OFF" + roomId);
                    }
                    isLedOn = !isLedOn;
                    emergencyHandler.postDelayed(this, 1000); // 1 saniye aralıkla yanıp sönme
                }
            }
        };
        emergencyHandler.post(emergencyBlinkRunnable);
    }

    private void stopEmergencyMode() {
        emergencyHandler.removeCallbacks(emergencyBlinkRunnable);
        sendCommand("OFF" + roomId);
    }

    private void sendCommand(String command) {
        if (MainActivity.outputStream != null) {
            try {
                MainActivity.outputStream.write((command + "\n").getBytes());
                Toast.makeText(this, "Komut gönderildi: " + command, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Komut gönderilemedi", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Bluetooth bağlantısı yok", Toast.LENGTH_SHORT).show();
        }
    }

    private void startTimer(int minutes) {
        Toast.makeText(this, minutes + " dakika sonra LED kapanacak", Toast.LENGTH_SHORT).show();
        timerHandler.postDelayed(() -> {
            sendCommand("OFF" + roomId);
            Toast.makeText(this, "Zamanlayıcı sona erdi, LED kapatıldı", Toast.LENGTH_SHORT).show();
        }, minutes * 60 * 1000L);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacksAndMessages(null);
        emergencyHandler.removeCallbacksAndMessages(null);
    }
}