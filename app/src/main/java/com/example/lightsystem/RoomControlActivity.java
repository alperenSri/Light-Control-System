package com.example.lightsystem;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class RoomControlActivity extends Activity {
    private TextView roomNameText;
    private Button redBtn, greenBtn, blueBtn, offBtn, setTimerBtn;
    private SeekBar brightnessSeekBar;
    private EditText timerInput;
    private Handler timerHandler;
    private int roomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.room_control_activity);

        // Get room ID and name from intent
        roomId = getIntent().getIntExtra("ROOM_ID", 0);
        String roomName = getIntent().getStringExtra("ROOM_NAME");

        // Initialize views
        roomNameText = findViewById(R.id.roomNameText);
        redBtn = findViewById(R.id.redBtn);
        greenBtn = findViewById(R.id.greenBtn);
        blueBtn = findViewById(R.id.blueBtn);
        offBtn = findViewById(R.id.offBtn);
        brightnessSeekBar = findViewById(R.id.brightnessSeekBar);
        timerInput = findViewById(R.id.timerInput);
        setTimerBtn = findViewById(R.id.setTimerBtn);

        roomNameText.setText(roomName);
        timerHandler = new Handler();

        // Set click listeners
        redBtn.setOnClickListener(v -> sendCommand("RED" + roomId));
        greenBtn.setOnClickListener(v -> sendCommand("GREEN" + roomId));
        blueBtn.setOnClickListener(v -> sendCommand("BLUE" + roomId));
        offBtn.setOnClickListener(v -> sendCommand("OFF" + roomId));

        brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
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
            String timerValue = timerInput.getText().toString();
            if (!timerValue.isEmpty()) {
                int minutes = Integer.parseInt(timerValue);
                startTimer(minutes);
            }
        });
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
    }
}