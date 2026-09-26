# Celestial Arts – Tiên Đạo Thần Thông

Mod **Fabric cho Minecraft 1.20.1** mang hệ thống tu tiên / thần thông phong cách donghua vào game:
linh lực, cảnh giới, độ kiếp, 20 công pháp với **hiệu ứng hoàn toàn tự dựng** (không dùng particle hay
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

### 20 công pháp
| Công pháp | Hệ | Loại | Cảnh giới | Mô tả hiệu ứng |
|---|---|---|---|---|
| Kiếm Khí Trảm | Kiếm | Đạn | Luyện Khí | Vung kiếm tạo vòng cung slash, phóng lưỡi kiếm khí có model riêng bay xa và xuyên địch; từ Kim Đan phóng **3 lưỡi hình quạt** |
| Kim Cương Chưởng | Đạo | Cận chiến | Luyện Khí | Kim ấn lóe lên trước lòng bàn tay, hất văng mọi thứ trong hình nón; địch bị đập vào tường chịu thêm sát thương |
| Liệt Diễm Trảo | Hỏa | Cận chiến | Luyện Khí | Ba vệt trảo lửa xé không khí, đốt cháy kẻ địch; trảo cuối cào xuống đất để lại 3 rãnh lửa cháy 3 giây |
| Băng Tiễn | Băng | Đạn | Luyện Khí | 5 mũi băng tinh bay hình quạt, làm chậm & đóng băng mục tiêu |
| Lôi Bộ | Lôi | Di chuyển | Luyện Khí | Hóa tia chớp dịch chuyển tức thời, để lại vệt lôi quang; khi đáp xuống điện dư **lan dây chuyền** qua tối đa 3 kẻ địch |
| Phong Nhận Vũ | Phong | Tăng cường | Trúc Cơ | Nhiều lưỡi gió quay quanh người, chém mọi kẻ tới gần |
| Huyền Vũ Thuẫn | Thổ | Tăng cường | Trúc Cơ | Mai rùa lục giác bao quanh, hấp thụ sát thương, vỡ khi hết độ bền |
| Địa Liệt | Thổ | Diện rộng | Trúc Cơ | Đập đất – sóng chấn động lan ra, mặt đất nứt phát sáng, gai đá trồi lên hất tung, cuối vết nứt **phun trào** thành quạt 5 gai; người dùng được da đá (Kháng I) |
| Phong Long Quyển | Phong | Đạn | Trúc Cơ | Phong long cuộn thành **lốc xoáy** có đầu rồng (model riêng) lăn bám theo mặt đất, leo bậc, hút địch vào tâm, nhấc bổng rồi nổ tung |
| Hỏa Liên | Hỏa | Đạn | Kim Đan | Đóa sen lửa bay chậm, nở bung khi trúng: cột lửa, cánh sen văng, cháy diện rộng |
| Thái Cực Trận | Đạo | Trận pháp | Kim Đan | Trận đồ âm dương – bát quái xoay dưới chân, hồi máu + hấp thụ cho đồng minh; nhịp **âm** hút địch vào tâm, nhịp **dương** đẩy văng ra |
| Ngự Kiếm Phi Hành | Kiếm | Di chuyển | Kim Đan | Triệu phi kiếm đứng lên bay tự do (điều khiển như thuyền bay, không bị kick fly) |
| Băng Phong Lĩnh Vực | Băng | Diện rộng | Kim Đan | Vòm băng, gai băng trồi lên, tuyết rơi; mọi kẻ địch trong vùng bị đóng băng |
| Vạn Kiếm Quy Tông | Kiếm | Triệu hồi | Nguyên Anh | Hàng chục linh kiếm hiện quanh người rồi lần lượt lao xuống mục tiêu |
| Tử Tiêu Thần Lôi | Lôi | Chùm tia | Nguyên Anh | Giữ phím để bắn chùm tia sét tím liên tục, có lõi trắng, vỏ xoáy, tia điện lan |
| Thôn Phệ Hư Không | Hư Không | Kênh | Nguyên Anh | Xoáy hư không hút kẻ địch vào tâm, rút máu & hồi linh lực |
| Đại Nhật Kim Thân | Đạo | Tăng cường | Nguyên Anh | Kim thân 10 giây: hóa giải & phản lại một nửa mỗi đòn, miễn lửa, Sức mạnh, hào quang mặt trời + phù văn xoay |
| Cửu Thiên Lôi Kiếp | Lôi | Diện rộng | Hóa Thần | Gọi mây kiếp, 9 tia lôi lần lượt giáng xuống vùng nhắm |
| Lôi Long Phá | Lôi | Đạn | Hóa Thần | Tụ lôi rồi phóng **lôi long** tím: đầu rồng model riêng, thân là dải sét vẽ theo vệt bay, tự săn địch, xuyên 3 mục tiêu, phân nhánh sang kẻ bên cạnh và nổ ở cuối đường |
| Thiên Kiếm | Đạo | Tuyệt kỹ | Độ Kiếp | Thiên kiếm khổng lồ giáng từ trời, cột sáng vàng, vết nứt & sóng xung kích hủy diệt |

### Hiệu ứng client tự dựng
* Hệ thống **FX** riêng (`render/fx`): 18 loại hiệu ứng thế giới (vòng xung kích, trận đồ, chùm tia, cột lửa,
  vòm băng, mây kiếp, vết nứt, xoáy, hào quang khí, cột sáng, sen nở, phù văn xoay…), render ở
  `WorldRenderEvents.AFTER_TRANSLUCENT` với **RenderLayer additive tự định nghĩa**.
* **Particle** riêng (15 loại, sprite sheet riêng qua mixin `ParticleManager`), có màu tùy chỉnh.
* **Model + renderer** cho mọi thực thể: kiếm khí, băng tiễn, hỏa liên, linh kiếm, phi kiếm, gai đá, thiên kiếm,
  phong long (phễu lốc + đầu rồng) và lôi long (đầu rồng + thân sét theo lịch sử vị trí).
* Rung camera theo cường độ, HUD (thanh linh lực, thanh tu vi, 6 ô kỹ năng với hồi chiêu), màn hình
  **Đạo Thư** để gán kỹ năng vào ô.
* 29 sự kiện âm thanh / 43 file `.ogg` được tổng hợp riêng từ "vật liệu" vật lý (không khí, va đập, kim loại, hồ quang điện,
  đất đá, chiêng) – không lấy từ vanilla; các âm hay lặp có 2–3 biến thể ngẫu nhiên.
* Mọi texture đều ≥ 32×32 (item 32², icon kỹ năng 32², trận đồ 256², thực thể tới 128²).

### Vật phẩm & lệnh
* **Bí tịch công pháp** (20 cuộn) – chuột phải để lĩnh ngộ, **Linh thạch / Linh thạch thượng phẩm** – hồi linh lực + kinh nghiệm,
  **Trúc Cơ Đan / Nguyên Anh Đan / Hồi Linh Đan / Thăng Thiên Đan**, **Đạo Thư** (mở màn hình kỹ năng, có lật trang), **Tiên Kiếm**.
* Lệnh `/celestial info|learn|forget|realm|qi|exp|breakthrough` (cần quyền OP).
* **Công thức chế tạo**: linh thạch (thạch anh tím + lapis), linh thạch thượng phẩm, đạo thư, đan dược, tiên kiếm và
  9 bí tịch cảnh giới thấp (Luyện Khí / Trúc Cơ). Bí tịch cảnh giới cao chỉ tìm thấy trong **rương công trình**
  (stronghold, mansion, bastion, ancient city, end city…) – độ hiếm tăng theo cảnh giới yêu cầu.
* **Thành tựu** riêng (tab *Tiên Lộ*): nhặt linh thạch, lĩnh ngộ công pháp đầu tiên, từng lần đột phá cảnh giới,
  học Ngự Kiếm / Cửu Thiên Lôi Kiếp / Thiên Kiếm, và lĩnh ngộ đủ 20 công pháp.

### Phím mặc định
`R F V G C Z` – 6 ô kỹ năng · `K` – mở Đạo Thư · `B` – đột phá. Kỹ năng kênh (chùm tia, xoáy) giữ phím / bấm lại để ngắt.

---

## Nhật ký cập nhật

### 1.3.5 – Làm lại toàn bộ âm thanh
* **Tất cả 24 âm thanh cũ bị thay** – bản cũ dựng từ các sóng sin tắt dần nên cái gì cũng nghe như chuông. Bộ mới trong
  `tools/gen_sounds.py` tổng hợp từ thành phần vật lý: nhiễu lọc cộng hưởng quét tần (tiếng rít gió), va đập (sub-bass + tiếng
  nổ + thân cộng hưởng + bão hòa), kim loại modal ngắn (tiếng "keng" lưỡi kiếm), Karplus-Strong (đàn tranh), hồ quang điện
  (răng cưa/vuông ngắt quãng + tia lách tách), đất đá (nhiễu nâu + tiếng nứt cộng hưởng thấp + sỏi), chiêng chùa (chỉ dùng
  cho trận pháp/đột phá), reverb Schroeder, nén bus, cắt đuôi im lặng.
* Mỗi kỹ năng có âm đúng chất: kiếm khí = xé gió + keng thép; hỏa = gầm lửa rối + lách tách; băng = tinh thể lớn dần + nứt;
  lôi = tiếng nổ tức thời + xé + ầm kéo dài; phong = rít gió cộng hưởng; thổ = rung nền + đá nứt; hư không = hút ngược +
  drone vực sâu; khiên = "vwoom" khóa lại; đột phá = dồn nén → va đập → chiêng nở → dư âm.
* **5 sự kiện mới** gắn cho kỹ năng trước đây phải mượn âm khác: `palm_strike` (Kim Cang Chưởng), `golden_body` (Kim Thân),
  `dragon_roar` (Lôi Long / Phong Long – tiếng gầm formant thật), `void_collapse` (Thôn Phệ Vực nổ sập), `freeze_field`
  (Băng Phong Vực). Có phụ đề vi/en.
* Âm hay lặp (kiếm khí, phóng kiếm, lửa, gió, sét, địa chấn, băng vỡ, khiên đỡ, chưởng, long ngâm) có 2–3 biến thể ngẫu nhiên.
* Script có `--analyse` in RMS, trọng tâm phổ (A-weighted), tỉ lệ năng lượng theo dải và độ phẳng phổ để kiểm tra cân bằng.

### 1.3.4 – Vạn Kiếm Quy Tông diện rộng
* Theo góp ý: **72 phi kiếm** (6 hàng × 12), giãn cách >1 ô, bán kính 3 → 7,5 ô và cao 1,2 → 5,7 ô sau lưng, xòe ±85° – một
  "bầu trời kiếm" thay vì bức tường sát người. Phóng 2 kiếm/tick, truy kích chia đều tới 6 mục tiêu trong nón 40 ô; không
  có địch thì rải xuống vùng 7×7 ô quanh điểm nhìn.

### 1.3.3 – Vạn Kiếm Quy Tông mới, sửa trận pháp bị xéo, Liệt Diễm Trảo xa hơn
* **Sửa lỗi hình học quan trọng**: `ClientFx.alignY/alignZ` dùng góc yaw kiểu Minecraft (`atan2(-x, z)`) với phép quay
  thuận tay phải nên **mọi hiệu ứng có hướng bị lật gương theo trục X** khi người chơi không nhìn dọc ±Z – trận pháp của
  Kim Cang Chưởng / Hàn Băng Tiễn nhìn "bị xéo", tia/luồng lệch hướng. Ảnh CI không lộ vì bài test luôn nhìn +Z.
* **Vạn Kiếm Quy Tông** làm lại: người chơi đứng yên, **48 phi kiếm** ngưng tụ thành 4 hàng cánh mở rộng dần **sau lưng**,
  mũi kiếm hướng về phía trước, hàng trong bung ra trước; sau ~1,7 s kiếm lần lượt lao thẳng qua người chơi rồi truy kích
  (chia đều tối đa 4 mục tiêu trong nón 36 ô), không có địch thì bay tới điểm đang nhìn và nổ kiếm khí.
* **Liệt Diễm Trảo**: tầm 4,2 → 7 ô, thêm lưỡi trảo lửa lớn bay ra giữa tầm, than hồng dài 6 ô.

### 1.3.2 – Phế bỏ công pháp ngay trong Đạo Kinh
* Người chơi thường giờ có thể **gỡ công pháp đã học**: mở Đạo Kinh (phím mặc định), chọn công pháp → nút **Phế bỏ** → bấm
  lần nữa trong 3 giây để xác nhận. Server hủy chiêu đang chạy, gỡ khỏi mọi ô kỹ năng và **trả lại Bí Tịch** vào túi (rơi
  xuống chân nếu túi đầy) nên có thể học lại hoặc trao cho người khác.
* Gói tin mới `forget_skill`; `/celestial forget` dùng chung đường xử lý (không hoàn Bí Tịch, im lặng).
* Game test mới `forgetSkillRefundsScrollAndClearsSlot` (13 test).

### 1.3.1 – Sửa lỗi crash khi chạy jar phát hành (mixin client không tìm thấy target)
* Jar 1.3.0 tải về chạy ngoài môi trường dev bị lỗi `CameraMixin ... could not find any targets matching 'update'`
  (và tương tự cho `ParticleManagerMixin`, `BipedEntityModelMixin`). Nguyên nhân: với `splitEnvironmentSourceSets()`
  Loom sinh refmap riêng cho source set `client` (`client-celestialarts-refmap.json`), nhưng `celestialarts.client.mixins.json`
  nằm ở `src/main/resources` nên bị gắn refmap của `main` – không có entry cho các mixin client. Đã chuyển file cấu hình
  sang `src/client/resources`.
* CI có thêm bước `tools/check_jar.py` kiểm tra **jar thật sau remap**: mọi mixin config phải có refmap, mọi lớp mixin phải
  có entry trong refmap và target phải là tên intermediary – lỗi kiểu này sẽ chặn build ngay thay vì lọt ra bản phát hành.

### 1.3.0 – Đại tu hình ảnh: bloom thật, tàn ảnh, vết tích mặt đất, hiệu ứng màn hình
* **Bloom (hào quang) thật sự** cho *mọi* hiệu ứng phát sáng: lớp render cộng màu, tia sét và hạt phát sáng được vẽ thêm vào một
  framebuffer riêng (`GlowPass`), làm mờ Gaussian 3 cấp bằng core shader `celestialarts:glow_blur` rồi cộng ngược lên khung
  hình. Ánh sáng "tràn" ra ngoài hình học như phim donghua thay vì chỉ là mảng màu sáng. Tự tắt khi bật đồ hoạ Fabulous.
* **Hiệu ứng màn hình** (`SCREEN_FLASH`): chớp sáng cộng màu khi đòn lớn chạm đất (Thiên Kiếm, Hỏa Liên, Lôi Long, Lôi Bộ…)
  và **màn trời tối sầm** với vignette tím suốt Cửu Thiên Lôi Kiếp / độ kiếp đột phá. Suy giảm theo khoảng cách (24 ô).
* **Tàn ảnh** (`AFTERIMAGE`): Lôi Bộ để lại 6 bóng ma của chính người chơi (da, giáp, vật cầm) dọc đường dịch chuyển, tan dần.
* **Vết tích mặt đất** (`GROUND_DECAL`): đất cháy đen còn than hồng sau Hỏa Liên/Hỏa Trảo/Thiên Kiếm/Lôi Kiếp/Địa Liệt,
  mảng băng dưới Băng Vực và nơi Băng Tiễn vỡ – tồn tại 15–40 giây rồi mờ dần.
* **Tụ khí trước chiêu lớn**: hạt linh khí hội tụ vào tay khi gồng Hỏa Liên, Lôi Long, Cửu Thiên Lôi Kiếp và khi thiền đột phá.
* Tia Tử Tiêu nhìn dọc trục (góc nhìn thứ ba phía sau) được vuốt mảnh dần như góc nhìn thứ nhất, không còn là đĩa sáng chói.
* Cân chỉnh lại độ sáng chung (gain lớp cộng màu 0.62, bloom 0.30/0.26/0.22) để không cháy trắng sau khi có bloom.

### 1.2.1 – Sửa lỗi & tinh chỉnh hiệu ứng (không thêm nội dung mới)
* **Sửa lỗi render quan trọng**: bộ đệm hiệu ứng chỉ có một buffer dự phòng nên khi một hiệu ứng lấy 2 lớp render rồi vẽ
  đan xen, hình học bị đẩy nhầm sang lớp/texture khác (tia Tử Tiêu vẽ bằng texture lõi, mây lôi kiếp vẽ bằng texture vòng…).
  Nay mỗi lớp hiệu ứng có buffer riêng.
* **Đạn kỹ năng không còn quay lại đánh chủ nhân/thú cưng/đồng đội** (Hỏa Liên, Kiếm Khí, Vạn Kiếm…) – dùng chung bộ lọc
  `EntityUtil.isValidTarget`.
* **Huyền Vũ Thuẫn chỉ hấp thụ đòn mà vanilla thật sự áp dụng** – không còn bị rút cạn (và kêu/nổ hạt) bởi mỗi tick bốc cháy
  trong 10 tick miễn thương, hay khi người chơi bất tử/kháng lửa.
* **Kỹ năng đang chạy được đóng đúng cách khi chết, đổi chiều, thoát game** (Ngự Kiếm không còn bỏ lại kiếm bay, khiên/vùng
  không "treo").
* Lôi Long Phá: thân rồng liền mạch (không còn đứt khúc), uốn lượn, vỏ tím dùng lớp mờ nên vẫn tím trên nền trời sáng.
* Mây lôi kiếp có khối (vành mây hướng camera, vòm trên) + tia sét bò dưới đáy mây.
* Thái Cực Trận đọc được âm-dương (nửa âm tối thật, không còn bị nhân đôi/mờ).
* Phong Nhận Vũ: lưỡi gió có "cánh buồm" đứng nên nhìn thấy cả từ góc nhìn thứ nhất; Kim Cương Chưởng bớt chói khi tự thi triển;
  mảnh đá Địa Liệt nhỏ và đa dạng hơn.

### 1.2.0 – "Siêu cập nhật"
* **4 công pháp mới**: Kim Cương Chưởng, Phong Long Quyển, Đại Nhật Kim Thân, Lôi Long Phá – kèm 2 thực thể mới có model
  (`DragonHeadModel` dùng chung), renderer riêng, icon, bí tịch, công thức.
* **Sát thương thần thông tăng theo cảnh giới** (+6 %/cảnh giới) qua `RealmPassives.skillDamageMultiplier`.
* Nâng cấp hầu hết công pháp cũ: Kiếm Khí Trảm 3 lưỡi, Băng Tiễn vỡ mảnh khi trúng mục tiêu đóng băng, Phong Nhận Vũ
  kết thúc bằng bão lưỡi gió, Huyền Vũ Thuẫn phản chấn khi vỡ + Hấp thụ khi hết hạn, Hỏa Liên để lại **biển lửa**,
  Thiên Kiếm để lại **phong ấn** làm chậm/suy yếu địch & hồi máu đồng minh, Ngự Kiếm đâm xuyên & **giáng địa** khi bổ nhào,
  Băng Phong Lĩnh Vực đóng băng lại theo chu kỳ, Thôn Phệ nổ tung theo lượng đã nuốt, Tử Tiêu Thần Lôi có 3 cấp dày dần
  và phân nhánh, Cửu Thiên Lôi Kiếp **đóng dấu lôi ấn** (mỗi 3 dấu nổ), Vạn Kiếm bay theo hướng nhìn khi không có mục tiêu.
* Lớp `ZoneCast` dùng chung cho vùng hiệu ứng theo thời gian; Đạo Thư lật trang; 2 đan dược mới; thêm gametest.

## Build

```bash
./gradlew build
# file jar nằm ở build/libs/celestialarts-1.2.1.jar
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

tools/  gen_fx_textures.py · gen_entity_textures.py · gen_gui_textures.py · gen_sounds.py (Pillow, numpy, scipy, soundfile)
```

Tất cả texture và âm thanh được sinh bằng các script trong `tools/`, có thể chỉnh sửa và chạy lại bất cứ lúc nào.

## Giấy phép
MIT – xem `LICENSE`.
