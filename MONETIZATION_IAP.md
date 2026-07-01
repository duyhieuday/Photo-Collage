# Premium / IAP — Tổng hợp việc Đã làm & Cần làm

> Màn Premium (`PremiumActivity` / `activity_premium.xml`) redesign theo Figma (node `454-2599`, file `x8z1JiveOXcqzJhxpubr55`) + hệ thống monetization P0. Mọi thay đổi GIỮ NGUYÊN logic billing gốc.

---

## ✅ ĐÃ LÀM

### 1. Màn Premium — redesign theo Figma
| Hạng mục | Chi tiết | File |
|---|---|---|
| Bố cục cuộn | `ConstraintLayout` chain → **`NestedScrollView`** (vuốt dọc, fillViewport) | `activity_premium.xml` |
| Nền gradient | Khớp đúng dải xanh đã bake trong ảnh carousel: `#0B0E17 → #2B3C1C → #0C0F07` (hết lộ "hộp"/seam) | `drawable/bg_iap_gradient.xml` |
| Title | 2 dòng **"Photo Collage / Premium" + emblem vàng** (V + vòng quỹ đạo) | `activity_premium.xml`, `drawable-nodpi/ic_premium_emblem.png` |
| Nút X | X **trắng** trong **vòng tròn đen 50%** — fix bug X bị đen/vô hình | `bg_iap_close_circle.xml` (⚠️ xem ghi chú kỹ thuật) |
| Ảnh carousel | Re-export Figma **scale 4 (1440×840) + nền trong suốt** (ghép group card + sparkle, alpha mềm chuẩn) → hết mờ + sparkle sạch, đồng bộ màu | `drawable-nodpi/iap_feature_1..3.png` |
| Footer | Fix typo "Term" → **"Terms & Conditions"** | `activity_premium.xml` |

### 2. Đa ngôn ngữ
- Tách **toàn bộ chuỗi** màn Premium + caption + upsell ra string resources (`iap_*`).
- Dịch sẵn **6 locale**: `values/` (en) + `values-en/vi/zh/hi/fr/pt/`.
- ⚠️ App đang để **`resConfigs "en"`** trong build.gradle → build chỉ giữ tiếng Anh (các locale khác bị strip). Khi bỏ/đổi resConfigs sẽ tự đồng bộ theo hệ thống language của app.

### 3. Entry point
- Icon **crown `ic_pro`** góc phải Home → mở `PremiumActivity` (mở thẳng, không kèm ad).
- Fix bug `btnMenu` bị gán 2 listener (trả về đúng `SettingActivity`).
- File: `activity_main.xml`, `MainActivity.kt`, `drawable-nodpi/ic_pro.png`.

### 4. P0 Monetization
| # | Hạng mục | Cách làm | Verify |
|---|---|---|---|
| 1 | **Onboarding / first-run paywall** | `SplashActivity.intent()`: **nếu remote BẬT onboarding** (`test_obd=="yes" && hehe`) + first-run → Language→ABOnBoarding→**Premium**→Home. **Nếu KHÔNG onboarding** → paywall first-run (extra `free_trial`, **dismissible**→Home) 1 lần (cờ `first_paywall_shown`), lần sau thẳng Home. Logic paywall gom vào helper `PremiumActivity.startFirstRunPaywallOrHome(Activity)` (dùng cả Splash lẫn ABOnBoarding). | ✅ Pixel (nhánh no-onboarding); nhánh onboarding build-verified (remote chưa có key test_obd) |
| 2 | **"Remove ads" lên #1** carousel | Reorder ảnh + caption (app ad-first → benefit mạnh nhất) | ✅ |
| 3 | **Restore Purchases** | Link footer → `queryPurchasesAsync` + feedback (bắt buộc theo policy Play) | ✅ Máy ảo |
| 4 | **per-week + SAVE%** gói Yearly | Tính từ `priceAmountMicros` ("Save 91% · only ₫4,557/week"), ẩn tới khi có giá | ✅ Pixel (giá thật) |
| 5 | ~~**Auto chọn trial offer**~~ → **ĐÃ BỎ trial, mua thẳng** | `pickOfferToken()` giờ chọn offer KHÔNG có phase giá=0 (mua thẳng, tính tiền ngay) | ✅ Pixel |
| 6 | **Watermark trên export free** | `WatermarkUtil.applyIfFree()` — vẽ "Photo Collage" góc dưới-phải cho user FREE; Premium/RemoveAd → bitmap gốc; lỗi → fallback gốc (không vỡ save). Inject ở **4 điểm export cuối**. | ✅ Máy ảo |
| 7 | **Upsell "Remove Watermark"** | Chip lime ở màn kết quả `ShowImageActivity` (cover collage + template + AI-remove) → tap mở Premium | ✅ Máy ảo |
| 8 | ~~**Free-trial framing**~~ → **ĐÃ TẮT (mua thẳng)** | `updateTrialFraming()` giờ LUÔN hiện "Subscribe Now" / "Cancel anytime", không bao giờ hiện "Start Free Trial" dù Play Console có offer trial. `trialDays()`/`periodToDays()` giữ lại (giá trị không dùng) để dễ bật lại sau. | ✅ Pixel |

---

## ⏳ CẦN LÀM

### P1 — đòn bẩy tiếp theo (theo nghiên cứu)
- **Feature-gate (lever #2)** — Soft-sell:
  - ✅ **AI Remove**: badge 👑 trên nút + tap (free) → dialog mời Premium ("Go Premium" / "Continue" vẫn cho dùng). Util chung `PremiumUpsell` (`ads/iap/PremiumUpsell.kt`). Verified máy ảo.
  - ✅ **Template premium** (Cách A): template số thứ tự ≥6/category = premium (~5 đầu/category free). Badge 👑 + gate đặt trong `TemplatePickerAdapter` (funnel chung → cover Home/picker/ShowImage/see-all). Verified máy ảo.
  - ✅ **Filter premium** (editor photo-edit-new): `FilterType.isPremium` (Auto/Enhance/BW = premium; Original/Grayscale/Invert free). Badge 👑 `ivCrown` + tap premium → dialog mời Premium. Cầu nối qua **abstract `PhotoEditorActivity.showPremiumUpsell()`** (app subclass `PhotoEditorWithBannerActivity` override = `PremiumUpsell.showFeatureDialog`) vì module editor KHÔNG import được app. `openPaywall()` cũng đã fill (mở `PremiumActivity`). Build PASS. ✅ **Verified Pixel 6a thật**: Edit→ảnh→Crop→Next→Filter → badge 👑 hiện đúng trên Auto/Enhance/BW (Original/Grayscale/Invert không), tap premium → dialog "Premium Feature" → "Continue" vẫn áp filter. (Emulator x86_64 KHÔNG test được path này: crash sẵn `UnsatisfiedLinkError: libdk_*.so` — `DockitDocumentDetector`/`PhotoEditorViewModel.kt:115`, lib chỉ build ARM; không liên quan gate.)
  - ✅ **Sticker premium** (Cách A): `StickerItem.isPremium` — mỗi category ~5 sticker đầu free, từ thứ 6 premium (`FREE_PER_CATEGORY=5` trong `StickerCatalog`). Badge `stickerPro` + gate trong `StickerGridAdapter` (dùng `PremiumUpsell` trực tiếp). Build PASS. ✅ **Verified máy ảo**: CollageActivity→Sticker → badge hiện đúng từ sticker thứ 6 (index≥5) → tap premium hiện dialog "Premium Feature" → "Continue" vẫn thêm sticker.
  - ⛔ **Effect**: KHÔNG có panel effect rời trong editor (chỉ slider adjust contrast/brightness/saturation/warmth = tool free). Không gate.
- **Premium ad-exemption** — ✅ verify trải nghiệm premium sạch (không badge/banner/ads). Fix 2 gap: `NativeFullScreen` (check premium bị comment) + `BannerAds` init (load banner cho premium). Các ad full-screen vốn đã exempt sẵn.
- ✅ **"Skip ads → pay"**: sau khi ĐÓNG interstitial chào "Remove ads forever?" → Premium. Hook `InterAds.onAdDismissedFullScreenContent` (nhánh non-native-after-inter), cứ mỗi `REMOVE_ADS_UPSELL_EVERY=2` lần đóng inter + user FREE. Dialog `PremiumUpsell.showRemoveAdsDialog()` (reuse `dialog_premium_upsell.xml`, đổi title `iap_remove_ads_title`; post-delayed 500ms tránh BadToken lúc window chuyển cảnh; callback đảm bảo chạy đúng 1 lần dù đóng kiểu gì → không kẹt luồng sau ad). ✅ **Verified Pixel thật**: đóng inter → dialog hiện → Continue → điều hướng tiếp bình thường.
- **Rewarded unlock**: "Xem quảng cáo để dùng AI Remove 1 lần" (soft-sell, giữ phần thưởng khan hiếm).
- Upsell watermark cho **single-photo editor** (hiện setResult→Home, không có result screen).

### P2 — tối ưu
- **Win-back offer** (giảm giá) cho user đóng paywall.
- **After-Nth-session** trigger (mở app lần ~3) cho user skip onboarding.
- **Cap interstitial** session-level (hiện đã có cooldown ~20–45s qua `flagQC`/`INTER_TIME`).
- **A/B test**: copy CTA, gói mặc định (tuần+trial vs năm), vị trí paywall.
- **Segment ad frequency** theo intent/geo.

---

## 📁 File chính & ghi chú kỹ thuật

**Màn Premium:** `PremiumActivity.java`, `activity_premium.xml`, `IapFeatureAdapter.java`, các `drawable/bg_iap_*`.

**Watermark:** `photo-editor-new/.../util/WatermarkUtil.kt` (đặt trong module để app + module đều gọi được). Inject ở 4 dispatcher export cuối: `FilterCollageActivity.saveToGallery`, `TemplateEditorActivity.saveToGallery`, `AfterRemoveActivity.saveToGallery`, `PhotoEditorViewModel.saveBitmapToGallery`. **KHÔNG** đụng cache/crop/sticker.

**Upsell:** `ShowImageActivity.kt` + `activity_show_image.xml` (chip `tvRemoveWatermark`). 3/4 editor route qua màn này.

**Paywall first-run:** `SplashActivity.intent()` (⚠️ routing Language/Onboarding gốc đã bị comment — app luôn vào thẳng Home; nên đặt paywall ở Splash, KHÔNG ở `ABOnBoardingActivity`).

⚠️ **Gotcha nút X đen:** `PremiumActivity extends FragmentActivity` (không phải AppCompat) → `app:tint` BỊ BỎ QUA; mà `ic_close_ab.xml` bake sẵn `android:tint="#000000"`. Fix = `AppCompatImageView` + `android:tint`. **Đừng** sửa tint trong `ic_close_ab.xml` (icon dùng chung nhiều màn).

⚠️ **Check Premium:** `com.example.piceditor.ads.Prefs` → `getPremium()==1` hoặc `isRemoveAd()`. SharedPreferences tên = string `app_name` ("Photo Collage Maker Grid Photo"), key "Premium"/"RemoveAd". `WatermarkUtil` (ở module) đọc qua `getIdentifier("app_name")`.

---

## 🧪 Testing
- **Pixel thật** (`192.168.31.253:5555`): có tài khoản Google Play → **billing trả giá thật** (₫237,000/năm, ₫53,000/tuần). Dùng test billing/free-trial.
- **Máy ảo** (`Pixel_4_API_34` = `emulator-5554`): KHÔNG có billing thật → giá "—", các dòng phụ thuộc giá ẩn (đúng behavior). Dùng test UI/flow/watermark.
- First-run flow: Splash → consent UMP → interstitial → quyền ảnh → (paywall first-run) → Home.
- Build: `gradlew :app:assembleDebug --offline`.

---

## 📚 Nguồn nghiên cứu (2024–2026)
RevenueCat State of Subscription Apps · Adapty · ScreensDesign teardowns (YouCam/VSCO/PhotoRoom/PicsArt). Kết luận chính: **onboarding paywall = 82–89% trial Day 0**; **watermark/output-gating = lever intent-cao nhất** cho photo app; **free-trial = unlock conversion lớn nhất** (opt-out ~48% vs opt-in ~18%); giữ **hybrid** (free+ads), đừng hard-wall.

---
*Cập nhật: phiên làm việc gần nhất — P0 hoàn tất, build PASS, git sạch.*
