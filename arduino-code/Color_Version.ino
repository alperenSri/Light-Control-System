#include "BluetoothSerial.h"

// Bluetooth seri nesnesi
BluetoothSerial SerialBT;

// LED Pin Tanımları
#define RED_PIN 4
#define GREEN_PIN 2
#define BLUE_PIN 15
// Buzzer Pin Tanımı (Kendi bağlantınıza göre değiştirin)
#define BUZZER_PIN 5

// Mevcut rengi tutmak için global değişken
String color = "";

// Acil durum modunu takip etmek için global değişken
bool isEmergency = false;

// Acil durum yanıp sönme ve çalma için zamanlama değişkenleri
unsigned long lastEmergencyToggle = 0;
const unsigned long emergencyInterval = 1000; // Yanıp sönme/çalma aralığı (1 saniye)
bool emergencyLedState = false; // Acil durum deseninin mevcut durumu (ON/OFF)

void setup() {
    // Seri haberleşmeyi başlat (debug için)
    Serial.begin(115200);
    // Bluetooth seri haberleşmeyi başlat
    SerialBT.begin("ESP32_LED"); // Bluetooth cihaz adını belirle

    // LED ve Buzzer pinlerini çıkış olarak ayarla
    pinMode(RED_PIN, OUTPUT);
    pinMode(GREEN_PIN, OUTPUT);
    pinMode(BLUE_PIN, OUTPUT);
    // Hata düzeltmesi: BUZZER_PIN için pinMode'da HIGH/LOW yerine OUTPUT kullanıldı
    pinMode(BUZZER_PIN, OUTPUT); // Buzzer pinini çıkış olarak ayarla

    // Başlangıçta LED'leri ve Buzzer'ı kapat
    // Common Anode varsayımıyla HIGH=Kapalı, LOW=Açık (analogWrite için 0=Açık, 255=Kapalı)
    digitalWrite(RED_PIN, HIGH);
    digitalWrite(GREEN_PIN, HIGH);
    digitalWrite(BLUE_PIN, HIGH);
    // Hata düzeltmesi: BUZZER_PIN için digitalWrite'da LOW yerine 0 kullanıldı
    digitalWrite(BUZZER_PIN, 0); // Buzzer başlangıçta kapalı
}

void loop() {
    // Bluetooth'dan veri geldiyse
    if (SerialBT.available()) {
        String cmd = SerialBT.readStringUntil('\n');
        cmd.trim();
        Serial.println("Gelen: " + cmd);

        // Gelen komutu kontrol et
        if (cmd == "RED") {
            isEmergency = false; // Acil durumdan çık
            // Hata düzeltmesi: BUZZER_PIN için digitalWrite'da LOW yerine 0 kullanıldı
            digitalWrite(BUZZER_PIN, 0); // Buzzer'ı kapat
            // Rengi ayarla (Common Anode: 0=Açık, 255=Kapalı)
            analogWrite(RED_PIN, 0);
            analogWrite(GREEN_PIN, 255);
            analogWrite(BLUE_PIN, 255);
            color = "red";
        } else if (cmd == "GREEN") {
            isEmergency = false; // Acil durumdan çık
            // Hata düzeltmesi: BUZZER_PIN için digitalWrite'da LOW yerine 0 kullanıldı
            digitalWrite(BUZZER_PIN, 0); // Buzzer'ı kapat
            analogWrite(RED_PIN, 255);
            analogWrite(GREEN_PIN, 0);
            analogWrite(BLUE_PIN, 255);
            color = "green";
        } else if (cmd == "BLUE") {
            isEmergency = false; // Acil durumdan çık
            // Hata düzeltmesi: BUZZER_PIN için digitalWrite'da LOW yerine 0 kullanıldı
            digitalWrite(BUZZER_PIN, 0); // Buzzer'ı kapat
            analogWrite(RED_PIN, 255);
            analogWrite(GREEN_PIN, 255);
            analogWrite(BLUE_PIN, 0);
            color = "blue";
        } else if (cmd == "OFF") {
            isEmergency = false; // Acil durumdan çık
            // Hata düzeltmesi: BUZZER_PIN için digitalWrite'da LOW yerine 0 kullanıldı
            digitalWrite(BUZZER_PIN, 0); // Buzzer'ı kapat
            analogWrite(RED_PIN, 255);
            analogWrite(GREEN_PIN, 255);
            analogWrite(BLUE_PIN, 255);
            color = "off"; // Opsiyonel: Kapalı durumunu takip et
        } else if (cmd.startsWith("BRIGHT:")) {
            // Parlaklık komutu geldiğinde acil durumda DEĞİLSE ve bir renk seçiliyse uygula
            if (!isEmergency && (color == "red" || color == "green" || color == "blue")) {
                int brightness = cmd.substring(7).toInt();
                brightness = constrain(brightness, 0, 255);

                // Common Anode için parlaklık ayarı: 0=En Parlak, 255=Kapalı
                // Gelen parlaklık (0=Kapalı, 255=Parlak) ise -> analogWrite = 255 - brightness
                if (color == "red") {
                    analogWrite(RED_PIN, 255 - brightness);
                } else if (color == "green") {
                    analogWrite(GREEN_PIN, 255 - brightness);
                } else if (color == "blue") {
                    analogWrite(BLUE_PIN, 255 - brightness);
                }
            }
        } else if (cmd == "EMERGENCY_ON") {
            Serial.println("!!! EMERGENCY ON !!!");
            isEmergency = true; // Acil durum modunu aktifleştir
            // Acil durum paterninin ilk durumunu (açık) hemen ayarla
            lastEmergencyToggle = millis(); // Zamanlayıcıyı sıfırla
            emergencyLedState = true; // ON durumu ile başla

            // LED'i kırmızı yap ve buzzer'ı aç
            analogWrite(RED_PIN, 0);    // Kırmızı AÇIK
            analogWrite(GREEN_PIN, 255); // Yeşil KAPALI
            analogWrite(BLUE_PIN, 255);  // Mavi KAPALI
            // Hata düzeltmesi: BUZZER_PIN için digitalWrite'da HIGH yerine 1 kullanıldı
            digitalWrite(BUZZER_PIN, 1); // Buzzer AÇIK

            // Opsiyonel: Acil duruma girince mevcut renk durumunu temizle?
            // color = "emergency";
        } else if (cmd == "EMERGENCY_OFF") {
            Serial.println("--- EMERGENCY OFF ---");
            isEmergency = false; // Acil durum modunu kapat
            // Acil durumdan çıkınca her şeyi kapat
            analogWrite(RED_PIN, 255); // Kırmızı KAPALI
            analogWrite(GREEN_PIN, 255); // Yeşil KAPALI
            analogWrite(BLUE_PIN, 255); // Mavi KAPALI
            // Hata düzeltmesi: BUZZER_PIN için digitalWrite'da LOW yerine 0 kullanıldı
            digitalWrite(BUZZER_PIN, 0); // Buzzer KAPALI
            // Opsiyonel: Acil durumdan çıkınca renk durumunu temizle?
            // color = "";
        }
    }

    // --- Acil Durum Deseni Mantığı ---
    // Bu kısım isEmergency true olduğu sürece sürekli çalışır
    if (isEmergency) {
        unsigned long currentTime = millis();

        // Yanıp sönme/çalma aralığı geçti mi kontrol et
        if (currentTime - lastEmergencyToggle >= emergencyInterval) {
            lastEmergencyToggle = currentTime; // Zamanlayıcıyı sıfırla

            // Durumu tersine çevir (ON ise OFF, OFF ise ON yap)
            emergencyLedState = !emergencyLedState;

            if (emergencyLedState) {
                // LED'i kırmızı yap ve buzzer'ı aç
                analogWrite(RED_PIN, 0);    // Kırmızı AÇIK
                analogWrite(GREEN_PIN, 255); // Yeşil KAPALI
                analogWrite(BLUE_PIN, 255);  // Mavi KAPALI
                // Hata düzeltmesi: BUZZER_PIN için digitalWrite'da HIGH yerine 1 kullanıldı
                digitalWrite(BUZZER_PIN, 1); // Buzzer AÇIK
                Serial.println("Emergency: ON"); // Debug
            } else {
                // LED'i ve buzzer'ı kapat
                analogWrite(RED_PIN, 255); // Kırmızı KAPALI
                analogWrite(GREEN_PIN, 255); // Yeşil KAPALI
                analogWrite(BLUE_PIN, 255); // Mavi KAPALI
                // Hata düzeltmesi: BUZZER_PIN için digitalWrite'da LOW yerine 0 kullanıldı
                digitalWrite(BUZZER_PIN, 0); // Buzzer KAPALI
                Serial.println("Emergency: OFF"); // Debug
            }
        }
    }
    // --- Acil Durum Deseni Mantığı Sonu ---

    // Küçük bir bekleme (gerekli değil ama bazen faydalı olabilir)
    // delay(1);
}