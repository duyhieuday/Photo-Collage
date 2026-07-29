# Master config for template photo-cells. ASCII only. Dot-sourced by gen_cells.ps1 & verify_cells.ps1.
# Cells from Figma frames (verified via overlay by sub-agents). Logic space 1125x2000.

$CELLS = [ordered]@{
  # ---- Birthday ----
  # bd01 Snoopy bac: 3 polaroid TRANG nghieng (-9/4/-16.5) - NONE + minrect white
  "bd01"=@(@(383,453,841,943,-9),@(108,1003,567,1492,4),@(591,1410,1050,1901,-16.5))
  # bd02 "to me": GRAY mask (vung tren XAM 237 = o LON, + 3 o nho duoi giu caption). Them o lon.
  "bd02"=@(@(0,0,1125,1415),@(0,1431,335,1795),@(393,1431,731,1796),@(789,1432,1124,1796))
  # bd03 lich 7.14: 2 polaroid TRANG nghieng (10.5/-10.5) - NONE + minrect white
  "bd03"=@(@(423,134,982,880,10.5),@(171,1017,717,1746,-10.5))
  # bd04 gingham: 2 o XAM ~205 nghieng (6/-7), goc vuong - NONE + rect xoay
  "bd04"=@(@(622,443,1051,1009,6),@(722,1153,1091,1644,-7))
  "bd05"=@(,@(267,642,955,1567))
  "bd06"=@(,@(152,158,963,1230))
  "bd07"=@(,@(118,566,966,1374))
  "bd08"=@(,@(231,711,894,1457))
  # bd09 Yankees: 2 polaroid TRANG (gan thang) - NONE + minrect white
  "bd09"=@(@(70,551,589,1083,-1),@(599,1220,1034,1663))
  "bd10"=@(@(608,424,1083,889),@(608,935,1083,1402),@(608,1449,1083,1924),@(88,1449,562,1924))

  # ---- Couple ----
  # cp01 Anniversary: so do CU (nghieng -7 / -3.4) lam o khong khop khung nen template bi an
  #   khoi catalog. Do lai bang diff Temp_/Thumb_ o dot import 2 -> xem muc "Couple 01, 11..20".
  # cp02 giay rach: 3 dai XAM 232 + divider TRANG -> GRAY mask (giu mep rach)
  "cp02"=@(@(0,0,1125,656),@(0,661,1125,1334),@(0,1331,1125,2000))
  # cp03 film strip: khung NGHIENG ~ -12.7 deg (rect = khung thang quanh tam + goc xoay)
  "cp03"=@(@(84,92,730,576,-12.7),@(202,631,848,1111,-12.5),@(323,1166,959,1662,-12.9))
  "cp04"=@(@(500,40,1069,788),@(111,1085,672,1833))
  "cp05"=@(@(117,161,531,712),@(416,826,855,1412))
  # cp06: man hinh may anh + o trang the LOVERS CLUB (do tay tu luoi, verify vfill)
  # cp06 (WHITE mask, o bao trum man hinh + o card)
  # cp06 (WHITE mask): man hinh 5 deg + card -4.5 deg (min-area-rect)
  "cp06"=@(@(465,495,812,955,5),@(193,1294,547,1649,-4.5))
  # cp07 Twitter You/Me: 2 o anh card XAM 237 nghieng (-8/4.5) -> GRAY mask (giu card+chu)
  "cp07"=@(@(168,353,721,836,-8),@(362,1295,915,1778,4.5))
  # cp08 "Dear my darling": 2 o TRANG 255, hoa che goc -> WHITE mask (clip dung hinh o + hoa noi tren).
  #   o tren THUC SU nghieng ~23 deg (flood-fill xac nhan: canh dai top->right = 23 deg).
  "cp08"=@(@(355,645,890,1056,23.5),@(287,1152,651,1570,2.5))
  "cp09"=@(@(145,344,560,759),@(603,344,1017,759),@(145,803,560,1217),@(603,803,1017,1217),@(145,1261,560,1676),@(603,1261,1017,1676))

  # ---- Glad season ----
  # gs01 Happy Graduation: 1 o XAM 237 nghieng -4.5 (polaroid treo) -> GRAY mask (giu vien+kep+may)
  "gs01"=@(,@(243,850,925,1672,-4.5))
  # gs02 typewriter: 2 o TRANG 255, nen kem 242 (khong WHITE-mask duoc) -> NONE
  "gs02"=@(@(188,349,936,838),@(187,870,936,1286))
  # gs03 BACK to SCHOOL (most-popular): 4 polaroid TRANG nghieng nhe; cell cu THANG -> them goc (NONE, nen trang)
  "gs03"=@(@(101,469,540,1086,-3),@(557,487,1020,1083,1.5),@(589,1126,1022,1702,-2.5),@(119,1125,552,1701,0.5))
  "gs04"=@(@(32,52,368,708),@(392,52,732,708),@(756,52,1092,708),@(32,1292,368,1944),@(392,1292,732,1944),@(756,1292,1092,1944))
  # gs05 THE CLASS (blue grid): 2 o TRANG 255 (o lon BO GOC), nen XANH -> WHITE mask (clip bo goc, khung xanh+textbox giu)
  "gs05"=@(@(80,80,1049,1049),@(59,1592,457,1968))
  "gs06"=@(@(68,620,1056,1140),@(552,1172,1056,1628))
  # gs07 CLASS OF 26: 3 polaroid TRANG nghieng (-10.5/4.5/-5.5) - NONE + minrect white
  "gs07"=@(@(100,138,567,615,-10.5),@(615,796,1054,1239,4.5),@(64,1370,520,1830,-5.5))
  # gs08 Seniors: o phone (-6, giu nguyen - OK) + polaroid (6 deg). Polaroid duoi bi toi -> do mep tay, rect bao kin polaroid
  "gs08"=@(@(93,451,545,1040,-6),@(643,967,1060,1397,6))
  "gs09"=@(@(519,477,997,814),@(91,1006,600,1370))
  "gs10"=@(,@(222,628,856,1306))

  # ---- IG Story ----
  "is01"=@(@(83,275,542,900),@(52,973,510,1598),@(615,399,1073,1024),@(583,1097,1042,1722))
  # is02 music player: 1 o anh TRANG 255 (khung giay 243 -> WHITE mask cat ca khung, dung NONE); cell cu lech xuong
  "is02"=@(,@(155,212,971,1024))
  "is03"=@(,@(251,599,901,1288))
  # is04 Pochacco: o anh = vung TRANG 255 (vien xam polaroid bao ngoai); NONE + rect = dung mep o trang
  "is04"=@(@(112,158,522,572),@(599,734,1030,1177,1),@(90,1370,521,1810,1))
  # is05 notebook: 5 o NGHIENG (GRAY mask + o xoay; seedflood 4-corner fit chuan)
  "is05"=@(@(123,266,467,746,-4.5),@(503,410,1032,892,-4.5),@(162,775,508,1255,-4.5),@(542,925,1073,1407,-4),@(203,1284,545,1764,-4.5))
  # is06: o chu nhat trang + hinh tim/may (bao long, se dung MaskMode.WHITE clip theo hinh)
  "is06"=@(@(53,329,530,1019),@(573,314,1075,905),@(40,1135,550,1595),@(574,1054,1056,1751))
  # is07 Lovely Barbie: 2 khung BO GOC, o trong XAM 237 NGHIENG (-3 / 6 deg) -> GRAY mask clip bo goc
  "is07"=@(@(178,210,829,776,-3),@(273,947,1019,1617,6))
  # is08 Save The Moments: 3 khung giay RACH, o trong XAM 237 NGHIENG (13.5 / -2 / -13 deg) -> GRAY mask
  "is08"=@(@(162,169,708,716,13.5),@(106,1229,652,1776,-13),@(514,730,1060,1276,-2))
  "is09"=@(@(136,552,386,851),@(430,552,989,1536),@(136,895,386,1194),@(136,1238,386,1536))
  # is10 Vinyl: 2 polaroid o trong TRANG 255 (vien 231 tach duoc), NGHIENG (7 / -17 deg) -> NONE + rect xoay
  "is10"=@(@(538,209,1023,709,7),@(161,1028,607,1485,-17))
  # is11 vien sao: 2 o trong TRANG 255 (vien sao nhieu mau tach duoc), gan thang -> NONE + rect
  "is11"=@(@(116,251,565,887),@(466,1117,1005,1882))
  # is12 (GRAY mask, o bao trum): Travels + Couple + Gym
  # is12 (GRAY mask): Travels/Couple/Gym - o xam THANG (0 deg, khung mau nghieng chi trang tri)
  "is12"=@(@(73,309,434,777),@(497,103,974,817),@(145,1419,921,1867))
  "is13"=@(@(102,603,741,1361),@(786,601,1052,947),@(786,1015,1052,1360),@(786,1427,1052,1773),@(102,1426,368,1772))
  "is14"=@(@(562,0,1125,556),@(564,556,1125,1120),@(564,1440,1125,2000),@(0,1119,564,1689))
  "is15"=@(@(176,567,949,1060),@(176,1255,949,1749))

  # ---- Summer ----
  # sm01: 3 the trang NGHIENG nhe (~ -7.5 / 0 / -7 deg) - minrect white
  "sm01"=@(@(70,174,1058,644,-7.5),@(71,761,1057,1259),@(69,1377,1058,1847,-7))
  # sm02 underwater: 5 o XAM ~217 -> GRAY2 mask (clip dung hinh khung).
  #   Rect = bbox bao trum gray (mask tu cat). Thu tu: left-upper truoc, top-right SAU
  #   (de o top-right ve de len, khong bi anh o trai tran vao goc chong cheo L-shape).
  #   o center NGHIENG 9.5 deg.
  "sm02"=@(@(149,520,588,957),@(487,366,1024,704),@(149,989,585,1425),@(612,805,978,1261,9.5),@(538,1285,1004,1650))
  # sm03 Summer Time: 3 trang so tay XAM 237 NGHIENG (9 / -10.5 / 14.5 deg) - minrect gray
  "sm03"=@(@(149,367,733,751,9),@(442,863,1025,1248,-10.5),@(404,1450,988,1834,14.5))
  # sm04 Summer Break: 2 o XAM ~222 NGHIENG (-8 / 4.5 deg) - minrect gray2 + NONE
  "sm04"=@(@(454,253,949,1061,-8),@(166,964,659,1783,4.5))
  # sm05 (GRAY mask, o bao trum): IG (thang) + dai film 3 o NGHIENG ~15.5 deg
  "sm05"=@(@(270,330,895,1035),@(710,725,1018,1030,15.5),@(625,1036,933,1340,15.4),@(540,1360,848,1638,15.7))
  "sm06"=@(@(52,269,615,716),@(635,269,1073,883),@(52,737,489,1351),@(510,904,1073,1351),@(52,1372,1073,1820))
  "sm07"=@(@(0,0,562,667),@(562,0,1125,667),@(0,667,562,1334),@(562,667,1125,1334),@(0,1334,562,2000),@(562,1334,1125,2000))
  "sm08"=@(@(21,151,368,620),@(21,640,368,1109),@(21,1130,368,1599),@(388,286,735,755),@(388,776,735,1245),@(388,1265,735,1734),@(757,400,1104,869),@(757,889,1104,1358),@(757,1379,1104,1848))
  # sm09 Best Trip: o TRANG trong khung tan; dai trai 3 o NGHIENG +3 deg, dai phai 2 o -2.5 deg - minrect white
  "sm09"=@(@(133,188,527,583,3),@(110,625,506,1020,3),@(88,1062,483,1457,3),@(608,763,1004,1159,-2.5),@(626,1200,1022,1596,-2.5))

  # ---- Sports ----
  # sp01 (GRAY mask - clip dung pixel xam 237; o XOAY de noi dung anh nghieng theo polaroid)
  "sp01"=@(@(175,329,946,924,3.4),@(147,1098,951,1687,-5.4))
  # sp02 SPECIAL MATCH: 3 o XAM 237 (top/bottom nghieng -8.5/4.5, giua thang) -> GRAY (minrect)
  "sp02"=@(@(144,316,902,846,-8.5),@(270,837,775,1359),@(128,1382,886,1898,4.5))
  # sp03 tennis: 3 o XAM tem rang cua (gan thang) -> GRAY (mask cat theo mep tem)
  "sp03"=@(@(280,98,844,507,-1.5),@(256,917,856,1346,-0.5),@(251,1467,854,1905,-0.5))
  # sp04 badminton: 2 o polaroid ruot TRANG (gan thang) -> NONE + rect
  "sp04"=@(@(191,108,752,846,0.5),@(531,1070,1098,1828,0.5))
  # sp05 Queen Futsal: 2 o TRANG tren so tay nghieng (5 / -3.5) -> NONE + rect
  "sp05"=@(@(199,485,915,972,5),@(244,1101,874,1662,-3.5))
  # sp06 scoreboard: 2 o XAM doc thang trong khung trang -> GRAY
  "sp06"=@(@(76,316,700,878),@(76,920,700,1482))
  # sp07 HALF TIME: 1 o XAM lon polaroid nghieng -7 -> GRAY
  "sp07"=@(,@(143,555,980,1393,-7))
  # sp08 GAME DAY: 3 o TRANG trong dai phim DEN nghieng nhe 4 -> WHITE (giu khung den)
  "sp08"=@(@(575,148,1104,678,4),@(532,730,1062,1260,4),@(489,1312,1019,1843,4))
  # sp09 KIDS FOOTBALL CLUB: 1 o HINH TRON trang -> WHITE (mask clip theo vong tron).
  # Circle that (do theo nguong mask app r,g,b>240): top 137, bottom 895, left 189, right 941 (rong nhat y=520).
  # Rect bao trong ca vong tron (margin ~4px moi phia); nen xanh/trophy/glove/net ngoai circle non-white nen mask tu loai.
  "sp09"=@(,@(183,135,945,898))
  # sp10: 2 o polaroid XAM nghieng (16 / -12.5) -> GRAY
  "sp10"=@(@(149,161,745,836,16),@(324,1109,946,1825,-12.5))

  # ==================================================================================
  # Dot import 2 (asset 11..20 moi) - do bang tools\cells2.ps1 (diff Temp_/Thumb_ giao
  # dai mau phang) + min-area rotated rect. Da doi chieu tung cai voi Thumb_ qua
  # tools\verify_sheet.ps1 de xac nhan DUNG so o.
  # ==================================================================================

  # ---- Birthday 11..20 ----
  # bd11: 1 o lon giua (vong tron) + 4 polaroid TRANG quanh -> WHITE mask clip dung hinh.
  #   THU TU QUAN TRONG: o tron phai dung DAU. Cells ve theo thu tu list, moi o clip theo rect
  #   rieng, nen o ve sau de len o ve truoc o cho rect giao nhau. De o tron thu 3 (theo thu tu
  #   toa do) thi no phu 49% polaroid phia tren -> anh polaroid bi vong tron de mat (da thay tren
  #   may ao). Design la polaroid NAM TREN vong tron, nen tron ve truoc, 4 polaroid ve sau.
  "bd11"=@(@(124,593,930,1358),@(462,323,772,634,8),@(679,466,985,772,-11.2),@(159,1229,468,1538,7.8),@(375,1370,680,1677,-11.2))
  # bd12: luoi 2 cot x 3 hang the "Ace", 3 o anh. Do tay tu luoi toa do: cot phai chay
  #   x 644..1076 (khong phai 1018 nhu detector doan), o tren cao toi y 464.
  "bd12"=@(@(653,2,1012,440),@(121,759,478,1229),@(657,1604,1013,2000))
  # bd13: 4 polaroid NGHIENG MANH, chong nhau. Do tren Temp_ theo dai xam cua O ANH (228-242,
  #   trung tinh) + erode 3 de dut cau noi. Nen giay cung xam nhung VIEN MOUNT TRANG ngan cach
  #   nen 4 o van tach ra sach: fill 0.80-0.97 = hinh chu nhat that, goc dang tin.
  #   CANH BAO: dung nhin overlay roi doan lai. Toi da tung tuong goc -21.5 la "qua nghieng"
  #   va hoan tac ve 0 — do la SAI, the that su nghieng ngan ay.
  "bd13"=@(@(233,546,571,1004,-21.8),@(579,589,914,1117,10.8),@(645,1020,984,1567,5.8),@(281,1037,619,1588,-19.8))
  # bd14/bd15: o XAM SANG ~218 -> ngoai dai GRAY -> GRAY2
  "bd14"=@(@(409,848,840,1171),@(409,1220,840,1538))
  "bd15"=@(@(82,405,491,818),@(82,874,491,1287),@(82,1343,491,1756))
  "bd16"=@(,@(240,605,881,1461,1))
  "bd17"=@(,@(135,780,986,1636))
  "bd18"=@(,@(176,345,945,1538))
  # bd19: 2 the nghieng 10.5, MOI THE bi 1 thanh HONG chia doi -> 4 o (khong phai 2).
  #   Quet doc theo truc cua the: thanh chia nam dung t=0.50 tren ca hai the.
  #   LUU Y khi chia o NGHIENG: khong cat thang toa do duoc, vi moi o xoay quanh TAM RIENG
  #   cua no. Phai lay tam moi nua roi XOAY offset do quanh tam the (10.5 do) de ra tam that.
  "bd19"=@(@(658,307,1042,758,10.5),@(181,476,565,928,10.5),@(571,787,955,1220,10.5),@(93,957,477,1391,10.5))
  # bd20: 1 o HINH TRON rang cua -> rect bao trum + WHITE mask clip theo vien rang cua
  "bd20"=@(,@(210,589,908,1287))

  # ---- Couple 01, 11..20 ----
  # cp01: mo lai (truoc bi an vi o khong khop khung). 2 o TRANG; o TREN nghieng -6.5 do
  #   (so do cu de 0 vi bi phan loai nham la "tron"; ban do tay tu 2026-06 cung ra -7).
  #   O DUOI do tay tu luoi toa do: vung trang chay x 156..924, y 770..1280.
  "cp01"=@(@(235,282,994,821,-6.5),@(156,770,924,1280))
  "cp11"=@(,@(311,638,818,1392))
  "cp12"=@(,@(255,518,892,1497))
  # cp13: luoi 2 cot x 3 hang, o giua-phai la chu -> 5 o anh. Vach phan cach manh nen
  #   phai do o AW=600 (AW=300 lam 5 o dinh thanh 1).
  "cp13"=@(@(13,11,557,664),@(568,15,1110,665),@(13,673,557,1323),@(13,1335,557,1985),@(568,1335,1112,1989))
  "cp14"=@(,@(206,659,920,1373,-15))
  # cp15: o hinh VOM (arch) -> rect bao trum + WHITE mask clip theo vom
  "cp15"=@(,@(202,371,922,1475))
  "cp16"=@(@(135,300,611,762),@(536,1189,1016,1651))
  "cp17"=@(@(450,296,995,850,-4),@(151,1075,708,1649,8.5))
  # cp18: CHI 3 O, cot PHAI. Cot TRAI la artwork Zootopia IN CUNG trong Temp_CP18.png (mo file
  #   nguon ra thay ro: trai co hinh, phai trang tron) — khong phai o anh. Truoc do minh chia
  #   deu 2x6 vi thay Thumb_ co 6 "anh", khong nhan ra 3 anh trai la hinh in san.
  "cp18"=@(@(562,0,1125,667),@(562,667,1125,1334),@(562,1334,1125,2000))
  "cp19"=@(@(131,379,998,968),@(482,1330,984,1829,0.5))
  "cp20"=@(,@(212,423,930,1138,1))

  # ---- Glad season 11..20 ----
  "gs11"=@(,@(177,573,841,1253,2))
  "gs12"=@(@(49,289,551,799),@(461,1017,892,1463))
  "gs13"=@(,@(197,575,901,1313,0.5))
  "gs14"=@(,@(54,740,910,1271,3.5))
  "gs15"=@(@(160,514,569,1058,-10),@(621,1165,1021,1679,14.5))
  "gs16"=@(,@(319,563,832,1092))
  "gs17"=@(,@(300,754,818,1280))
  "gs18"=@(,@(101,540,1024,1659))
  "gs19"=@(,@(87,1226,578,1593,6))
  # gs20: detector con bat them 1 dai doc hep o goc tren-phai (964,51,1099,423) - doi chieu
  #   Thumb_ chi co 1 anh -> da bo dai do.
  "gs20"=@(,@(259,687,855,1306))

  # ---- IG Story 16..17 (is18 Temp_=Thumb_ nen khong dung duoc) ----
  "is16"=@(,@(259,477,862,1081))
  "is17"=@(@(106,153,570,622,-0.5),@(353,686,864,1212,-7),@(576,1308,1124,1900,-0.5))

  # ---- Sports 11..20 ----
  "sp11"=@(,@(98,375,1028,1625))
  # sp12 "80": 3 o TRON ben trai + 1 o PILL lon ben phai. Cac hinh nay khong phai chu nhat nen
  #   rect chi BAO TRUM, WHITE mask lo phan clip theo dung hinh tron/pill.
  "sp12"=@(@(60,664,444,1000),@(500,660,1066,1850),@(64,1090,416,1424),@(50,1530,430,1860))
  # sp13: luoi 3 o XAM, vach phan cach manh -> do o AW=600
  "sp13"=@(@(568,34,1089,995),@(34,1005,555,1966),@(568,1007,1089,1968))
  "sp14"=@(,@(98,330,1024,1197))
  "sp17"=@(@(214,522,896,1250),@(300,1328,821,1887))
  # sp16: 2 vung XAM ben phai dai phim CHEO. Canh trai cua moi vung nghieng ~5 do -> rect bao
  #   trum, GRAY mask clip theo dung hinh. Hai rect roi nhau theo y nen khong tranh hit-test.
  #   (sp15 cung kieu nay nhung 3 dai cheo co y CHONG nhau -> khong bieu dien duoc, da bo.)
  "sp16"=@(@(620,0,1125,980),@(660,980,1125,2000))
  # sp18: luoi 2x2 FULL-BLEED, o duoi-trai la khoi chu xam -> 3 o anh. Khung toan TRANG khong
  #   co vien nen detector khong tach duoc; do tay tu tools\grid_measure.ps1.
  "sp18"=@(@(0,0,563,995),@(563,0,1125,995),@(563,995,1125,2000))
  # sp19: 3 o XAM (217,217,217) xep bac thang, DINH LIEN NHAU thanh 1 component nen
  #   connected-components khong tach duoc. Do bang cach quet bien trai/phai theo TUNG HANG
  #   de tim buoc thang (chinh xac hon han doc luoi bang mat, vd o1 that ra rong toi 408
  #   chu khong phai 296).
  "sp19"=@(@(0,0,408,600),@(302,600,825,1420),@(720,1420,1125,2000))
  "sp20"=@(@(56,281,544,1921),@(566,356,1058,998),@(566,1148,1058,1887))

  # ---- Summer vibe 10, 12..20 (sm11 thieu Thumb_ nen khong do duoc) ----
  # sm10: o hinh VOM -> WHITE mask
  "sm10"=@(,@(79,0,1024,1325))
  "sm12"=@(@(251,30,870,657),@(251,687,870,1313),@(251,1343,870,1970))
  # sm13: luoi 7 o (3 tren + 1 dai giua + 3 duoi)
  "sm13"=@(@(75,424,371,724),@(409,424,709,728),@(750,428,1046,728),@(75,762,1046,1328),@(75,1362,371,1925),@(412,1362,708,1925),@(750,1362,1046,1925))
  # sm14: 4 o hinh BLOB huu co xep doc; AW=600 tach duoc 3 -> GRAY mask clip theo hinh blob
  "sm14"=@(@(214,261,909,855),@(150,784,977,1348),@(231,1293,894,1813))
  "sm15"=@(@(574,184,1058,649),@(8,203,484,664),@(45,852,472,1261),@(585,1107,1102,1602),@(30,1437,491,1857))
  # sm16: luoi 2x2 o TRANG duoi tieu de NEW DROP -> do o AW=600
  "sm16"=@(@(139,669,557,1046),@(566,671,988,1048),@(137,1055,557,1434),@(568,1050,986,1434))
  # sm17: 4 o TRANG (do cu chi ra 2 vi nguong loc qua chat + do phan giai phan tich qua cao
  #   lam mep rang cua noi o vao nen roi bi loai). Do lai o AW=450 -> du 4, da verify overlay.
  #   O3 la polaroid CO MOUNT: so do theo khung bao ca mount, ma voi NONE thi rect chinh la anh
  #   -> phai thu ve vung anh ben trong (day 1508 -> 1455, doc theo luoi toa do).
  "sm17"=@(@(62,62,538,775),@(685,262,922,491,-1.2),@(440,755,1050,1455,2.8),@(62,1498,675,1938))
  # sm18: luoi 2 cot x 3 hang, o GIUA-TRAI la panel chu -> 5 o anh. Truoc chi co 3 vi
  #   Thumb_ de trong 2 o cot phai; "Thumb_ khong dien" KHONG co nghia la khong phai slot.
  "sm18"=@(@(0,0,560,662),@(562,0,1122,662),@(562,665,1122,1332),@(0,1332,560,1998),@(562,1332,1122,1998))
  # sm19 "Sea you": 4 o TRANG bo goc. Truoc chi co 3 — thieu o NHO duoi-trai, vi trong Thumb_
  #   designer dat vao do mot the chu "beach" nen minh tuong la trang tri; mo Temp_ ra thi no
  #   la o trang rong nhu 3 o kia. Giu NONE: goc bo rat nhe, ma WHITE mask se an ca nen troi
  #   (gan trang) nam trong rect.
  "sm19"=@(@(94,773,536,1328),@(626,927,952,1280),@(565,1368,1012,1828),@(142,1400,490,1722))
  "sm20"=@(,@(248,675,870,1568))
}

# Mask clip anh dung theo pixel khung (chinh xac, o chi can bao trum):
#  GRAY = khung xam #ededed (is05/is12/sm05/sp01); WHITE = khung/hinh trang (is06 tim-may, cp06 man hinh+card).
# Con lai NONE (khung trang ro net da khop: cp03 film, gs03 giay...).
# Summer: clip anh dung theo hinh khung de khong tran vien / khong ho mep.
#  sm01 = card TRANG tren nen vang (WHITE); sm03 page xam 237 + sm07 puzzle xam 232 (GRAY 231-243);
#  sm02 (~217) & sm04 (~222) xam SANG hon dai GRAY -> GRAY2 (208-228).
$MASKS = @{ "is05"="GRAY"; "is12"="GRAY"; "sm05"="GRAY"; "sp01"="GRAY"; "is06"="WHITE"; "cp06"="WHITE";
            "sm01"="WHITE"; "sm02"="GRAY2"; "sm03"="GRAY"; "sm04"="GRAY2"; "sm07"="GRAY";
            "is07"="GRAY"; "is08"="GRAY";
            "bd02"="GRAY"; "cp02"="GRAY"; "cp07"="GRAY"; "cp08"="WHITE";
            "gs01"="GRAY"; "gs05"="WHITE"; "gs02"="WHITE";
            "sp03"="GRAY"; "sp07"="GRAY";
            # sm06: 5 o BO GOC (ban kinh ~24 don vi — do pixel: tai y=271 vung o bat dau x=77,
            #   tai y=300 la x=53). Rect vuong tho 4 goc ra nen trang -> can GRAY clip theo hinh.
            "sm06"="GRAY";
            "sp08"="WHITE"; "sp09"="WHITE";

            # ---- Dot import 2 (asset 11..20) ----
            # QUY TAC (rut ra sau khi user bao 12 template lech, 2026-07-28):
            #   O HINH CHU NHAT (ke ca nghieng) -> KHONG dung mask, de NONE.
            #   Mask theo mau la mask TOAN CUC: no chocc thung MOI pixel dung dai mau nam trong
            #   cellRect, ke ca VIEN TRANG (mount) cua polaroid -> anh trum len mount thay vi
            #   nam gon trong do (cp17/cp01/cp12/bd13). Them nua, pixel MEP da bi anti-alias thi
            #   nam ngoai dai -> con nguyen -> tao vanh sang mong dev tren anh (sm13/sm16/sp18).
            #   NONE thi anh fill dung cellRect, clip goi -> khong tran, khong vanh.
            #   Chi dung mask khi o KHONG phai chu nhat, vi luc do rect chi bao trum va mask moi
            #   la thu cat dung hinh: bd11 (tron), bd20 (tron rang cua), cp15+sm10 (vom),
            #   sp12 (tron+pill), sm14 (blob), gs18 (vien luon song), sp16 (canh cheo).
            # CHI 10 template nay con mask, vi o cua chung KHONG phai chu nhat:
            "bd11"="WHITE";   # o lon = vong TRON
            "bd20"="WHITE";   # o TRON rang cua
            "cp14"="WHITE";   # sticker trai tim DE LEN o -> mask chua sticker lai
            "cp15"="WHITE";   # o hinh VOM (pill)
            "sm10"="WHITE";   # o hinh VOM
            "sp12"="WHITE";   # 3 o TRON + 1 o PILL
            "gs18"="WHITE";   # vien LUON SONG
            "sm14"="GRAY";    # 3 o BLOB huu co
            "sp14"="GRAY";    # the BO GOC -> rect vuong an mat 4 goc mount
            "sp16"="GRAY" }   # 2 vung canh CHEO
            # Tat ca template con lai (o chu nhat, ke ca nghieng) -> NONE. Xem quy tac o tren.
# Ghi chu Sports: sp02/sp06/sp10 = o CHU NHAT -> NONE (mask GRAY toan cuc gay bleed cheo khi rect nghieng cham lo xam
#   o ben canh; NONE gioi han anh trong rect tung o). sp03 (tem rang cua) + sp07 giu GRAY (can clip hinh khong-chu-nhat).
