# DriveFlow - Shared Contract & Architecture Reference

## PROJECT FACTS
• Product: DriveFlow. TWO programs that are tightly linked through ONE backend: (a) the Android app "DriveFlow Driver", (b) the web program "DriveFlow Admin". Both talk only to the backend REST API defined below. Nothing in the app may work around the backend contract, and nothing in the admin may use a field the backend does not define.
• Purpose: the Android app reads the screen of three driver apps (inDrive, Uber Driver, DiDi Driver) and shows a colored verdict beside each request and in a floating bar. It NEVER taps, accepts, rejects or negotiates. It only reads and shows.
• Distribution: NOT on Google Play. Signed APK sent to a closed group of drivers. Subscriptions are sold by hand: the driver pays the owner directly, the owner creates a license key in the Admin.
• Android: Kotlin, Jetpack Compose Material3, applicationId com.driveflow.driver, minSdk 32 (Android 12L, same as the reference app), targetSdk 36, compileSdk 36, MVVM, coroutines/Flow, DataStore, kotlinx.serialization, OkHttp. No DI framework (manual AppGraph object). Libraries: kotlinx-coroutines, AndroidX core/lifecycle/activity, ML Kit on-device text recognition (bundled Latin model, com.google.mlkit:text-recognition), tesseract4android (native libtesseract + leptonica) with ara and eng traineddata bundled in assets, OkHttp, kotlinx.serialization, DataStore. NOT used because the app is sideloaded: Play Billing, Play Core, Firebase, play-services-location.
• Languages: Arabic (Egyptian dialect, RTL) is the default, English is second. All UI text lives in string resources.
• Brand colors: background navy #07131B, surface #0E2230, gold #F6C945, mint #7CF5C4, text #FFFFFF, muted text #9FB3C2. Verdict colors: GREEN #22C55E, ORANGE #F59E0B, RED #EF4444, GRAY #64748B.
• Supported ride apps and packages: inDrive = sinet.startup.inDriver, Uber Driver = com.ubercab.driver, DiDi Driver = com.didiglobal.driver. Internal codes: indrive, uber, didi. Reference versions and device (from the owner's phone): inDrive 5.189.0, DiDi Captain 7.10.58, Uber Driver 4.601.10000 (not yet in use by the owner, so no Uber screenshots exist); reference device Samsung SM-A175F, screen 1080x2340, Arabic UI, apps installed from Google Play.
• Backend: Node.js 20, TypeScript, Express, SQLite (better-sqlite3), one Dockerfile. REST for the app under /v1, REST for the admin under /admin/api, and the Admin web build is served by the same server under /admin.
• License key format: DF-XXXX-XXXX-XXXX using the characters A-Z (without I and O) and 2-9.
• License statuses used everywhere: TRIAL, ACTIVE, FREE, REVOKED, UNKNOWN.
• Package names: com.driveflow.driver with sub-packages core, data, reader, ocr, engine, overlay, license, ui.

## SHARED KOTLIN MODELS
```kotlin
enum class RideApp(val pkg: String, val code: String) { INDRIVE("sinet.startup.inDriver", "indrive"), UBER("com.ubercab.driver", "uber"), DIDI("com.didiglobal.driver", "didi") }
enum class ReadSource { ACCESSIBILITY, OCR }
enum class RoadState { UNKNOWN, EASY, SLOW, HARD }
enum class Tone { GREEN, ORANGE, RED, GRAY }
enum class StopsMode { IGNORE, PREFER, WARN, AVOID }
enum class LongTripMode { IGNORE, WARN, AVOID }
enum class DistanceUnit { KM, MILES, BOTH }
enum class LicenseStatus { TRIAL, ACTIVE, FREE, REVOKED, UNKNOWN }

data class ParsedRequest(
    val app: RideApp, val key: String, val source: ReadSource,
    val fare: Double?, val currency: String?,
    val pickupKm: Double?, val tripKm: Double?, val totalKm: Double?, val pickupMinutes: Int?,
    val riderRating: Double?, val riderTrips: Int?, val requestAgeSeconds: Int?,
    val pickupAddress: String?, val destinationAddress: String?, val extraStops: Int = 0,
    val paymentMethod: String?, val riderNote: String?, val offerCount: Int?,
    val roadState: RoadState, val productName: String?,
    val bounds: android.graphics.Rect, val isDetailOpen: Boolean = false, val confidence: Float = 1.0f
)

data class Policy(
    val minPricePerKm: Double = 0.0,
    val minTotalKm: Double? = null, val maxTotalKm: Double? = null, val maxTripKm: Double? = null, val maxPickupKm: Double? = null, val maxPickupMinutes: Int? = null,
    val minRiderRating: Double? = null, val minRiderTrips: Int? = null, val maxRequestAgeMinutes: Int? = null,
    val negotiationStep: Int = 5, val maxOffer: Double? = null,
    val currency: String = "EGP", val governorate: String? = null,
    val avoidedAreas: List<String> = emptyList(), val preferredAreas: List<String> = emptyList(),
    val allowedPayments: Set<String> = emptySet(), val bannedNoteWords: List<String> = emptyList(),
    val stopsMode: StopsMode = StopsMode.IGNORE, val stopFee: Double? = null,
    val longTripMode: LongTripMode = LongTripMode.IGNORE, val longTripKm: Double? = null,
    val warnOnUberSaver: Boolean = false, val freePickupKm: Double? = null,
    val fuelCostPerKm: Double? = null, val upkeepCostPerKm: Double? = null,
    val commissionPct: Map<RideApp, Double> = emptyMap(), val commissionEnabled: Map<RideApp, Boolean> = emptyMap(), val appEnabled: Map<RideApp, Boolean> = mapOf(RideApp.INDRIVE to true, RideApp.UBER to true, RideApp.DIDI to true),
    val distanceUnit: DistanceUnit = DistanceUnit.KM, val basisNet: Boolean = false
)

data class Verdict(
    val tone: Tone, val reasonCode: String,
    val titleAr: String, val titleEn: String, val detailAr: String, val detailEn: String,
    val flags: List<String>, val totalKm: Double?, val grossPerKm: Double?, val netPerKm: Double?,
    val netProfit: Double?, val suggestedPrice: Double?
)
```

## DECISION ENGINE CONTRACT & REASON CODES

### Mathematical Definitions
• totalKm = req.totalKm ?: (pickupKm + tripKm) when both exist
• billableKm = totalKm minus min(pickupKm, policy.freePickupKm ?: 0)
• grossPerKm = fare / billableKm (if billableKm <= 0 then null)
• carCost = (policy.fuelCostPerKm + policy.upkeepCostPerKm) * totalKm
• commission = fare * pct/100 when commissionEnabled[app] is true, else 0
• stopCost = extraStops * (policy.stopFee ?: 0)
• netProfit = fare - commission - carCost - stopCost (null if fare or totalKm missing)
• netPerKm = netProfit / totalKm
• Measured value: netPerKm when policy.basisNet is true, else grossPerKm
• suggestedPrice: smallest price reaching policy.minPricePerKm (inverted formula), rounded UP to multiple of policy.negotiationStep, capped by policy.maxOffer; null if already satisfied or impossible

### Reason Codes Table
| reasonCode | Tone | Title (Ar) | Title (En) | Detail (Ar) | Detail (En) |
|---|---|---|---|---|---|
| APP_DISABLED | GRAY | التطبيق معطل | App Disabled | تم تعطيل متابعة هذا التطبيق في الإعدادات | Monitoring for this app is disabled in settings |
| DATA_MISSING | GRAY | بيانات ناقصة | Data Missing | افتح الطلب لعرض التفاصيل / مسافة المشوار | Open request to view details / trip distance |
| PICKUP_TOO_FAR | RED | بيك أب بعيد | Pickup Too Far | مسافة الوصول للزبون تتجاوز الحد الأقصى | Pickup distance exceeds maximum limit |
| PICKUP_TOO_LONG | RED | وقت وصول طويل | Pickup Too Long | وقت الوصول للزبون يتجاوز الحد الأقصى | Pickup duration exceeds maximum limit |
| TRIP_TOO_LONG | RED | مشوار طويل جداً | Trip Too Long | مسافة المشوار تتجاوز الحد الأقصى | Trip distance exceeds maximum limit |
| TOTAL_TOO_SHORT | RED | مشوار قصير جداً | Trip Too Short | إجمالي المسافة أقل من الحد الأدنى | Total distance is below minimum limit |
| TOTAL_TOO_LONG | RED | إجمالي مسافة كبير | Total Distance Too High | إجمالي المسافة يتجاوز الحد الأقصى | Total distance exceeds maximum limit |
| RATING_LOW | RED | تقييم منخفض | Low Rating | تقييم الراكب أقل من الحد الأدنى | Rider rating is below minimum threshold |
| TRIPS_FEW | RED | رحلات قليلة | Few Trips | عدد رحلات الراكب السابقة غير كافٍ | Rider completed trips is below minimum |
| REQUEST_OLD | RED | طلب قديم | Request Expired | الطلب مر عليه وقت طويل منذ إرساله | Request is too old |
| PAYMENT_NOT_ALLOWED | RED | طريقة دفع مرفوضة | Payment Not Allowed | طريقة الدفع غير مسموح بها في إعداداتك | Payment method is not allowed by your rules |
| NOTE_BANNED_WORD | RED | ملاحظة محظورة | Banned Note Word | ملاحظة الراكب تحتوي على كلمات محظورة | Rider note contains a banned keyword |
| AREA_AVOIDED | RED | منطقة محظورة | Avoided Area | نقطة الانطلاق أو الوجهة تقع في منطقة محظورة | Pickup or destination is in an avoided area |
| STOPS_AVOID | RED | وقفات مرفوضة | Extra Stops Avoided | الطلب يحتوي على وقفات إضافية وقاعدتك ترفضها | Extra stops are avoided by your policy |
| LONG_TRIP_AVOID | RED | سفر مرفوض | Long Trip Avoided | مسافة المشوار تعتبر سفراً وقاعدتك ترفض السفر | Trip distance qualifies as long trip and policy avoids it |
| PRICE_LOW | RED | السعر ضعيف | Price Too Low | السعر لكل كم أقل من الحد الأدنى المطلوب | Rate per km is below minimum required |
| STOPS_WARN | ORANGE | وقفات إضافية | Extra Stops | الطلب يحتوي على وقفات إضافية تحتاج تأكيد | Request contains extra stops that need review |
| LONG_TRIP_WARN | ORANGE | مشوار طويل | Long Trip | مشوار طويل يتجاوز الحد المحدد | Long trip exceeding your threshold |
| UBER_SAVER | ORANGE | أوبر سيفر | Uber Saver | طلب أوبر سيفر بتسعيرة منخفضة | Uber Saver request with discounted fare |
| PRICE_BORDERLINE | ORANGE | سعر قريب من الحد | Borderline Price | السعر أقل من الحد بنسبة بسيطة (ضمن 5%) | Price is slightly below limit (within 5%) |
| LOW_CONFIDENCE | ORANGE | قراءة غير مؤكدة | Low Confidence | دقة قراءة الشاشة منخفضة، راجع الأرقام بنفسك | Reading confidence is low, verify manually |
| AREA_UNKNOWN | ORANGE | المحافظة غير محددة | Governorate Not Set | المحافظة غير محددة مع وجود مناطق مخصصة | Governorate is not set while custom areas are configured |
| ACCEPTABLE | GREEN | طلب ممتاز | Great Request | الطلب يطابق شروطك وسعره مناسب | Request matches your rules with acceptable rate |

### Flags
• PREFERRED_AREA
• STOPS_PREFERRED
• HOME_DIRECTION
• FREE_PICKUP_USED
• NET_BASIS
• OPEN_FOR_ROUTE
• FREE_PLAN
