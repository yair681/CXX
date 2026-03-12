# WhatsApp Watch 🟢⌚

אפליקציית Wear OS שמציגה QR אמיתי של WhatsApp Web לחיבור השעון לחשבון שלך.

## ארכיטקטורה
```
[Render Backend - Baileys] ←WebSocket→ [Galaxy Watch 6]
         ↑
   [WhatsApp בטלפון סורק QR]
```

---

## שלב 1 — Deploy Backend ל-Render

1. עלה לגיטהאב את הפרויקט
2. כנס ל [render.com](https://render.com) → New Web Service
3. חבר את הריפו
4. הגדרות:
   - **Root Directory:** `backend`
   - **Build Command:** `npm install`
   - **Start Command:** `node index.js`
5. לחץ Deploy
6. שמור את הכתובת: `https://YOUR-APP.onrender.com`

---

## שלב 2 — עדכן כתובת ב-app

פתח:
```
wear-app/app/src/main/java/com/whatsappwatch/MainActivity.kt
```

שנה שורה:
```kotlin
private val BACKEND_URL = "wss://YOUR-APP-NAME.onrender.com"
```

עדכן ל-URL שקיבלת מ-Render.

---

## שלב 3 — בנה APK דרך GitHub Actions

1. Push לגיטהאב (main branch)
2. כנס ל: `Actions` → `Build Wear OS APK`
3. לחץ על ה-run → גלול למטה ל-**Artifacts**
4. הורד `whatsapp-watch-apk.zip`
5. חלץ → תקבל `app-release-unsigned.apk`

---

## שלב 4 — התקן לשעון דרך ADB

### חבר שעון דרך WiFi:
1. שעון: הגדרות → מפתח → אפשר ADB debug
2. מצא IP של השעון בהגדרות WiFi
3. בCMD:
```cmd
adb connect 192.168.X.X:5555
adb devices
```

### התקן APK:
```cmd
adb install app-release-unsigned.apk
```

### הפעל:
```cmd
adb shell am start -n com.whatsappwatch/.MainActivity
```

---

## שימוש

1. פתח את האפליקציה בשעון
2. המתן לQR להופיע (~10 שניות)
3. פתח WhatsApp בטלפון → מכשירים מקושרים → קשר מכשיר
4. סרוק את הQR בשעון
5. ✅ השעון מחובר לחשבון WhatsApp שלך!
