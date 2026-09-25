# Celestial Arts – Tiên Đạo Thần Thông

Mod **Fabric cho Minecraft 1.20.1** mang hệ thống tu tiên / thần thông phong cách donghua vào game:
linh lực, cảnh giới, độ kiếp, 16 công pháp với **hiệu ứng hoàn toàn tự dựng** (không dùng particle hay
hiệu ứng có sẵn của vanilla), model riêng cho mọi thực thể, âm thanh tự tổng hợp và giao diện chọn kỹ năng.

> Mod ID: `celestialarts` · Minecraft `1.20.1` · Fabric Loader `≥ 0.15` · Fabric API `0.92.2+1.20.1` · Java 17

---

## Tính năng

### Hệ thống tu luyện
| Cảnh giới | Linh lực tối đa | Hồi phục / tick | Kinh nghiệm để đột phá |
|---|---|---|---|
| Luyện Khí | 100 | 0.35 | 400 |
| Trúc Cơ | 160 | 0.50 | 1 500 |
| Kim Đan | 240 | 0.70 | 4 000 |
| Nguyên Anh | 340 | 0.95 | 9 000 |
| Hóa Thần | 460 | 1.25 | 18 000 |
| Độ Kiếp | 620 | 1.70 | – |

* **Linh lực (Qi)** hồi tự nhiên, hồi nhanh gấp 3 khi *ngồi thiền* (sneak đứng yên trên mặt đất).
* **Kinh nghiệm** nhận được khi dùng công pháp, hạ quái, dùng linh thạch.
* **Đột phá** (phím `B` hoặc nút trong sách): khi đủ kinh nghiệm sẽ diễn ra *thiên kiếp* – mây kiếp
  tụ trên đầu, 3 – 9 tia lôi giáng xuống, kết thúc bằng trận pháp và cột sáng thăng cảnh.

### Thể phách theo cảnh giới (bị động)
Mỗi lần đột phá, thân thể tu sĩ được tôi luyện (áp dụng bằng attribute modifier, không cộng dồn):

| Cảnh giới | Máu | Sát thương | Tốc độ | Độ dẻo giáp | Kháng đẩy lùi | Thần thông bị động |
|---|---|---|---|---|---|---|
| Luyện Khí | – | – | – | – | – | – |
| Trúc Cơ | +2 ♥ | +1 | +6 % | +1 | 0.08 | **Lăng Không Bộ** – nhấn nhảy lần nữa giữa không trung (6 linh lực) |
| Kim Đan | +4 ♥ | +2 | +12 % | +2 | 0.16 | Miễn sát thương rơi |
| Nguyên Anh | +6 ♥ | +3 | +18 % | +3 | 0.24 | **Ngự Không** – giữ Shift giữa không trung để lơ lửng hạ chậm; không chết đuối |
| Hóa Thần | +8 ♥ | +4 | +24 % | +4 | 0.32 | Miễn sát thương lửa / dung nham |
| Độ Kiếp | +10 ♥ | +5 | +30 % | +5 | 0.40 | Lăng Không Bộ dùng được 2 lần |

Khi ngồi thiền, quanh người xuất hiện **hào quang tụ khí** và các hạt linh khí xoáy vào đan điền.

### 16 công pháp
| Công pháp | Hệ | Loại | Cảnh giới | Mô tả hiệu ứng |
|---|---|---|---|---|
| Kiếm Khí Trảm | Kiếm | Đạn | Luyện Khí | Vung kiếm tạo vòng cung slash, phóng lưỡi kiếm khí có model riêng bay xa và xuyên địch |
| Liệt Diễm Trảo | Hỏa | Cận chiến | Luyện Khí | Ba vệt trảo lửa xé không khí, đốt cháy kẻ địch; trảo cuối cào xuống đất để lại 3 rãnh lửa cháy 3 giây |
| Băng Tiễn | Băng | Đạn | Luyện Khí | 5 mũi băng tinh bay hình quạt, làm chậm & đóng băng mục tiêu |
| Lôi Bộ | Lôi | Di chuyển | Luyện Khí | Hóa tia chớp dịch chuyển tức thời, để lại vệt lôi quang; khi đáp xuống điện dư **lan dây chuyền** qua tối đa 3 kẻ địch |
| Phong Nhận Vũ | Phong | Tăng cường | Trúc Cơ | Nhiều lưỡi gió quay quanh người, chém mọi kẻ tới gần |
| Huyền Vũ Thuẫn | Thổ | Tăng cường | Trúc Cơ | Mai rùa lục giác bao quanh, hấp thụ sát thương, vỡ khi hết độ bền |
| Địa Liệt | Thổ | Diện rộng | Trúc Cơ | Đập đất – sóng chấn động lan ra, mặt đất nứt phát sáng, gai đá trồi lên hất tung, cuối vết nứt **phun trào** thành quạt 5 gai; người dùng được da đá (Kháng I) |
| Hỏa Liên | Hỏa | Đạn | Kim Đan | Đóa sen lửa bay chậm, nở bung khi trúng: cột lửa, cánh sen văng, cháy diện rộng |
| Thái Cực Trận | Đạo | Trận pháp | Kim Đan | Trận đồ âm dương – bát quái xoay dưới chân, hồi máu + hấp thụ cho đồng minh; nhịp **âm** hút địch vào tâm, nhịp **dương** đẩy văng ra |
| Ngự Kiếm Phi Hành | Kiếm | Di chuyển | Kim Đan | Triệu phi kiếm đứng lên bay tự do (điều khiển như thuyền bay, không bị kick fly) |
| Băng Phong Lĩnh Vực | Băng | Diện rộng | Kim Đan | Vòm băng, gai băng trồi lên, tuyết rơi; mọi kẻ địch trong vùng bị đóng băng |
| Vạn Kiếm Quy Tông | Kiếm | Triệu hồi | Nguyên Anh | Hàng chục linh kiếm hiện quanh người rồi lần lượt lao xuống mục tiêu |
| Tử Tiêu Thần Lôi | Lôi | Chùm tia | Nguyên Anh | Giữ phím để bắn chùm tia sét tím liên tục, có lõi trắng, vỏ xoáy, tia điện lan |
| Thôn Phệ Hư Không | Hư Không | Kênh | Nguyên Anh | Xoáy hư không hút kẻ địch vào tâm, rút máu & hồi linh lực |
| Cửu Thiên Lôi Kiếp | Lôi | Diện rộng | Hóa Thần | Gọi mây kiếp, 9 tia lôi lần lượt giáng xuống vùng nhắm |
| Thiên Kiếm | Đạo | Tuyệt kỹ | Độ Kiếp | Thiên kiếm khổng lồ giáng từ trời, cột sáng vàng, vết nứt & sóng xung kích hủy diệt |

### Hiệu ứng client tự dựng
* Hệ thống **FX** riêng (`render/fx`): 18 loại hiệu ứng thế giới (vòng xung kích, trận đồ, chùm tia, cột lửa,
  vòm băng, mây kiếp, vết nứt, xoáy, hào quang khí, cột sáng, sen nở, phù văn xoay…), render ở
  `WorldRenderEvents.AFTER_TRANSLUCENT` với **RenderLayer additive tự định nghĩa**.
* **Particle** riêng (15 loại, sprite sheet riêng qua mixin `ParticleManager`), có màu tùy chỉnh.
* **Model + renderer** cho mọi thực thể: kiếm khí, băng tiễn, hỏa liên, linh kiếm, phi kiếm, gai đá, thiên kiếm.
* Rung camera theo cường độ, HUD (thanh linh lực, thanh tu vi, 6 ô kỹ năng với hồi chiêu), màn hình
  **Đạo Thư** để gán kỹ năng vào ô.
* 24 âm thanh `.ogg` được tổng hợp riêng (không lấy từ vanilla).
* Mọi texture đều ≥ 32×32 (item 32², icon kỹ năng 32², trận đồ 256², thực thể tới 128²).

### Vật phẩm & lệnh
* **Bí tịch công pháp** (16 cuộn) – chuột phải để lĩnh ngộ, **Linh thạch / Linh thạch thượng phẩm** – hồi linh lực + kinh nghiệm,
  **Trúc Cơ Đan / Nguyên Anh Đan**, **Đạo Thư** (mở màn hình kỹ năng), **Tiên Kiếm**.
* Lệnh `/celestial info|learn|forget|realm|qi|exp|breakthrough` (cần quyền OP).
* **Công thức chế tạo**: linh thạch (thạch anh tím + lapis), linh thạch thượng phẩm, đạo thư, đan dược, tiên kiếm và
  7 bí tịch cảnh giới thấp (Luyện Khí / Trúc Cơ). Bí tịch cảnh giới cao chỉ tìm thấy trong **rương công trình**
  (stronghold, mansion, bastion, ancient city, end city…) – độ hiếm tăng theo cảnh giới yêu cầu.
* **Thành tựu** riêng (tab *Tiên Lộ*): nhặt linh thạch, lĩnh ngộ công pháp đầu tiên, từng lần đột phá cảnh giới,
  học Ngự Kiếm / Cửu Thiên Lôi Kiếp / Thiên Kiếm, và lĩnh ngộ đủ 16 công pháp.

### Phím mặc định
`R F V G C Z` – 6 ô kỹ năng · `K` – mở Đạo Thư · `B` – đột phá. Kỹ năng kênh (chùm tia, xoáy) giữ phím / bấm lại để ngắt.

---

## Build

```bash
./gradlew build
# file jar nằm ở build/libs/celestialarts-1.1.0.jar
```

Yêu cầu JDK 17. Chạy client dev: `./gradlew runClient`.

### Kiểm thử tự động (GameTest) & CI

Mod kèm bộ **game test chạy trên server headless** (`gametest/CelestialGameTests.java`, entrypoint
`fabric-gametest`), chạy bằng:

```bash
./gradlew runGametest      # báo cáo JUnit tại build/junit.xml
```

Các bài test hiện có (tất cả đều **pass** trên GitHub Actions):

| Test | Kiểm tra |
|---|---|
| `castEverySkill` | 16 công pháp đều thi triển được ở cảnh giới Độ Kiếp, không ném exception |
| `projectileSkillsSpawnEntities` | Kiếm khí / băng tiễn / hoả liên / vạn kiếm / thiên kiếm sinh đúng entity |
| `swordFlightMountsPlayer` | Ngự kiếm phi hành: người chơi cưỡi kiếm, bấm lần nữa thì hạ xuống |
| `channelSkillStopsOnSecondPress` | Kỹ năng niệm (tử lôi quang trụ) chặn kỹ năng khác và dừng khi bấm lại |
| `qiCostAndCooldownApplied` | Trừ linh lực, đặt hồi chiêu, từ chối khi thiếu linh lực / đang hồi chiêu |
| `realmPassivesScaleWithRealm` | Thể phách theo cảnh giới: +máu đúng mức, không cộng dồn, miễn rơi/lửa/đuối đúng cảnh giới, Lăng Không Bộ trừ linh lực |
| `breakthroughAdvancesRealm` | Đột phá Luyện Khí → Trúc Cơ sau khi độ kiếp 140 tick |
| `qiNbtRoundTrip` | Lưu / đọc NBT dữ liệu tu luyện |
| `fxDataRoundTrip` | Gói tin FX serialize / deserialize chính xác |
| `dataPackContentLoaded` | Công thức, advancement, entity, item của mod được đăng ký và nạp |
| `skillEntitiesTickWithoutCrashing` | Mọi entity kỹ năng tick 100 tick không crash |

Ngoài ra còn có **client auto-test** (`client/autotest/CelestialAutoTest.java`, bật bằng
`-Dcelestialarts.autotest`, task `./gradlew runAutoTestClient`): mở client thật (trong CI chạy dưới xvfb),
audit mixin, tạo thế giới superflat, học toàn bộ công pháp qua `/celestial`, thi triển cả 16 công pháp qua gói tin
thật, spawn đủ 18 loại FX + 15 loại particle, mở Đạo Thư, kiểm tra ngự kiếm cưỡi/hạ, chụp ~25 ảnh màn hình rồi thoát.

Workflow `.github/workflows/build.yml` build jar, chạy game test server + client auto-test và ghi log (lỗi biên
dịch, báo cáo JUnit, cảnh báo, ảnh chụp thu nhỏ) vào issue theo dõi #1 sau mỗi lần push; ảnh gốc nằm trong
artifact `client-screenshots`.

## Cấu trúc mã

```
src/main/java/com/ngoducduy/celestialarts
├── cultivation/   Realm, PlayerQi, QiHolder, Breakthrough (thiên kiếp), CultivationEvents
├── skill/         Skill, SkillRegistry, SkillManager, SkillContext, cast/ (kỹ năng kéo dài), skills/ (16 công pháp)
├── entity/        7 thực thể kỹ năng (đạn, phi kiếm, gai đá, thiên kiếm)
├── network/       FxType, FxData, ModPackets (C2S cast/release/slot/breakthrough, S2C sync/fx/shake)
├── registry/      ModEntities, ModItems, ModParticles, ModSounds, ModEffects, ModDamageTypes, ModLootTables, ModAdvancements
├── effect/        Hiệu ứng trạng thái (Đóng băng, Linh hỏa thiêu đốt, Kiếm ý)
├── item/          Bí tịch, linh thạch, đan dược, đạo thư, tiên kiếm
└── command/       /celestial

src/client/java/com/ngoducduy/celestialarts/client
├── render/layer/  RenderLayer additive / glow tự định nghĩa
├── render/fx/     ClientFxManager + 18 loại hiệu ứng thế giới
├── render/entity/ Model + renderer cho từng thực thể
├── particle/      15 particle riêng + sprite sheet riêng
├── gui/           SkillHud, SkillBookScreen
└── mixin/         Camera (rung), BipedEntityModel (đứng trên kiếm), ParticleManager (sheet riêng)

tools/  gen_fx_textures.py · gen_entity_textures.py · gen_gui_textures.py · gen_sounds.py (Pillow, numpy, soundfile)
```

Tất cả texture và âm thanh được sinh bằng các script trong `tools/`, có thể chỉnh sửa và chạy lại bất cứ lúc nào.

## Giấy phép
MIT – xem `LICENSE`.
