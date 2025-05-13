package com.example.lightsystem;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
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
    public static OutputStream outputStream;

    private Button connectBtn, emergencyBtn;
    private CardView room1Card, room2Card, room3Card;

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Initialize views
        connectBtn = findViewById(R.id.connectBtn);
        emergencyBtn = findViewById(R.id.emergencyBtn);
        room1Card = findViewById(R.id.room1Card);
        room2Card = findViewById(R.id.room2Card);
        room3Card = findViewById(R.id.room3Card);

        // Check Bluetooth permissions
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { android.Manifest.permission.BLUETOOTH_CONNECT },
                    REQUEST_BLUETOOTH_CONNECT);
        }

        // Set click listeners
        connectBtn.setOnClickListener(v -> connectToBluetooth());

        room1Card.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RoomControlActivity.class);
            intent.putExtra("ROOM_ID", 1);
            intent.putExtra("ROOM_NAME", "Living Room");
            startActivity(intent);
        });

        room2Card.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RoomControlActivity.class);
            intent.putExtra("ROOM_ID", 2);
            intent.putExtra("ROOM_NAME", "Bedroom");
            startActivity(intent);
        });

        room3Card.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RoomControlActivity.class);
            intent.putExtra("ROOM_ID", 3);
            intent.putExtra("ROOM_NAME", "Kitchen");
            startActivity(intent);
        });

        emergencyBtn.setOnClickListener(v -> activateEmergencyMode());
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

    private void activateEmergencyMode() {
        if (outputStream != null) {
            try {
                // Turn all LEDs red
                outputStream.write("EMERGENCY\n".getBytes());
                Toast.makeText(this, "Acil durum modu aktif", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "Komut gönderilemedi", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Bluetooth bağlantısı yok", Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}