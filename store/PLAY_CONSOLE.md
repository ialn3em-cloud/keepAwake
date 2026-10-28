# إجابات Play Console

## منتج الترقية (مهم، بدونه زر الشراء ما يشتغل)
**Monetize with Play ← Products ← One-time products ← Create product**

| الخانة | القيمة |
|---|---|
| Product ID | `pro_unlock` (بالضبط كذا، التطبيق يبحث عن هذا الاسم) |
| Name | مفتاح الشاشة برو |
| Description | الشاشة تبقى مضاءة تلقائيًا في التطبيقات اللي تختارها |
| Price | 1.99 USD، أو اكتب السعر بالريال وخل Google تحسب باقي الدول |

بعدها اضغط **Activate**. يحتاج حساب تاجر (Payments profile) مرتبط بحساب المطوّر، وPlay Console يطلبه منك أول مرة.

**عشان تجرب الشراء مجانًا:** Settings ← License testing ← أضف إيميلك وإيميلات المختبرين. يشترون بـ«بطاقة اختبار» ولا ينخصم منهم شي.

---

## أقسام App content
انسخ هذه الإجابات في أقسام **Policy ← App content** في Play Console. أسئلة Google قد تتغير قليلًا مع الوقت، فاختر الأقرب.

## رابط سياسة الخصوصية (Privacy policy)
```
https://github.com/ialn3em-cloud/keepAwake/blob/main/store/PRIVACY.md
```
⚠️ قبل لصق الرابط: افتح `store/PRIVACY.md` على GitHub واكتب بريدك مكان **[بريدك الإلكتروني]** و **[your email]**.

## Ads
**No, my app does not contain ads.**

## App access
**All functionality is available without special access.** لا يوجد تسجيل دخول.

## Content rating
اختر الفئة **Utility, Productivity, Communication, or Other**، وأجب **No** على كل الأسئلة: لا عنف، ولا محتوى للبالغين، ولا تواصل بين المستخدمين، ولا مشاركة موقع.

## Target audience
**18 and over**، أو **13 and over**. لا تختر أي فئة أقل من 13 لأنها تضيف شروطًا إضافية.

## Data safety
- Does your app collect or share any of the required user data types? → **No**
- النتيجة في صفحة المتجر: «No data collected» و«No data shared».

## Government apps / Financial features / Health
**No** على كل الأسئلة.

---

## Foreground service permissions
Google تسأل عن كل نوع خدمة في المقدمة. التطبيق يستخدم **Special use** فقط.

**خدمة المفتاح:**
```
The app's only purpose is to keep the screen on while the user wants it. When the user turns the switch on (in the app, the Quick Settings tile or the widget), a foreground service holds a 1x1 invisible overlay with FLAG_KEEP_SCREEN_ON so the screen stays on over any app. It must keep running while the user is in other apps, and stops when the user turns the switch off or the battery drops to 10% without charging. No other foreground service type fits this use.
```

**ميزة برو (التطبيقات التلقائية)، أضفها تحت الأولى:**
```
Automatic apps: when the user enables it and picks apps, a second special-use foreground service checks which app is in front (Usage access) and keeps the screen on only while one of the chosen apps is open. It pauses while the screen is off. The user can turn it off at any time in the app.
```

إذا طلبوا **فيديو**: صوّر شاشة جوالك 20 إلى 30 ثانية (تسجيل الشاشة من الإشعارات) وأنت تشغّل المفتاح، وتفتح تطبيقًا، والشاشة لا تنطفئ، ثم تطفئ المفتاح. ارفعه على يوتيوب «غير مدرج» (Unlisted) وضع الرابط.

---

## نقطتان قد يسأل عنهما Google في ميزة برو

1. **Usage access (PACKAGE_USAGE_STATS):** مسموح ما دام سبب استخدامه واضحًا للمستخدم. وهو موضح في التطبيق والوصف.
2. **Ignore battery optimizations:** Google تسمح به فقط لاستخدامات محددة. إذا اعترضوا عليه، أقدر أحذف الطلب المباشر ويبقى زر يفتح إعدادات البطارية، والميزة تبقى تعمل.
