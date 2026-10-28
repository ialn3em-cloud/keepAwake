1) انشئ مستودعا جديدا (Repository) على github.com
2) ارفع كل محتويات هذا المجلد كما هي (بما فيها مجلد .github)
3) أضف مفتاح التوقيع مرة واحدة فقط (الخطوة التالية)
4) افتح تبويب Actions وانتظر انتهاء Build APK (3-5 دقائق)
5) من قسم Artifacts نزّل:
   ScreenSwitch       ← التطبيق (app-release.apk)، وبرو فيه يُفتح بالشراء من Google Play
   ScreenSwitch-Test  ← نسخة تجربة لجوالك فقط، برو فيها مفتوح (لا ترفعها للمتجر)
   GooglePlay-AAB     ← الملف الذي ترفعه على Google Play (app-release.aab)

مفتاح التوقيع (مرة واحدة فقط):
كل نسخة يجب أن تُوقَّع بنفس المفتاح، وإلا يرفض الجوال تثبيت التحديث فوق النسخة القديمة.
في المستودع: Settings ← Secrets and variables ← Actions ← New repository secret
وأضف هذه الأربعة:
  KEYSTORE_BASE64    محتوى ملف KEYSTORE_BASE64.txt
  KEYSTORE_PASSWORD  كلمة مرور ملف المفتاح
  KEY_ALIAS          keepawake
  KEY_PASSWORD       كلمة مرور المفتاح
احتفظ بنسخة من ملف keepawake-release.jks وكلمة المرور في مكان آمن.
لا ترفعه داخل المستودع أبدا. إذا ضاع، لن تستطيع تحديث التطبيق عند الناس بدون حذفه.

ملفات Google Play (صور المتجر والنصوص وسياسة الخصوصية): مجلد store
