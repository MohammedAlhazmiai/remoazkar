package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * أدعية الأنبياء عليهم السلام كما وردت في القرآن الكريم — ميزة حصرية.
 * كل دعاء مرفق بنبيه وسنده من الآية، مع شرح موجز لموضع الدعاء.
 */
data class ProphetDua(
    val prophet: String,
    val dua: String,
    val reference: String,
    val context: String
)

val prophetDuas: List<ProphetDua> = listOf(
    ProphetDua(
        "آدم عليه السلام",
        "رَبَّنَا ظَلَمْنَا أَنفُسَنَا وَإِن لَّمْ تَغْفِرْ لَنَا وَتَرْحَمْنَا لَنَكُونَنَّ مِنَ الْخَاسِرِينَ",
        "الأعراف: 23",
        "دعاء التوبة الأولى بعد المعصية، وأصل كل استغفار."
    ),
    ProphetDua(
        "نوح عليه السلام",
        "رَّبِّ اغْفِرْ لِي وَلِوَالِدَيَّ وَلِمَن دَخَلَ بَيْتِيَ مُؤْمِنًا وَلِلْمُؤْمِنِينَ وَالْمُؤْمِنَاتِ",
        "نوح: 28",
        "دعاء يجمع الاستغفار للنفس والوالدين والأهل والمؤمنين."
    ),
    ProphetDua(
        "نوح عليه السلام",
        "رَّبِّ انصُرْنِي عَلَى الْقَوْمِ الْمُكَذِّبِينَ",
        "المؤمنون: 26",
        "دعاء المستضعف حين تُكذّب دعوته."
    ),
    ProphetDua(
        "إبراهيم عليه السلام",
        "رَبَّنَا تَقَبَّلْ مِنَّا إِنَّكَ أَنتَ السَّمِيعُ الْعَلِيمُ",
        "البقرة: 127",
        "دعاء عند بناء الكعبة، يُقال عند إتمام عمل صالح."
    ),
    ProphetDua(
        "إبراهيم عليه السلام",
        "رَبِّ اجْعَلْنِي مُقِيمَ الصَّلَاةِ وَمِن ذُرِّيَّتِي رَبَّنَا وَتَقَبَّلْ دُعَاءِ",
        "إبراهيم: 40",
        "دعاء الثبات على الصلاة لنفسك ولذريتك."
    ),
    ProphetDua(
        "إبراهيم عليه السلام",
        "رَبَّنَا اغْفِرْ لِي وَلِوَالِدَيَّ وَلِلْمُؤْمِنِينَ يَوْمَ يَقُومُ الْحِسَابُ",
        "إبراهيم: 41",
        "دعاء يوم الحساب، يجمع الأهل والمؤمنين."
    ),
    ProphetDua(
        "إسماعيل عليه السلام",
        "رَبِّ اجْعَلْنِي مُقِيمَ الصَّلَاةِ",
        "مريم: 55 (إشارة)",
        "من صفات إسماعيل: كان يأمر أهله بالصلاة والزكاة."
    ),
    ProphetDua(
        "يعقوب عليه السلام",
        "إِنَّمَا أَشْكُو بَثِّي وَحُزْنِي إِلَى اللَّهِ",
        "يوسف: 86",
        "دعاء الحزين الذي لا يجد ملجأً إلا الله."
    ),
    ProphetDua(
        "يوسف عليه السلام",
        "رَبِّ قَدْ آتَيْتَنِي مِنَ الْمُلْكِ وَعَلَّمْتَنِي مِن تَأْوِيلِ الْأَحَادِيثِ فَاطِرَ السَّمَاوَاتِ وَالْأَرْضِ أَنتَ وَلِيِّي فِي الدُّنْيَا وَالْآخِرَةِ تَوَفَّنِي مُسْلِمًا وَأَلْحِقْنِي بِالصَّالِحِينَ",
        "يوسف: 101",
        "دعاء من بلغ المنزلة أن يختم له بالإسلام."
    ),
    ProphetDua(
        "أيوب عليه السلام",
        "رَبِّ إِنِّي مَسَّنِيَ الضُّرُّ وَأَنتَ أَرْحَمُ الرَّاحِمِينَ",
        "الأنبياء: 83",
        "دعاء المريض بأدب، بلا شكوى ولا تسخط."
    ),
    ProphetDua(
        "موسى عليه السلام",
        "رَبِّ اشْرَحْ لِي صَدْرِي وَيَسِّرْ لِي أَمْرِي وَاحْلُلْ عُقْدَةً مِّن لِّسَانِي",
        "طه: 25-27",
        "دعاء من يواجه مهمة صعبة أو خطابًا ثقيلًا."
    ),
    ProphetDua(
        "موسى عليه السلام",
        "رَبِّ إِنِّي ظَلَمْتُ نَفْسِي فَاغْفِرْ لِي",
        "القصص: 16",
        "اعتراف بالذنب مع طلب المغفرة."
    ),
    ProphetDua(
        "موسى عليه السلام",
        "رَبِّ بِمَا أَنْعَمْتَ عَلَيَّ فَلَنْ أَكُونَ ظَهِيرًا لِّلْمُجْرِمِينَ",
        "القصص: 17",
        "عهد بالامتناع عن نصرة الظالم مقابل النعمة."
    ),
    ProphetDua(
        "داود عليه السلام",
        "رَبِّ اغْفِرْ لِي وَهَبْ لِي مُلْكًا لَّا يَنبَغِي لِأَحَدٍ مِّن بَعْدِي",
        "ص: 35",
        "دعاء الجمع بين الاستغفار وطلب الفضل."
    ),
    ProphetDua(
        "سليمان عليه السلام",
        "رَبِّ أَوْزِعْنِي أَنْ أَشْكُرَ نِعْمَتَكَ الَّتِي أَنْعَمْتَ عَلَيَّ وَعَلَى وَالِدَيَّ وَأَنْ أَعْمَلَ صَالِحًا تَرْضَاهُ",
        "النمل: 19",
        "دعاء الشكر عند رؤية النعمة، وأفضل دعاء للوالدين."
    ),
    ProphetDua(
        "يونس عليه السلام",
        "لَّا إِلَهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ",
        "الأنبياء: 87",
        "دعاء الفرج في الظلمات، ما دعا به مكروب إلا فرّج الله عنه."
    ),
    ProphetDua(
        "زكريا عليه السلام",
        "رَبِّ هَبْ لِي مِن لَّدُنكَ ذُرِّيَّةً طَيِّبَةً إِنَّكَ سَمِيعُ الدُّعَاءِ",
        "آل عمران: 38",
        "دعاء الذرية الصالحة عند تأخرها."
    ),
    ProphetDua(
        "زكريا عليه السلام",
        "رَبِّ لَا تَذَرْنِي فَرْدًا وَأَنتَ خَيْرُ الْوَارِثِينَ",
        "الأنبياء: 89",
        "دعاء من لم يُرزق الولد وخشي الوحدة."
    ),
    ProphetDua(
        "يحيى عليه السلام",
        "رَبِّ آتِنَا مِن لَّدُنكَ رَحْمَةً وَهَيِّئْ لَنَا مِنْ أَمْرِنَا رَشَدًا",
        "الكهف: 10",
        "دعاء الفتية حين لجأوا إلى الكهف، لكل من ضاق به الأمر."
    ),
    ProphetDua(
        "عيسى عليه السلام",
        "اللَّهُمَّ رَبَّنَا أَنزِلْ عَلَيْنَا مَائِدَةً مِّنَ السَّمَاءِ تَكُونُ لَنَا عِيدًا لِّأَوَّلِنَا وَآخِرِنَا وَآيَةً مِّنكَ",
        "المائدة: 114",
        "دعاء طلب البركة والرزق الجماعي."
    ),
    ProphetDua(
        "محمد ﷺ",
        "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
        "البقرة: 201",
        "أجمع دعاء للخير في الدارين."
    ),
    ProphetDua(
        "محمد ﷺ",
        "رَبِّ زِدْنِي عِلْمًا",
        "طه: 114",
        "أقصر دعاء لطلب العلم، وهو أمر إلهي لنبيه."
    ),
    ProphetDua(
        "محمد ﷺ",
        "رَّبِّ أَعُوذُ بِكَ مِنْ هَمَزَاتِ الشَّيَاطِينِ وَأَعُوذُ بِكَ رَبِّ أَن يَحْضُرُونِ",
        "المؤمنون: 97-98",
        "دعاء الحماية من وساوس الشيطان وحضوره."
    ),
    ProphetDua(
        "محمد ﷺ",
        "رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِن لَّدُنكَ رَحْمَةً",
        "آل عمران: 8",
        "دعاء الثبات على الهداية، يُكثر منه أهل الإيمان."
    ),
    ProphetDua(
        "محمد ﷺ",
        "رَبِّ اغْفِرْ وَارْحَمْ وَأَنتَ خَيْرُ الرَّاحِمِينَ",
        "المؤمنون: 118",
        "خاتمة سورة المؤمنون، دعاء جامع موجز."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProphetDuasScreen(onBackClick: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var expandedIndex by remember { mutableIntStateOf(-1) }

    val filtered = remember(query) {
        if (query.isBlank()) prophetDuas
        else prophetDuas.filter {
            it.prophet.contains(query.trim()) ||
                it.dua.contains(query.trim()) ||
                it.context.contains(query.trim())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("أدعية الأنبياء", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("ابحث في الأدعية أو أسماء الأنبياء…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered) { item ->
                    val index = prophetDuas.indexOf(item)
                    val isExpanded = expandedIndex == index

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedIndex = if (isExpanded) -1 else index },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isExpanded)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        item.prophet,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "﴿ ${item.dua} ﴾",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Start,
                                lineHeight = 30.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = item.reference,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )

                            AnimatedVisibility(visible = isExpanded) {
                                Column {
                                    Spacer(Modifier.height(10.dp))
                                    HorizontalDivider()
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = item.context,
                                        fontSize = 13.sp,
                                        lineHeight = 22.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
