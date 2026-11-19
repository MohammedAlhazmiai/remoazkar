package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * أسماء الله الحسنى التسعة والتسعون مع معانيها — ميزة حصرية.
 * تعمل دون إنترنت، مع بحث فوري بالاسم أو بالمعنى.
 */
data class AllahName(val number: Int, val name: String, val meaning: String)

val allahNames: List<AllahName> = listOf(
    AllahName(1, "الله", "الإله المعبود بحق، المستحق للعبادة وحده"),
    AllahName(2, "الرحمن", "ذو الرحمة الواسعة التي وسعت كل شيء"),
    AllahName(3, "الرحيم", "كثير الرحمة بالمؤمنين، يوصل رحمته إليهم"),
    AllahName(4, "الملك", "المالك المتصرف في ملكه بلا منازع"),
    AllahName(5, "القدوس", "المنزّه عن كل نقص وعيب"),
    AllahName(6, "السلام", "السالم من كل عيب، المؤمّن عباده من الخوف"),
    AllahName(7, "المؤمن", "المصدّق لرسله بالمعجزات، المؤمّن لعباده"),
    AllahName(8, "المهيمن", "الرقيب الحفيظ على خلقه، المطّلع على أحوالهم"),
    AllahName(9, "العزيز", "الغالب الذي لا يُغلب، القوي الذي لا يضعف"),
    AllahName(10, "الجبار", "القهّار، جابر كل كسير، المصلح لأمور خلقه"),
    AllahName(11, "المتكبر", "المتعالي عن صفات الخلق، المتكبر عن كل نقص"),
    AllahName(12, "الخالق", "المبدع للخلق من غير أصل ولا مثال"),
    AllahName(13, "البارئ", "المنشئ للخلق على غير مثال سابق"),
    AllahName(14, "المصور", "مصوّر الخلق كيف شاء، وأعطى كل شيء صورته"),
    AllahName(15, "الغفار", "كثير المغفرة لذنوب عباده مهما تكررت"),
    AllahName(16, "القهار", "القاهر لكل شيء، الذي دانت له الخلائق"),
    AllahName(17, "الوهاب", "كثير العطاء، يهب بلا مقابل ولا سبب"),
    AllahName(18, "الرزاق", "المتكفل بالرزق، يرزق كل مخلوق بقدر حاجته"),
    AllahName(19, "الفتاح", "الذي يفتح أبواب الرحمة والرزق، ويحكم بين عباده"),
    AllahName(20, "العليم", "المحيط بكل شيء علمًا، لا يخفى عليه شيء"),
    AllahName(21, "القابض", "الذي يقبض الرزق والأرواح بحكمته"),
    AllahName(22, "الباسط", "الذي يبسط الرزق ويوسعه على من يشاء"),
    AllahName(23, "الخافض", "الذي يخفض من استحق الخفض من عباده"),
    AllahName(24, "الرافع", "الذي يرفع أولياءه بالطاعة، وأهل العلم درجات"),
    AllahName(25, "المعز", "الذي يعز من يشاء من عباده فلا يذل"),
    AllahName(26, "المذل", "الذي يذل من يشاء من عباده بعدله"),
    AllahName(27, "السميع", "الذي يسمع كل شيء، لا تخفى عليه نجوى"),
    AllahName(28, "البصير", "الذي يرى كل شيء، لا يعزب عن بصره مثقال ذرة"),
    AllahName(29, "الحكم", "الذي يحكم بالحق والعدل، لا رادّ لحكمه"),
    AllahName(30, "العدل", "الذي لا يجور في حكمه، ولا يظلم مثقال ذرة"),
    AllahName(31, "اللطيف", "اللطيف بعباده، الخبير بهم، يوصل الخير بلطف"),
    AllahName(32, "الخبير", "العالم بالبواطن والسرائر، لا يخفى عليه شيء"),
    AllahName(33, "الحليم", "الذي لا يعاجل بالعقوبة، يحلم على عصيان عباده"),
    AllahName(34, "العظيم", "ذو العظمة الكاملة، العظيم في ذاته وصفاته"),
    AllahName(35, "الغفور", "الذي يغفر الذنوب ولو بلغت عنان السماء"),
    AllahName(36, "الشكور", "الذي يثيب على القليل بالكثير، ويشكر اليسير"),
    AllahName(37, "العلي", "العالي فوق كل شيء بذاته وقدره"),
    AllahName(38, "الكبير", "ذو الكبرياء والعظمة، الكبير في كل شيء"),
    AllahName(39, "الحفيظ", "الذي يحفظ خلقه وأعمالهم، لا يعزب عنه شيء"),
    AllahName(40, "المقيت", "الذي يوصل القوت إلى خلقه، الحافظ لهم"),
    AllahName(41, "الحسيب", "الكافي لعباده، المحاسب لهم على أعمالهم"),
    AllahName(42, "الجليل", "ذو الجلال والعظمة، الجليل في صفاته"),
    AllahName(43, "الكريم", "كثير الخير والجود، يعطي بلا سؤال"),
    AllahName(44, "الرقيب", "المطّلع على كل شيء، لا يغيب عنه حال"),
    AllahName(45, "المجيب", "الذي يجيب دعاء الداعين، ويقضي حوائج السائلين"),
    AllahName(46, "الواسع", "الذي وسع رحمته وعلمه كل شيء"),
    AllahName(47, "الحكيم", "الذي يضع الأمور في مواضعها، أفعاله كلها حكمة"),
    AllahName(48, "الودود", "المحب لأوليائه، المحبوب لعباده الصالحين"),
    AllahName(49, "المجيد", "ذو المجد والشرف والكرم الواسع"),
    AllahName(50, "الباعث", "الذي يبعث الخلق يوم القيامة للحساب"),
    AllahName(51, "الشهيد", "الحاضر الذي لا يغيب، الشاهد على كل شيء"),
    AllahName(52, "الحق", "الثابت الذي لا يزول، وجوده حق ووعده حق"),
    AllahName(53, "الوكيل", "القائم بمصالح عباده، الكافي من توكل عليه"),
    AllahName(54, "القوي", "ذو القوة الكاملة التي لا يعتريها ضعف"),
    AllahName(55, "المتين", "الشديد القوي الذي لا تلحقه مشقة"),
    AllahName(56, "الولي", "الناصر لعباده المؤمنين، المتولي أمورهم"),
    AllahName(57, "الحميد", "المحمود على كل حال، المستحق لكل حمد"),
    AllahName(58, "المحصي", "الذي أحصى كل شيء عددًا، لا يفوته شيء"),
    AllahName(59, "المبدئ", "الذي بدأ الخلق وأنشأه ابتداءً"),
    AllahName(60, "المعيد", "الذي يعيد الخلق بعد الفناء للبعث"),
    AllahName(61, "المحيي", "الذي يحيي الموتى، ويحيي الأرض بعد موتها"),
    AllahName(62, "المميت", "الذي يقضي بالموت على من أراد من خلقه"),
    AllahName(63, "الحي", "الحيّ الذي لا يموت، حياته كاملة أزلية"),
    AllahName(64, "القيوم", "القائم بنفسه، المقيم لغيره، المدبر لأمر خلقه"),
    AllahName(65, "الواجد", "الذي لا يعوزه شيء، واجد كل ما يريد"),
    AllahName(66, "الماجد", "ذو الشرف والكرم والعطاء الجزيل"),
    AllahName(67, "الواحد", "المتفرد بالذات والصفات، لا شريك له"),
    AllahName(68, "الأحد", "الفرد الذي لا نظير له ولا شريك"),
    AllahName(69, "الصمد", "السيد المقصود في الحوائج، الذي لا جوف له"),
    AllahName(70, "القادر", "القادر على كل شيء، لا يعجزه أمر"),
    AllahName(71, "المقتدر", "التام القدرة، الذي لا يفوته مطلوب"),
    AllahName(72, "المقدم", "الذي يقدّم من يشاء من خلقه بحكمته"),
    AllahName(73, "المؤخر", "الذي يؤخر ما يشاء من أمره وخلقه"),
    AllahName(74, "الأول", "الذي ليس قبله شيء، الأول بلا بداية"),
    AllahName(75, "الآخر", "الذي ليس بعده شيء، الآخر بلا نهاية"),
    AllahName(76, "الظاهر", "العالي فوق كل شيء، الظاهر بأدلته"),
    AllahName(77, "الباطن", "القريب من كل شيء، العالم بالخفيات"),
    AllahName(78, "الوالي", "المتصرف بالأمور، المالك لتدبير خلقه"),
    AllahName(79, "المتعالي", "المرتفع عن كل نقص، المتعالي عن ظنون الخلق"),
    AllahName(80, "البر", "المحسن إلى عباده، كثير البر والإحسان"),
    AllahName(81, "التواب", "الذي يقبل توبة عباده مهما تكررت"),
    AllahName(82, "المنتقم", "الذي ينتقم من الظالمين بعد الإمهال"),
    AllahName(83, "العفو", "الذي يعفو عن الذنوب، ويمحو آثارها"),
    AllahName(84, "الرؤوف", "كثير الرأفة والرحمة بعباده"),
    AllahName(85, "مالك الملك", "المتصرف في ملكه كيف يشاء، يؤتي الملك من يشاء"),
    AllahName(86, "ذو الجلال والإكرام", "ذو العظمة والجلال، المستحق للإكرام والتعظيم"),
    AllahName(87, "المقسط", "الذي يحكم بالقسط والعدل بين عباده"),
    AllahName(88, "الجامع", "الذي يجمع الخلائق ليوم الحساب، ويجمع المتفرقات"),
    AllahName(89, "الغني", "الذي لا يحتاج إلى أحد، وكل شيء محتاج إليه"),
    AllahName(90, "المغني", "الذي يغني من يشاء من خلقه بفضله"),
    AllahName(91, "المانع", "الذي يمنع ما يشاء بحكمته، ويحفظ عباده"),
    AllahName(92, "الضار", "الذي يضر من يشاء بحكمته وعدله"),
    AllahName(93, "النافع", "الذي ينفع من يشاء بفضله ورحمته"),
    AllahName(94, "النور", "الذي نوره يهدي، وبنوره استنارت السماوات"),
    AllahName(95, "الهادي", "الذي يهدي عباده إلى الحق والرشاد"),
    AllahName(96, "البديع", "الذي خلق الخلق على غير مثال سابق"),
    AllahName(97, "الباقي", "الذي لا يفنى ولا يزول، دائم الوجود"),
    AllahName(98, "الوارث", "الذي يرث كل شيء بعد فناء خلقه"),
    AllahName(99, "الرشيد", "الذي أفعاله كلها رشد وحكمة وصواب")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NamesOfAllahScreen(onBackClick: () -> Unit) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(query) {
        if (query.isBlank()) allahNames
        else allahNames.filter {
            it.name.contains(query.trim()) || it.meaning.contains(query.trim())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("أسماء الله الحسنى", fontWeight = FontWeight.Bold) },
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
                placeholder = { Text("ابحث باسم أو بمعنى…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Text(
                text = "${filtered.size} اسمًا — «ولله الأسماء الحسنى فادعوه بها»",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "${item.number}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.name,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    item.meaning,
                                    fontSize = 13.sp,
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
