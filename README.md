# Ultra Voice Recorder · اولترا ویس ریکوردر

[فارسی](#فارسی) · [English](#english)

---

## English

A native Android voice recorder (Kotlin, Jetpack Compose, Room, DataStore, Foreground Service).
The UI follows a dark, minimal style: one big record button, a live waveform with a large timer while recording.

### Download
Every push to `main` builds the app on GitHub Actions and updates a rolling pre-release:

`https://github.com/taheryanmohamadtaha-lab/Call-recorder/releases/download/latest/app-debug.apk`

(The build is also uploaded as the `app-debug` workflow artifact.)

### Install on your phone
1. Download `app-debug.apk` on the phone.
2. Open it. If Android asks, allow "Install unknown apps" for your browser/file manager.
3. Open **Ultra Voice Recorder** and allow the microphone (and notifications) when asked.
4. The debug build installs as a separate app (`com.ultravoice.recorder.debug`).

### Features

| Status | Feature |
|---|---|
| ✅ Done | Microphone recording to 16-bit PCM WAV, foreground service with always-visible notification (pause/resume/stop), works with the screen off |
| ✅ Done | Pause / resume, live timer, live file size, live level bar and scrolling waveform |
| ✅ Done | Quality presets (Standard 16 kHz, High 44.1 kHz, Studio 48 kHz stereo, Lossless 48 kHz) + custom sample rate (16 / 22.05 / 44.1 / 48 kHz) and mono/stereo. If the device refuses stereo, it falls back to mono and the UI shows what was really recorded |
| ✅ Done | WAV header refreshed every second, so a crash/kill still leaves a playable file; unreferenced files are recovered on next start |
| ✅ Done | Max recording length, automatic stop + save on low storage |
| ✅ Done | Archive: search (name + note), sort (date/size/duration), favorites, trash with restore, delete forever, empty trash, multi-select, day headers |
| ✅ Done | Per-file: rename, note, favorite, share (FileProvider), move to trash |
| ✅ Done | Player: play/pause, ±5/10/30 s, speed 0.5×–2×, volume, waveform with tap/drag to seek, elapsed/remaining time |
| ✅ Done | Non-destructive enhancement (new file; original kept): 100 Hz high-pass (wind/rumble), gentle noise gate, peak normalisation. Classic DSP, **not** AI |
| ✅ Done | Dark / light / system theme, English + Persian UI with RTL |
| ✅ Done | Everything stays on the device; nothing is uploaded; the app never records without the notification |
| ⚠️ Limited | **Phone-call recording:** see below |
| ❌ Not implemented | AAC/M4A/OGG/FLAC output (WAV only), app PIN/biometric lock, file encryption, backup/export of the whole library, custom folders, bookmarks, background playback service, AI noise suppression, silence removal, bundled Persian font (system font is used) |

### Phone-call recording – what is and is not possible
* Since **Android 10**, normal apps cannot capture the other person's voice or the call audio stream through official APIs. Most manufacturers also block microphone capture during a call (you may get silence).
* This app **does not** use Accessibility tricks, root, or any other bypass, and it never records secretly.
* What it does offer:
  1. **Call time log** – start/end times only (no number, no name, no audio), needs the *Phone* permission, which you grant on the Calls screen.
  2. **Speaker-mode recording** – put the call on speaker and record with the microphone. You must tick a consent box confirming everyone knows and agrees. Quality depends on the phone, and recording calls may be regulated by local law.
* The Calls screen shows your device and Android version and states this limitation.

### Build
```bash
./gradlew testDebugUnitTest assembleDebug     # APK: app/build/outputs/apk/debug/app-debug.apk
```
Requires JDK 17 and the Android SDK (API 34). GitHub Actions does this automatically (`.github/workflows/build.yml`).

### Signed release APK
Publish a GitHub Release; the workflow builds `assembleRelease` and attaches the APK. For signing add these repository secrets (never commit a keystore):
`KEYSTORE_BASE64` (base64 of your `.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
Without them the release APK is unsigned.

### Project layout
```
app/src/main/java/com/ultravoice/recorder/
  audio/     WavRecorder, WavHeader, Enhancer, WaveformExtractor, PlayerController, RecorderState
  data/      Room entities/DAOs, DataStore settings, RecordingRepository
  services/  RecordingService (foreground), CallStateReceiver (call times only)
  ui/        Compose screens, theme, icons, waveforms, ViewModel
.github/workflows/build.yml
```

### Tests
Unit tests: WAV header build/parse, formatting helpers, enhancer (normalisation, original untouched).
Real-device checks (recording, screen-off, interruptions) must be done on a phone; see the checklist in `docs/TEST_REPORT.md`.

---

## فارسی

اپلیکیشن بومی اندروید برای ضبط صدا (Kotlin، Jetpack Compose، Room، DataStore، سرویس پیش‌زمینه) با ظاهری تیره و مینیمال: یک دکمه‌ی بزرگ ضبط، و هنگام ضبط، موج صوتی زنده و تایمر بزرگ.

### دانلود
با هر Push روی شاخه‌ی `main`، برنامه توسط GitHub Actions ساخته می‌شود و این لینک به‌روز می‌شود:

`https://github.com/taheryanmohamadtaha-lab/Call-recorder/releases/download/latest/app-debug.apk`

### نصب روی گوشی
۱. فایل `app-debug.apk` را روی گوشی دانلود کنید.
۲. آن را باز کنید و در صورت پرسش، «نصب از منابع ناشناس» را برای مرورگر/فایل‌منیجر مجاز کنید.
۳. برنامه را باز کنید و دسترسی میکروفون و اعلان را بدهید.
۴. نسخه‌ی دیباگ با شناسه‌ی `com.ultravoice.recorder.debug` نصب می‌شود.

### راهنمای استفاده
* **ضبط:** دکمه‌ی بزرگ میکروفون را لمس کنید. هنگام ضبط می‌توانید «توقف موقت/ادامه»، «ذخیره» یا «حذف» را بزنید. اعلان ضبط همیشه دیده می‌شود و از آن هم می‌توان ضبط را متوقف کرد.
* **آرشیو:** جستجو، مرتب‌سازی (تاریخ/حجم/مدت)، علاقه‌مندی‌ها، سطل زباله. با لمس طولانی چند فایل را انتخاب کنید.
* **پخش‌کننده:** پرش ۵/۱۰/۳۰ ثانیه، سرعت ۰٫۵ تا ۲ برابر، لمس یا کشیدن روی موج برای رفتن به نقطه‌ی دلخواه. از همین صفحه می‌توانید نام، یادداشت، علاقه‌مندی، اشتراک‌گذاری، حذف و «بهبود کیفیت صدا» را انجام دهید.
* **تنظیمات:** پروفایل کیفیت، نرخ نمونه‌برداری، مونو/استریو، حداکثر مدت ضبط، تم.

### امکانات
| وضعیت | امکان |
|---|---|
| ✅ کامل | ضبط میکروفون در WAV ‏۱۶ بیت، سرویس پیش‌زمینه با اعلان دائمی، کار با صفحه‌ی خاموش |
| ✅ کامل | توقف موقت/ادامه، تایمر و حجم زنده، نوار شدت صدا و موج زنده |
| ✅ کامل | پروفایل‌های کیفیت و انتخاب نرخ نمونه‌برداری و مونو/استریو؛ اگر گوشی استریو را نپذیرد مونو ضبط می‌شود و همان نمایش داده می‌شود |
| ✅ کامل | سرآیند WAV هر ثانیه به‌روز می‌شود تا در صورت بسته شدن ناگهانی، فایل قابل پخش بماند و در اجرای بعدی بازیابی شود |
| ✅ کامل | حداکثر مدت ضبط و توقف خودکار همراه با ذخیره هنگام کمبود فضا |
| ✅ کامل | آرشیو، سطل زباله، بازیابی، علاقه‌مندی، یادداشت، تغییر نام، اشتراک‌گذاری، انتخاب چندتایی |
| ✅ کامل | پخش‌کننده با سرعت، پرش، موج قابل لمس |
| ✅ کامل | بهبود صدا به‌صورت غیرمخرب (فایل جدید): فیلتر باد/کم‌فرکانس، گیت نویز ملایم، نرمال‌سازی. پردازش سیگنال کلاسیک است، نه هوش مصنوعی |
| ✅ کامل | تم تیره/روشن/سیستم، فارسی و انگلیسی با RTL |
| ⚠️ محدود | ضبط مکالمه‌ی تلفنی (توضیح پایین) |
| ❌ پیاده‌سازی نشده | خروجی AAC/M4A/OGG/FLAC، قفل PIN/بیومتریک، رمزگذاری، پشتیبان‌گیری، پوشه‌های اختصاصی، بوکمارک، پخش پس‌زمینه، حذف نویز با هوش مصنوعی، حذف سکوت، فونت فارسی اختصاصی |

### ضبط مکالمه‌ی تلفنی؛ چه چیزی ممکن است و چه چیزی نه
* از **اندروید ۱۰** به بعد، برنامه‌های عادی از طریق APIهای رسمی نمی‌توانند صدای طرف مقابل یا جریان صوتی تماس را ضبط کنند. بسیاری از سازندگان هم میکروفون را هنگام تماس مسدود می‌کنند (ممکن است سکوت ضبط شود).
* این برنامه از روش‌های دورزدن (Accessibility، روت و مانند آن) استفاده **نمی‌کند** و هرگز مخفیانه ضبط نمی‌کند.
* امکانات موجود: ۱) **گزارش زمان تماس‌ها** (فقط زمان شروع/پایان؛ بدون شماره، نام یا صدا) که به مجوز «تلفن» نیاز دارد. ۲) **ضبط در حالت بلندگو** با میکروفون، فقط پس از تأیید اینکه همه‌ی طرف‌ها آگاه و راضی‌اند. کیفیت به گوشی بستگی دارد و قوانین محل شما ممکن است ضبط مکالمه را محدود کنند.

### ساخت
```bash
./gradlew testDebugUnitTest assembleDebug
```
JDK 17 و Android SDK نسخه‌ی ۳۴ لازم است؛ GitHub Actions همه‌ی این مراحل را خودکار انجام می‌دهد.

### APK امضاشده (Release)
یک Release در گیت‌هاب منتشر کنید؛ Workflow فایل Release را می‌سازد و پیوست می‌کند. برای امضا، این Secrets را در تنظیمات مخزن بسازید (کلید را هرگز در مخزن نگذارید): `KEYSTORE_BASE64`، `KEYSTORE_PASSWORD`، `KEY_ALIAS`، `KEY_PASSWORD`.

سازنده: محمدطاها طاهریان

## License
MIT – see `LICENSE`.
