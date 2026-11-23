package com.ahmedramadan.visuals

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

private const val SUPABASE_URL = "https://owlglaqltdniywcprzab.supabase.co"
private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im93bGdsYXFsdGRuaXl3Y3ByemFiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTUzNTY2NDgsImV4cCI6MjExMDkzMjY0OH0._Y7iR4YU7z10-TOkRHSYC_466_5pJ3mFSYjRqGgbWGo"

private val BgDark = Color(0xFF0B0B0D)
private val SurfaceDark = Color(0xFF131317)
private val SurfaceCard = Color(0xFF1B1B21)
private val AccentGold = Color(0xFFC8A46E)
private val TextIvory = Color(0xFFF3EFE6)
private val MutedGray = Color(0xFF9A958C)
private val DangerRed = Color(0xFFE06C75)

data class CollectionItem(
    val id: String,
    val slug: String,
    val titleEn: String,
    val titleAr: String,
    val isPublished: Boolean
)

data class WorkItem(
    val id: String,
    val slug: String,
    val titleEn: String,
    val titleAr: String,
    val origin: String,
    val collectionId: String,
    val thumbUrl: String,
    val fullUrl: String,
    val isPublished: Boolean,
    val isFeatured: Boolean,
    val qualityScore: Int
)

data class CommentItem(
    val id: String,
    val body: String,
    val isHidden: Boolean,
    val author: String,
    val workTitle: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = BgDark,
                    surface = SurfaceDark,
                    primary = AccentGold,
                    onPrimary = BgDark,
                    onBackground = TextIvory,
                    onSurface = TextIvory
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = BgDark) {
                    VisualsAdminApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualsAdminApp() {
    val client = remember { OkHttpClient() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var accessToken by remember { mutableStateOf<String?>(null) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var collections by remember { mutableStateOf<List<CollectionItem>>(emptyList()) }
    var works by remember { mutableStateOf<List<WorkItem>>(emptyList()) }
    var comments by remember { mutableStateOf<List<CommentItem>>(emptyList()) }

    // Navigation state: 0=Home, 1=Works, 2=Upload, 3=Collections, 4=Comments
    var activeTab by remember { mutableStateOf(0) }
    var editingWork by remember { mutableStateOf<WorkItem?>(null) }

    // Filters
    var workSearch by remember { mutableStateOf("") }
    var workStatusFilter by remember { mutableStateOf("all") } // all, published, archived, featured

    // Upload state
    var selectedColIndex by remember { mutableStateOf(0) }
    var selectedOrigin by remember { mutableStateOf("camera") }
    var uploadTitleAr by remember { mutableStateOf("") }
    var uploadTitleEn by remember { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf("") }

    // New collection state
    var newColSlug by remember { mutableStateOf("") }
    var newColEn by remember { mutableStateOf("") }
    var newColAr by remember { mutableStateOf("") }

    // Update checker state
    var updateVersion by remember { mutableStateOf<String?>(null) }
    var updateApkUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVer = pkgInfo.versionName ?: "1.0.0"
                val req = Request.Builder()
                    .url("https://api.github.com/repos/ahmedissamramadan/visuals-android/releases/latest")
                    .addHeader("Accept", "application/vnd.github+json")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val json = JSONObject(resp.body?.string() ?: "{}")
                        val tag = json.optString("tag_name", "").removePrefix("v")
                        val assets = json.optJSONArray("assets")
                        var dlUrl = json.optString("html_url", "https://github.com/ahmedissamramadan/visuals-android/releases/latest")
                        if (assets != null && assets.length() > 0) {
                            dlUrl = assets.getJSONObject(0).optString("browser_download_url", dlUrl)
                        }
                        if (tag.isNotBlank() && tag != currentVer) {
                            updateVersion = tag
                            updateApkUrl = dlUrl
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun loadDashboardData(token: String) {
        withContext(Dispatchers.IO) {
            val colReq = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/collections?select=id,slug,title_en,title_ar,is_published&order=sort_order")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $token")
                .build()
            client.newCall(colReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val arr = JSONArray(resp.body?.string() ?: "[]")
                    val list = mutableListOf<CollectionItem>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        list.add(
                            CollectionItem(
                                o.getString("id"),
                                o.getString("slug"),
                                o.getString("title_en"),
                                o.getString("title_ar"),
                                o.optBoolean("is_published", true)
                            )
                        )
                    }
                    collections = list
                }
            }

            val workReq = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/works?select=id,slug,title_en,title_ar,origin,collection_id,image_urls,is_published,is_featured,quality_score&order=created_at.desc&limit=250")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $token")
                .build()
            client.newCall(workReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val arr = JSONArray(resp.body?.string() ?: "[]")
                    val list = mutableListOf<WorkItem>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val imgs = o.optJSONObject("image_urls")
                        val thumb = imgs?.optString("480")?.ifBlank { imgs.optString("1200", "") } ?: ""
                        val full = imgs?.optString("1200")?.ifBlank { thumb } ?: thumb
                        list.add(
                            WorkItem(
                                o.getString("id"),
                                o.getString("slug"),
                                o.optString("title_en", ""),
                                o.optString("title_ar", ""),
                                o.getString("origin"),
                                o.getString("collection_id"),
                                thumb,
                                full,
                                o.optBoolean("is_published", true),
                                o.optBoolean("is_featured", false),
                                o.optInt("quality_score", 85)
                            )
                        )
                    }
                    works = list
                }
            }

            val comReq = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/comments?select=id,body,is_hidden,profiles(display_name),works(title_en)&order=created_at.desc&limit=50")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $token")
                .build()
            client.newCall(comReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val arr = JSONArray(resp.body?.string() ?: "[]")
                    val list = mutableListOf<CommentItem>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val author = o.optJSONObject("profiles")?.optString("display_name") ?: "Visitor"
                        val workTitle = o.optJSONObject("works")?.optString("title_en") ?: ""
                        list.add(CommentItem(o.getString("id"), o.getString("body"), o.getBoolean("is_hidden"), author, workTitle))
                    }
                    comments = list
                }
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        val token = accessToken ?: return@rememberLauncherForActivityResult
        if (uris.isEmpty() || collections.isEmpty()) return@rememberLauncherForActivityResult
        val col = collections[selectedColIndex.coerceIn(0, collections.lastIndex)]

        scope.launch {
            isLoading = true
            withContext(Dispatchers.IO) {
                for ((idx, uri) in uris.withIndex()) {
                    statusMsg = "جاري رفع ${idx + 1} من ${uris.size}..."
                    val input = context.contentResolver.openInputStream(uri) ?: continue
                    val orig = BitmapFactory.decodeStream(input)
                    input.close()
                    if (orig == null) continue

                    val slug = "work-${System.currentTimeMillis()}-${idx + 1}"
                    val imageUrls = JSONObject()

                    for (w in listOf(480, 1200, 2400)) {
                        val scale = if (orig.width > w) w.toFloat() / orig.width else 1f
                        val scaled = Bitmap.createScaledBitmap(orig, (orig.width * scale).toInt(), (orig.height * scale).toInt(), true)
                        val out = ByteArrayOutputStream()
                        val fmt = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
                        scaled.compress(fmt, 82, out)
                        val bytes = out.toByteArray()

                        val storagePath = "works/$slug/$w.webp"
                        val upReq = Request.Builder()
                            .url("$SUPABASE_URL/storage/v1/object/visuals/$storagePath")
                            .post(bytes.toRequestBody("image/webp".toMediaType()))
                            .addHeader("apikey", SUPABASE_ANON_KEY)
                            .addHeader("Authorization", "Bearer $token")
                            .addHeader("x-upsert", "true")
                            .build()
                        client.newCall(upReq).execute().close()
                        imageUrls.put(w.toString(), "$SUPABASE_URL/storage/v1/object/public/visuals/$storagePath")
                    }

                    val blurBmp = Bitmap.createScaledBitmap(orig, 24, (24f * orig.height / orig.width).toInt().coerceAtLeast(1), true)
                    val blurOut = ByteArrayOutputStream()
                    val fmt = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
                    blurBmp.compress(fmt, 40, blurOut)
                    val blurUrl = "data:image/webp;base64," + Base64.encodeToString(blurOut.toByteArray(), Base64.NO_WRAP)

                    val tEn = uploadTitleEn.ifBlank { uploadTitleAr.ifBlank { "Work ${idx + 1}" } }
                    val tAr = uploadTitleAr.ifBlank { tEn }
                    val body = JSONObject().apply {
                        put("slug", slug)
                        put("collection_id", col.id)
                        put("origin", selectedOrigin)
                        put("media_type", "image")
                        put("title_en", tEn)
                        put("title_ar", tAr)
                        put("image_urls", imageUrls)
                        put("blur_data_url", blurUrl)
                        put("width", orig.width)
                        put("height", orig.height)
                    }
                    val insReq = Request.Builder()
                        .url("$SUPABASE_URL/rest/v1/works")
                        .post(body.toString().toRequestBody("application/json".toMediaType()))
                        .addHeader("apikey", SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                    client.newCall(insReq).execute().close()
                }
                loadDashboardData(token)
                statusMsg = "تم رفع ${uris.size} عمل بنجاح إلى ${col.titleAr}!"
            }
            isLoading = false
        }
    }

    // Handle system Back button inside app
    BackHandler(enabled = editingWork != null || activeTab != 0) {
        if (editingWork != null) editingWork = null
        else activeTab = 0
    }

    if (accessToken == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (updateVersion != null && updateApkUrl != null) {
                Button(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateApkUrl))) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("تحديث جديد متاح (v$updateVersion) - اضغط للتحميل")
                }
            }
            Text("Visuals Admin", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AccentGold)
            Spacer(Modifier.height(6.dp))
            Text("لوحة التحكم الشاملة", fontSize = 15.sp, color = MutedGray)
            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("اسم المستخدم (admin / boda890) أو الإيميل") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("الرمز السري") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            if (loginError != null) {
                Spacer(Modifier.height(8.dp))
                Text(loginError!!, color = DangerRed, fontSize = 13.sp)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        loginError = null
                        val email = when {
                            username.trim().equals("boda890", ignoreCase = true) -> "bodykhaled5000@gmail.com"
                            username.trim().equals("admin", ignoreCase = true) -> "ahmedissam.personal@gmail.com"
                            else -> username.trim()
                        }
                        val payload = JSONObject().put("email", email).put("password", password).toString()
                        withContext(Dispatchers.IO) {
                            val req = Request.Builder()
                                .url("$SUPABASE_URL/auth/v1/token?grant_type=password")
                                .post(payload.toRequestBody("application/json".toMediaType()))
                                .addHeader("apikey", SUPABASE_ANON_KEY)
                                .build()
                            client.newCall(req).execute().use { resp ->
                                val txt = resp.body?.string() ?: "{}"
                                val json = JSONObject(txt)
                                if (resp.isSuccessful && json.has("access_token")) {
                                    val tok = json.getString("access_token")
                                    accessToken = tok
                                    loadDashboardData(tok)
                                } else {
                                    loginError = json.optString("error_description", "بيانات الدخول غير صحيحة")
                                }
                            }
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isLoading) "جاري الدخول..." else "تسجيل الدخول")
            }
        }
        return
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
                navigationIcon = {
                    if (editingWork != null) {
                        TextButton(onClick = { editingWork = null }) {
                            Text("← رجوع", color = AccentGold, fontWeight = FontWeight.Bold)
                        }
                    } else if (activeTab != 0) {
                        TextButton(onClick = { activeTab = 0 }) {
                            Text("🏠 الرئيسية", color = AccentGold, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                title = {
                    Text(
                        when {
                            editingWork != null -> "تعديل العمل"
                            activeTab == 0 -> "الرئيسية"
                            activeTab == 1 -> "إدارة الأعمال"
                            activeTab == 2 -> "رفع جديد"
                            activeTab == 3 -> "المجموعات"
                            else -> "التعليقات"
                        },
                        color = TextIvory,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                actions = {
                    TextButton(onClick = {
                        val tok = accessToken ?: return@TextButton
                        scope.launch { loadDashboardData(tok) }
                    }) { Text("🔄", fontSize = 18.sp) }
                    TextButton(onClick = { accessToken = null; editingWork = null; activeTab = 0 }) {
                        Text("خروج", color = MutedGray, fontSize = 13.sp)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = SurfaceDark) {
                val navItems = listOf("🏠 الرئيسية", "🖼 الأعمال", "⬆️ رفع", "📁 المجموعات", "💬 تعليقات")
                navItems.forEachIndexed { idx, label ->
                    NavigationBarItem(
                        selected = editingWork == null && activeTab == idx,
                        onClick = { editingWork = null; activeTab = idx },
                        icon = { Text(label.substringBefore(" "), fontSize = 18.sp) },
                        label = { Text(label.substringAfter(" "), fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            if (updateVersion != null && updateApkUrl != null) {
                Button(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateApkUrl))) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("تحديث جديد متاح (v$updateVersion) - اضغط للتحميل")
                }
            }

            val currentEdit = editingWork
            if (currentEdit != null) {
                // Full Work Editor Screen
                var editAr by remember(currentEdit.id) { mutableStateOf(currentEdit.titleAr) }
                var editEn by remember(currentEdit.id) { mutableStateOf(currentEdit.titleEn) }
                var editOrigin by remember(currentEdit.id) { mutableStateOf(currentEdit.origin) }
                var editColId by remember(currentEdit.id) { mutableStateOf(currentEdit.collectionId) }
                var editPub by remember(currentEdit.id) { mutableStateOf(currentEdit.isPublished) }
                var editFeat by remember(currentEdit.id) { mutableStateOf(currentEdit.isFeatured) }

                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = currentEdit.fullUrl,
                        contentDescription = editAr,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceDark)
                    )
                    OutlinedTextField(value = editAr, onValueChange = { editAr = it }, label = { Text("العنوان بالعربي") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editEn, onValueChange = { editEn = it }, label = { Text("العنوان بالإنجليزي") }, modifier = Modifier.fillMaxWidth())

                    Text("الأصل (Origin):", color = MutedGray, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("camera" to "📷 كاميرا", "ai" to "✨ ذكاء اصطناعي", "design" to "🎨 تصميم").forEach { (orig, lbl) ->
                            FilterChip(selected = editOrigin == orig, onClick = { editOrigin = orig }, label = { Text(lbl) })
                        }
                    }

                    Text("المجموعة:", color = MutedGray, fontSize = 13.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        collections.forEach { col ->
                            FilterChip(selected = editColId == col.id, onClick = { editColId = col.id }, label = { Text(col.titleAr) })
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(selected = editPub, onClick = { editPub = !editPub }, label = { Text(if (editPub) "منشور ✓" else "مؤرشف") })
                        FilterChip(selected = editFeat, onClick = { editFeat = !editFeat }, label = { Text(if (editFeat) "مميز ★" else "عادي ☆") })
                    }

                    Button(
                        onClick = {
                            val token = accessToken ?: return@Button
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val body = JSONObject().apply {
                                        put("title_ar", editAr.trim())
                                        put("title_en", editEn.trim())
                                        put("origin", editOrigin)
                                        put("collection_id", editColId)
                                        put("is_published", editPub)
                                        put("is_featured", editFeat)
                                    }
                                    val req = Request.Builder()
                                        .url("$SUPABASE_URL/rest/v1/works?id=eq.${currentEdit.id}")
                                        .patch(body.toString().toRequestBody("application/json".toMediaType()))
                                        .addHeader("apikey", SUPABASE_ANON_KEY)
                                        .addHeader("Authorization", "Bearer $token")
                                        .build()
                                    client.newCall(req).execute().close()
                                    loadDashboardData(token)
                                }
                                editingWork = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("حفظ التعديلات") }

                    OutlinedButton(
                        onClick = {
                            val token = accessToken ?: return@OutlinedButton
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val req = Request.Builder()
                                        .url("$SUPABASE_URL/rest/v1/works?id=eq.${currentEdit.id}")
                                        .delete()
                                        .addHeader("apikey", SUPABASE_ANON_KEY)
                                        .addHeader("Authorization", "Bearer $token")
                                        .build()
                                    client.newCall(req).execute().close()
                                    loadDashboardData(token)
                                }
                                editingWork = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                    ) { Text("حذف العمل نهائياً") }
                }
            } else {
                when (activeTab) {
                    0 -> {
                        // Home Overview
                        val pubWorks = works.count { it.isPublished }
                        val archWorks = works.count { !it.isPublished }
                        val featWorks = works.count { it.isFeatured }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatCard("المجموعات", collections.size.toString(), Modifier.weight(1f))
                                StatCard("منشور", pubWorks.toString(), Modifier.weight(1f))
                                StatCard("مؤرشف", archWorks.toString(), Modifier.weight(1f))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatCard("مختارات ★", featWorks.toString(), Modifier.weight(1f))
                                StatCard("التعليقات", comments.size.toString(), Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("اختصارات سريعة", color = MutedGray, fontSize = 13.sp)
                            Button(onClick = { activeTab = 2 }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                                Text("⬆️ رفع صور جديدة من المعرض")
                            }
                            OutlinedButton(onClick = { activeTab = 1 }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                                Text("🖼 استعراض وتعديل الأعمال")
                            }
                            OutlinedButton(onClick = { activeTab = 3 }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                                Text("📁 إدارة المجموعات")
                            }
                            OutlinedButton(
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ahmedissamramadan.github.io/visuals/"))) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("🌐 فتح الموقع المباشر")
                            }
                        }
                    }
                    1 -> {
                        // Works Management
                        val filtered = works.filter { w ->
                            val matchStatus = when (workStatusFilter) {
                                "published" -> w.isPublished
                                "archived" -> !w.isPublished
                                "featured" -> w.isFeatured
                                else -> true
                            }
                            val q = workSearch.trim().lowercase()
                            val matchQ = q.isEmpty() || w.titleAr.lowercase().contains(q) || w.titleEn.lowercase().contains(q)
                            matchStatus && matchQ
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = workSearch,
                                onValueChange = { workSearch = it },
                                label = { Text("بحث في الأعمال...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("all" to "الكل", "published" to "منشور", "archived" to "مؤرشف", "featured" to "مميز ★").forEach { (k, lbl) ->
                                    FilterChip(selected = workStatusFilter == k, onClick = { workStatusFilter = k }, label = { Text(lbl) })
                                }
                            }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(filtered) { w ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SurfaceCard, RoundedCornerShape(10.dp))
                                            .clickable { editingWork = w }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = w.thumbUrl,
                                            contentDescription = w.titleAr,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceDark)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(w.titleAr.ifBlank { w.titleEn }, color = if (w.isPublished) TextIvory else MutedGray, fontWeight = FontWeight.Medium)
                                            Text("${w.origin.uppercase()} • ${w.qualityScore}% • اضغط للتعديل", color = AccentGold, fontSize = 11.sp)
                                        }
                                        Text(if (w.isFeatured) "★" else "✏️", fontSize = 18.sp, color = AccentGold)
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Upload Tab
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("1. اختر المجموعة:", color = MutedGray, fontSize = 13.sp)
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                collections.forEachIndexed { idx, col ->
                                    FilterChip(selected = selectedColIndex == idx, onClick = { selectedColIndex = idx }, label = { Text(col.titleAr) })
                                }
                            }
                            Text("2. اختر الأصل:", color = MutedGray, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("camera" to "📷 كاميرا", "ai" to "✨ ذكاء اصطناعي", "design" to "🎨 تصميم").forEach { (orig, lbl) ->
                                    FilterChip(selected = selectedOrigin == orig, onClick = { selectedOrigin = orig }, label = { Text(lbl) })
                                }
                            }
                            OutlinedTextField(value = uploadTitleAr, onValueChange = { uploadTitleAr = it }, label = { Text("العنوان بالعربي (اختياري)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = uploadTitleEn, onValueChange = { uploadTitleEn = it }, label = { Text("العنوان بالإنجليزي (اختياري)") }, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = { picker.launch("image/*") },
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (isLoading) "جاري الرفع..." else "اختيار صور من المعرض ورفعها")
                            }
                            if (statusMsg.isNotBlank()) {
                                Text(statusMsg, color = AccentGold, fontSize = 14.sp)
                            }
                        }
                    }
                    3 -> {
                        // Collections Management
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(12.dp)).padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("إنشاء مجموعة جديدة", color = AccentGold, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(value = newColSlug, onValueChange = { newColSlug = it }, label = { Text("الرابط المختصر (slug)") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = newColAr, onValueChange = { newColAr = it }, label = { Text("الاسم بالعربي") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = newColEn, onValueChange = { newColEn = it }, label = { Text("الاسم بالإنجليزي") }, modifier = Modifier.fillMaxWidth())
                                    Button(
                                        onClick = {
                                            val token = accessToken ?: return@Button
                                            scope.launch {
                                                withContext(Dispatchers.IO) {
                                                    val body = JSONObject().apply {
                                                        put("slug", newColSlug.trim())
                                                        put("title_en", newColEn.trim())
                                                        put("title_ar", newColAr.trim())
                                                        put("year", 2026)
                                                        put("sort_order", collections.size + 1)
                                                        put("is_published", true)
                                                    }
                                                    val req = Request.Builder()
                                                        .url("$SUPABASE_URL/rest/v1/collections")
                                                        .post(body.toString().toRequestBody("application/json".toMediaType()))
                                                        .addHeader("apikey", SUPABASE_ANON_KEY)
                                                        .addHeader("Authorization", "Bearer $token")
                                                        .build()
                                                    client.newCall(req).execute().close()
                                                    loadDashboardData(token)
                                                }
                                                newColSlug = ""; newColEn = ""; newColAr = ""
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("إنشاء المجموعة") }
                                }
                            }
                            items(collections) { c ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(10.dp)).padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(c.titleAr, color = if (c.isPublished) TextIvory else MutedGray, fontWeight = FontWeight.SemiBold)
                                        Text("${c.titleEn} (${c.slug})", color = MutedGray, fontSize = 12.sp)
                                    }
                                    TextButton(onClick = {
                                        val token = accessToken ?: return@TextButton
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                val body = JSONObject().put("is_published", !c.isPublished).toString()
                                                val req = Request.Builder()
                                                    .url("$SUPABASE_URL/rest/v1/collections?id=eq.${c.id}")
                                                    .patch(body.toRequestBody("application/json".toMediaType()))
                                                    .addHeader("apikey", SUPABASE_ANON_KEY)
                                                    .addHeader("Authorization", "Bearer $token")
                                                    .build()
                                                client.newCall(req).execute().close()
                                                loadDashboardData(token)
                                            }
                                        }
                                    }) {
                                        Text(if (c.isPublished) "إخفاء" else "نشر", color = if (c.isPublished) DangerRed else AccentGold)
                                    }
                                }
                            }
                        }
                    }
                    4 -> {
                        // Comments Moderation
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(comments) { c ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(10.dp)).padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${c.author} • ${c.workTitle}", color = AccentGold, fontSize = 12.sp)
                                        Text(c.body, color = if (c.isHidden) MutedGray else TextIvory, fontSize = 14.sp)
                                    }
                                    TextButton(onClick = {
                                        val token = accessToken ?: return@TextButton
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                val body = JSONObject().put("is_hidden", !c.isHidden).toString()
                                                val req = Request.Builder()
                                                    .url("$SUPABASE_URL/rest/v1/comments?id=eq.${c.id}")
                                                    .patch(body.toRequestBody("application/json".toMediaType()))
                                                    .addHeader("apikey", SUPABASE_ANON_KEY)
                                                    .addHeader("Authorization", "Bearer $token")
                                                    .build()
                                                client.newCall(req).execute().close()
                                                loadDashboardData(token)
                                            }
                                        }
                                    }) { Text(if (c.isHidden) "إظهار" else "إخفاء") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(SurfaceCard, RoundedCornerShape(12.dp)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AccentGold)
        Text(label, fontSize = 12.sp, color = MutedGray)
    }
}
