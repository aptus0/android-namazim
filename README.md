# Android Namazım

Namaz vakitleri, günlük hadis, Ramazan takibi, premium Material 3 arayüzü ve kıble özellikleri içeren Android uygulaması.

## Özellikler

- Konuma göre otomatik şehir/ilçe algılama (Türkiye odaklı)
- Günlük namaz vakitleri ve bir sonraki vakte canlı geri sayım
- Ramazan/takvim görünümü
- Günün hadisi + favoriler
- Bildirim ve alarm ayarları
- Kıble pusulası ve kıble haritası (Google Maps)

## Teknoloji

- Java (Android)
- Material 3
- Navigation Component
- Room
- WorkManager
- Retrofit + OkHttp
- Google Play Services (Location, Maps)

## Gereksinimler

- Android Studio (Hedgehog+ önerilir)
- JDK 11
- `google_maps_key` için geçerli Maps API anahtarı

`app/src/main/res/values/strings.xml` içindeki `google_maps_key` değerini güncelleyin:

```xml
<string name="google_maps_key">YOUR_GOOGLE_MAPS_API_KEY</string>
```

## Build

Debug APK:

```bash
./gradlew :app:assembleDebug
```

Çıktı:

`app/build/outputs/apk/debug/app-debug.apk`

## Sürümleme

- Tag formatı: `vMAJOR.MINOR.PATCH`
- İlk yayın: `v1.0.0`

Detaylar için: [CHANGELOG.md](CHANGELOG.md)
