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

### 16 công pháp
| Công pháp | Hệ | Loại | Cảnh giới | Mô tả hiệu ứng |
|---|---|---|---|---|
| Kiếm Khí Trảm | Kiếm | Đạn | Luyện Khí | Vung kiếm tạo vòng cung slash, phóng lưỡi kiếm khí có model riêng bay xa và xuyên địch |
| Liệt Diễm Trảo | Hỏa | Cận chiến | Luyện Khí | Ba vệt trảo lửa xé không khí, đốt cháy kẻ địch phía trước |
| Băng Tiễn | Băng | Đạn | Luyện Khí | 5 mũi băng tinh bay hình quạt, làm chậm & đóng băng mục tiêu |
| Lôi Bộ | Lôi | Di chuyển | Luyện Khí | Hóa tia chớp dịch chuyển tức thời, để lại vệt lôi quang & sốc điện |
| Phong Nhận Vũ | Phong | Tăng cường | Trúc Cơ | Nhiều lưỡi gió quay quanh người, chém mọi kẻ tới gần |
| Huyền Vũ Thuẫn | Thổ | Tăng cường | Trúc Cơ | Mai rùa lục giác bao quanh, hấp thụ sát thương, vỡ khi hết độ bền |
| Địa Liệt | Thổ | Diện rộng | Trúc Cơ | Đập đất – sóng chấn động lan ra, mặt đất nứt phát sáng, gai đá trồi lên hất tung |
| Hỏa Liên | Hỏa | Đạn | Kim Đan | Đóa sen lửa bay chậm, nở bung khi trúng: cột lửa, cánh sen văng, cháy diện rộng |
| Thái Cực Trận | Đạo | Trận pháp | Kim Đan | Trận đồ âm dương – bát quái xoay dưới chân, hồi máu đồng minh, đẩy lùi & làm chậm địch |
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

### Phím mặc định
`R F V G C Z` – 6 ô kỹ năng · `K` – mở Đạo Thư · `B` – đột phá. Kỹ năng kênh (chùm tia, xoáy) giữ phím / bấm lại để ngắt.

---

## Build

```bash
./gradlew build
# file jar nằm ở build/libs/celestialarts-1.0.0.jar
```

Yêu cầu JDK 17. Chạy client dev: `./gradlew runClient`.

> **Lưu ý:** mã nguồn được viết và đối chiếu kỹ với Yarn mappings `1.20.1+build.10` và Fabric API `0.92.2`,
> nhưng môi trường tạo mod này không có JDK nên **chưa được biên dịch tại chỗ**. Nếu gặp lỗi biên dịch khi build
> lần đầu, hãy mở issue kèm log – thường chỉ là chỉnh nhỏ.

## Cấu trúc mã

```
src/main/java/com/ngoducduy/celestialarts
├── cultivation/   Realm, PlayerQi, QiHolder, Breakthrough (thiên kiếp), CultivationEvents
├── skill/         Skill, SkillRegistry, SkillManager, SkillContext, cast/ (kỹ năng kéo dài), skills/ (16 công pháp)
├── entity/        7 thực thể kỹ năng (đạn, phi kiếm, gai đá, thiên kiếm)
├── network/       FxType, FxData, ModPackets (C2S cast/release/slot/breakthrough, S2C sync/fx/shake)
├── registry/      ModEntities, ModItems, ModParticles, ModSounds, ModEffects, ModDamageTypes
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
