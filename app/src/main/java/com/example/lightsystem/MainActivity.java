package com.example.lightsystem;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    private static final String DEVICE_NAME = "ESP32_LED";
    private static final UUID UUID_PORT = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_BLUETOOTH_CONNECT = 100;

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice device;
    BluetoothSocket bluetoothSocket;
    OutputStream outputStream;

    Button connectBtn, redBtn, greenBtn, blueBtn, offBtn;

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectBtn = findViewById(R.id.connectBtn);
        redBtn = findViewById(R.id.redBtn);
        greenBtn = findViewById(R.id.greenBtn);
        blueBtn = findViewById(R.id.blueBtn);
        offBtn = findViewById(R.id.offBtn);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        SeekBar brightnessSeekBar = findViewById(R.id.brightnessSeekBar);

        brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 0-255 arası değer gönder
                sendData("BRIGHT:" + progress + "\n");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { android.Manifest.permission.BLUETOOTH_CONNECT },
                    REQUEST_BLUETOOTH_CONNECT);
        }

        connectBtn.setOnClickListener(v -> connectToBluetooth());

        redBtn.setOnClickListener(v -> sendData("RED\n"));
        greenBtn.setOnClickListener(v -> sendData("GREEN\n"));
        blueBtn.setOnClickListener(v -> sendData("BLUE\n"));
        offBtn.setOnClickListener(v -> sendData("OFF\n"));
    }

    private void connectToBluetooth() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth aktif değil", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth izni gerekiyor", Toast.LENGTH_SHORT).show();
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice pairedDevice : pairedDevices) {
            if (pairedDevice.getName().equals(DEVICE_NAME)) {
                device = pairedDevice;
                break;
            }
        }

        if (device == null) {
            Toast.makeText(this, "ESP32 bulunamadı", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID_PORT);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            Toast.makeText(this, "Bağlantı başarılı", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Bağlantı başarısız", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendData(String data) {
        try {
            if (outputStream != null) {
                outputStream.write(data.getBytes());
                Toast.makeText(this, "Gönderildi: " + data.trim(), Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Gönderim hatası", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_BLUETOOTH_CONNECT && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "İzin verildi", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "İzin reddedildi", Toast.LENGTH_SHORT).show();
        }
    }
}