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
  # cp01 Anniversary: 2 o TRANG nghieng (top -6.5) - NONE + minrect white
  "cp01"=@(@(179,281,983,701,-7),@(151,674,910,1290,-3.4))
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
  "gs08"=@(@(93,451,545,1040,-6),@(612,920,1108,1500,6))
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
  "is08"=@(@(162,169,708,716,13.5),@(514,730,1060,1276,-2),@(106,1229,652,1776,-13))
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

  # ---- Sports (sp01 only image; vector-masked white polaroids) ----
  # sp01 (GRAY mask - clip dung pixel xam 237; o XOAY de noi dung anh nghieng theo polaroid)
  "sp01"=@(@(175,329,946,924,3.4),@(147,1098,951,1687,-5.4))
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
            "gs01"="GRAY"; "gs05"="WHITE"; "gs02"="WHITE" }
