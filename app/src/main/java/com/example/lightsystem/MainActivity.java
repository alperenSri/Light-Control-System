package com.example.lightsystem;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log; // Log sınıfını import et
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

    // Log mesajları için bir TAG tanımla
    private static final String TAG = "MainActivity";

    private static final String DEVICE_NAME = "ESP32_LED";
    private static final UUID UUID_PORT = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_BLUETOOTH_CONNECT = 100;

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice device;
    BluetoothSocket bluetoothSocket;
    OutputStream outputStream;

    Button connectBtn, redBtn, greenBtn, blueBtn, offBtn, emergencyBtn; // emergencyBtn eklendi

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
        emergencyBtn = findViewById(R.id.emergencyBtn); // emergencyBtn ID'si ile bağla

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        SeekBar brightnessSeekBar = findViewById(R.id.brightnessSeekBar);

        brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Parlaklık değeri 0-255 arası
                sendData("BRIGHT:" + progress + "\n");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Bluetooth bağlantı izni kontrolü ve isteme
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { android.Manifest.permission.BLUETOOTH_CONNECT },
                    REQUEST_BLUETOOTH_CONNECT);
        }

        // Buton tıklama olayları
        connectBtn.setOnClickListener(v -> connectToBluetooth());

        redBtn.setOnClickListener(v -> sendData("RED\n"));
        greenBtn.setOnClickListener(v -> sendData("GREEN\n"));
        blueBtn.setOnClickListener(v -> sendData("BLUE\n"));
        offBtn.setOnClickListener(v -> sendData("OFF\n"));

        // Emergency butonu tıklama olayı
        emergencyBtn.setOnClickListener(v -> sendData("EMERGENCY_ON\n")); // Emergency komutunu gönder
    }

    private void connectToBluetooth() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth aktif değil", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Bluetooth is not active or not supported."); // Log warning
            return;
        }

        // İzin tekrar kontrol ediliyor (bazı senaryolar için faydalı)
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth izni gerekiyor", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Bluetooth connect permission not granted."); // Log warning
            return;
        }

        // Eşleşmiş cihazlar arasında ESP32'yi bul
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        device = null; // Cihazı sıfırla
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice pairedDevice : pairedDevices) {
                Log.d(TAG, "Found paired device: " + pairedDevice.getName() + " - " + pairedDevice.getAddress()); // Log paired devices
                if (pairedDevice.getName().equals(DEVICE_NAME)) {
                    device = pairedDevice;
                    break;
                }
            }
        } else {
            Toast.makeText(this, "Eşleşmiş cihaz bulunamadı.", Toast.LENGTH_LONG).show();
            Log.w(TAG, "No paired devices found."); // Log warning
            return;
        }


        if (device == null) {
            Toast.makeText(this, "ESP32 cihazı (" + DEVICE_NAME + ") bulunamadı. Eşleştiğinizden emin olun.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "ESP32 device (" + DEVICE_NAME + ") not found in paired devices."); // Log error
            return;
        }

        // Bluetooth soketi oluştur ve bağlan
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID_PORT);
            Log.i(TAG, "Attempting to connect to socket..."); // Log info
            // Bağlantı ayrı bir thread'de yapılmalı idealde, UI thread'ini bloklamamak için.
            // Basit örnek için burada yapılıyor.
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            Toast.makeText(this, "Bağlantı başarılı: " + DEVICE_NAME, Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Connection successful to " + DEVICE_NAME); // Log info
        } catch (IOException e) {
            Toast.makeText(this, "Bağlantı başarısız: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Connection failed: " + e.getMessage(), e); // Log error with exception
        }
    }

    private void sendData(String data) {
        try {
            if (outputStream != null) {
                outputStream.write(data.getBytes());
                // Seri monitör yerine Android Logcat'e yazdır
                Log.i(TAG, "Sent: " + data.trim());
                // Toast.makeText(this, "Gönderildi: " + data.trim(), Toast.LENGTH_SHORT).show(); // Her gönderimde Toast göstermek spam yapabilir
            } else {
                Toast.makeText(this, "Bluetooth bağlı değil", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Cannot send data, Bluetooth not connected."); // Log warning
            }
        } catch (IOException e) {
            Toast.makeText(this, "Gönderim hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Send data error: " + e.getMessage(), e); // Log error with exception
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // Üst sınıfı çağır
        if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth İzni Verildi", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Bluetooth CONNECT permission granted."); // Log info
                // İzin verildikten sonra bağlantı tekrar denenebilir
                // connectToBluetooth(); // Opsiyonel: İzin verilince hemen bağlanmayı dene
            } else {
                Toast.makeText(this, "Bluetooth İzni Reddedildi. Bağlantı yapılamaz.", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Bluetooth CONNECT permission denied."); // Log warning
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Uygulama kapanırken Bluetooth bağlantısını kapat
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                // Seri monitör yerine Android Logcat'e yazdır
                Log.i(TAG, "Bluetooth socket closed.");
            }
        } catch (IOException e) {
            // Seri monitör yerine Android Logcat'e yazdır
            Log.e(TAG, "Error closing Bluetooth socket: " + e.getMessage(), e); // Log error with exception
        }
    }
}