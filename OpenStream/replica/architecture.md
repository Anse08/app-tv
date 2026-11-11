# البنية

- اللغة: Kotlin، الواجهة: Jetpack Compose (Material 3)
- المشغّل: AndroidX Media3 (ExoPlayer + PlayerView)
- الشبكة: OkHttp + org.json (مدمج)
- الصور: Coil
- التخزين: SharedPreferences (إشارات مرجعية + مواضع المشاهدة)
- التنزيل: DownloadManager
- الحد الأدنى: API 24، الهدف: API 35

## الطبقات
```
ui/          Compose screens + theme + navigation
AppViewModel حالة البحث والعنصر المحدد والمصدر قيد التشغيل
provider/    MediaProvider (واجهة)  <- ArchiveProvider (Internet Archive)
data/        CatalogItem, Playable, LibraryStore
```

## نظام المزوّدين
`MediaProvider` هو بديل "الإضافات": لإضافة مصدر جديد قانوني، اكتب صنفًا ينفّذ الواجهة وسجّله في `AppViewModel.providers`.

## ترتيب البناء التالي
1. افتح المشروع في Android Studio وانتظر مزامنة Gradle
2. شغّل على محاكي أو جهاز
3. أضف: دعم Android TV ثم Chromecast ثم شاشة الإعدادات
