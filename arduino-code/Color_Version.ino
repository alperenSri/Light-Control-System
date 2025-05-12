#include "BluetoothSerial.h"

// Bluetooth seri nesnesi
BluetoothSerial SerialBT;

// LED Pin Tanımları
#define RED_PIN 4
#define GREEN_PIN 2
#define BLUE_PIN 15

// Mevcut rengi tutmak için global değişken
// Başlangıçta boş veya "off" olarak ayarlanabilir.
String color = ""; // Global değişken

void setup() {
    // Seri haberleşmeyi başlat (debug için)
    Serial.begin(115200);
    // Bluetooth seri haberleşmeyi başlat
    SerialBT.begin("ESP32_LED"); // Bluetooth cihaz adını belirle

    // LED pinlerini çıkış olarak ayarla
    pinMode(RED_PIN, OUTPUT);
    pinMode(GREEN_PIN, OUTPUT);
    pinMode(BLUE_PIN, OUTPUT);

    // Başlangıçta LED'leri kapat (Common Anode varsayımıyla HIGH=Kapalı)
    digitalWrite(RED_PIN, HIGH);
    digitalWrite(GREEN_PIN, HIGH);
    digitalWrite(BLUE_PIN, HIGH);

    // color değişkeni burada tanımlıydı, yukarıya global olarak taşındı.
}

void loop() {
    // Bluetooth'dan veri geldiyse
    if (SerialBT.available()) {
        // Veriyi oku (satır sonuna kadar)
        String cmd = SerialBT.readStringUntil('\n');
        // Baştaki ve sondaki boşlukları temizle
        cmd.trim();

        // Gelen komutu seri monitöre yazdır (debug için)
        Serial.println("Gelen: " + cmd);

        // Gelen komutu kontrol et
        if (cmd == "RED") {
            // Kırmızı rengi ayarla (Common Anode: 0=Açık, 255=Kapalı)
            analogWrite(RED_PIN, 0);
            analogWrite(GREEN_PIN, 255);
            analogWrite(BLUE_PIN, 255);
            // Mevcut rengi kaydet
            color = "red";
        } else if (cmd == "GREEN") {
            // Yeşil rengi ayarla
            analogWrite(RED_PIN, 255);
            analogWrite(GREEN_PIN, 0);
            analogWrite(BLUE_PIN, 255);
            // Mevcut rengi kaydet
            color = "green";
        } else if (cmd == "BLUE") {
            // Mavi rengi ayarla
            analogWrite(RED_PIN, 255);
            analogWrite(GREEN_PIN, 255);
            analogWrite(BLUE_PIN, 0);
            // Mevcut rengi kaydet
            color = "blue";
        } else if (cmd == "OFF") {
            // LED'leri kapat
            analogWrite(RED_PIN, 255);
            analogWrite(GREEN_PIN, 255);
            analogWrite(BLUE_PIN, 255);
            // Renk durumunu "off" olarak da işaretleyebilirsiniz,
            // ancak mevcut mantık sadece red/green/blue için parlaklık ayarlıyor.
            // color = "off"; // İsteğe bağlı
        }
            // Parlaklık ayarlama komutu geldiyse VE mevcut renk kayıtlıysa
            // Bu blok artık SerialBT.available() kontrolünün İÇİNDE.
        else if (cmd.startsWith("BRIGHT:")) {
            // Parlaklık değerini al (substring(7) "BRIGHT:" den sonraki kısım)
            int brightness = cmd.substring(7).toInt();
            // Parlaklık değerini 0-255 arasına sınırla
            brightness = constrain(brightness, 0, 255);

            // Parlaklığı mevcut renge göre ayarla
            // Common Anode LED'ler için: 0=En Parlak, 255=Kapalı
            // Gelen parlaklık 0=Kapalı, 255=En Parlak kabul ediliyorsa,
            // analogWrite değeri = 255 - brightness olmalı.
            // Örnek: brightness 255 (en parlak) -> analogWrite(..., 0)
            // Örnek: brightness 0 (kapalı) -> analogWrite(..., 255)

            if (color == "red") {
                analogWrite(RED_PIN, 255 - brightness);
                // Diğer renkler kapalı kalmalı
                analogWrite(GREEN_PIN, 255);
                analogWrite(BLUE_PIN, 255);
            } else if (color == "green") {
                analogWrite(RED_PIN, 255);
                analogWrite(GREEN_PIN, 255 - brightness);
                analogWrite(BLUE_PIN, 255);
            } else if (color == "blue") {
                analogWrite(RED_PIN, 255);
                analogWrite(GREEN_PIN, 255);
                analogWrite(BLUE_PIN, 255 - brightness);
            }
            // Eğer color değişkeni "off" veya başka bir şeyse, parlaklık ayarı yapılmaz.
        }
    }
    // else if (cmd.startsWith("BRIGHT:") ... } bu kısım yukarıya taşındı
    // burası artık boş kaldı.
}