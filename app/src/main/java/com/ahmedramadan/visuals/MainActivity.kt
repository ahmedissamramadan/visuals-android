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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
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

data class CollectionItem(val id: String, val slug: String, val titleEn: String, val titleAr: String, val isPublished: Boolean)
data class WorkItem(val id: String, val slug: String, val titleEn: String, val titleAr: String, val origin: String, val collectionId: String, val thumbUrl: String, val fullUrl: String, val isPublished: Boolean, val isFeatured: Boolean, val qualityScore: Int)
data class CommentItem(val id: String, val body: String, val isHidden: Boolean, val author: String, val workTitle: String)
data class AuditLogItem(val id: String, val adminName: String, val action: String, val targetType: String, val targetId: String, val createdAt: String)

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

    var isAr by remember { mutableStateOf(true) }
    fun tr(ar: String, en: String) = if (isAr) ar else en

    var accessToken by remember { mutableStateOf<String?>(null) }
    var currentAdminId by remember { mutableStateOf<String?>(null) }
    var currentAdminName by remember { mutableStateOf("admin") }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var collections by remember { mutableStateOf<List<CollectionItem>>(emptyList()) }
    var works by remember { mutableStateOf<List<WorkItem>>(emptyList()) }
    var comments by remember { mutableStateOf<List<CommentItem>>(emptyList()) }
    var auditLogs by remember { mutableStateOf<List<AuditLogItem>>(emptyList()) }

    var activeTab by remember { mutableStateOf(0) }
    var editingWork by remember { mutableStateOf<WorkItem?>(null) }
    var viewingCollection by remember { mutableStateOf<CollectionItem?>(null) }
    var confirmDeleteCol by remember { mutableStateOf<CollectionItem?>(null) }

    var workSearch by remember { mutableStateOf("") }
    var workStatusFilter by remember { mutableStateOf("all") }

    var selectedColIndex by remember { mutableStateOf(0) }
    var selectedOrigin by remember { mutableStateOf("camera") }
    var uploadTitleAr by remember { mutableStateOf("") }
    var uploadTitleEn by remember { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf("") }

    var newColSlug by remember { mutableStateOf("") }
    var newColEn by remember { mutableStateOf("") }
    var newColAr by remember { mutableStateOf("") }

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

    suspend fun logAdminAction(token: String, action: String, targetType: String, targetId: String, details: JSONObject = JSONObject()) {
        val adminId = currentAdminId ?: return
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("admin_id", adminId)
                    put("admin_name", currentAdminName)
                    put("action", action)
                    put("target_type", targetType)
                    put("target_id", targetId)
                    put("details", details)
                }
                val req = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/admin_audit_logs")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                client.newCall(req).execute().close()
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
                        list.add(CollectionItem(o.getString("id"), o.getString("slug"), o.getString("title_en"), o.getString("title_ar"), o.optBoolean("is_published", true)))
                    }
                    collections = list
                    viewingCollection?.let { vc -> viewingCollection = list.find { it.id == vc.id } }
                }
            }

            val workReq = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/works?select=id,slug,title_en,title_ar,origin,collection_id,image_urls,is_published,is_featured,quality_score&order=created_at.desc&limit=300")
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
                        list.add(WorkItem(o.getString("id"), o.getString("slug"), o.optString("title_en", ""), o.optString("title_ar", ""), o.getString("origin"), o.getString("collection_id"), thumb, full, o.optBoolean("is_published", true), o.optBoolean("is_featured", false), o.optInt("quality_score", 85)))
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

            val logReq = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/admin_audit_logs?select=id,admin_name,action,target_type,target_id,created_at&order=created_at.desc&limit=25")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $token")
                .build()
            client.newCall(logReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val arr = JSONArray(resp.body?.string() ?: "[]")
                    val list = mutableListOf<AuditLogItem>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        list.add(AuditLogItem(o.getString("id"), o.getString("admin_name"), o.getString("action"), o.getString("target_type"), o.getString("target_id"), o.getString("created_at").take(16).replace("T", " ")))
                    }
                    auditLogs = list
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
                    statusMsg = tr("جاري رفع ${idx + 1}/${uris.size}...", "Uploading ${idx + 1}/${uris.size}...")
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
                    logAdminAction(token, "upload_work", "work", slug, JSONObject().put("collection", col.slug))
                }
                loadDashboardData(token)
                statusMsg = tr("تم رفع ${uris.size} عمل بنجاح!", "Uploaded ${uris.size} work(s)!")
            }
            isLoading = false
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides if (isAr) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        BackHandler(enabled = editingWork != null || viewingCollection != null || activeTab != 0) {
            when {
                editingWork != null -> editingWork = null
                viewingCollection != null -> viewingCollection = null
                else -> activeTab = 0
            }
        }

        // Delete Collection Confirmation Dialog
        val delCol = confirmDeleteCol
        if (delCol != null) {
            AlertDialog(
                onDismissRequest = { confirmDeleteCol = null },
                title = { Text(tr("تأكيد حذف المجموعة", "Confirm Delete Collection")) },
                text = { Text(tr("هل أنت متأكد من حذف \"${delCol.titleAr}\" نهائياً مع كل أعمالها؟ لا يمكن التراجع.", "Permanently delete \"${delCol.titleEn}\" and its works? This cannot be undone.")) },
                confirmButton = {
                    TextButton(onClick = {
                        val token = accessToken ?: return@TextButton
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val req = Request.Builder()
                                    .url("$SUPABASE_URL/rest/v1/collections?id=eq.${delCol.id}")
                                    .delete()
                                    .addHeader("apikey", SUPABASE_ANON_KEY)
                                    .addHeader("Authorization", "Bearer $token")
                                    .build()
                                client.newCall(req).execute().close()
                                logAdminAction(token, "delete_collection", "collection", delCol.slug)
                                loadDashboardData(token)
                            }
                            if (viewingCollection?.id == delCol.id) viewingCollection = null
                            confirmDeleteCol = null
                        }
                    }) { Text(tr("حذف نهائي", "Delete"), color = DangerRed) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDeleteCol = null }) { Text(tr("إلغاء", "Cancel")) }
                }
            )
        }

        if (accessToken == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { isAr = !isAr }) { Text(if (isAr) "EN" else "عربي", color = AccentGold) }
                }
                Image(
                    painter = painterResource(R.drawable.ic_nb_projector),
                    contentDescription = "Visuals Admin",
                    modifier = Modifier.size(88.dp).clip(RoundedCornerShape(20.dp))
                )
                Spacer(Modifier.height(14.dp))
                Text("Visuals Admin", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                Text(tr("لوحة التحكم الشاملة", "Enterprise Visual Control"), fontSize = 14.sp, color = MutedGray)
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(tr("اسم المستخدم أو البريد الإلكتروني", "Username or Email")) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(tr("الرمز السري", "Password")) },
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
                                        val userObj = json.optJSONObject("user")
                                        currentAdminId = userObj?.optString("id")
                                        currentAdminName = if (email.startsWith("bodykhaled")) "boda890" else "admin"
                                        accessToken = tok
                                        loadDashboardData(tok)
                                    } else {
                                        loginError = tr("بيانات الدخول غير صحيحة", "Invalid credentials")
                                    }
                                }
                            }
                            isLoading = false
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (isLoading) tr("جاري الدخول...", "Signing in...") else tr("تسجيل الدخول", "Sign In")) }
            }
            return@CompositionLocalProvider
        }

        Scaffold(
            containerColor = BgDark,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
                    navigationIcon = {
                        when {
                            editingWork != null -> TextButton(onClick = { editingWork = null }) { Text(tr("رجوع", "Back"), color = AccentGold) }
                            viewingCollection != null -> TextButton(onClick = { viewingCollection = null }) { Text(tr("رجوع", "Back"), color = AccentGold) }
                            activeTab != 0 -> TextButton(onClick = { activeTab = 0 }) { Text(tr("الرئيسية", "Home"), color = AccentGold) }
                        }
                    },
                    title = {
                        Text(
                            when {
                                editingWork != null -> tr("تعديل العمل", "Edit Work")
                                viewingCollection != null -> if (isAr) viewingCollection!!.titleAr else viewingCollection!!.titleEn
                                activeTab == 0 -> tr("الرئيسية", "Dashboard")
                                activeTab == 1 -> tr("الأعمال", "Works")
                                activeTab == 2 -> tr("رفع جديد", "Upload")
                                activeTab == 3 -> tr("المجموعات", "Collections")
                                else -> tr("التعليقات", "Comments")
                            },
                            color = TextIvory, fontWeight = FontWeight.Bold, fontSize = 17.sp
                        )
                    },
                    actions = {
                        TextButton(onClick = { isAr = !isAr }) { Text(if (isAr) "EN" else "عربي", color = AccentGold, fontSize = 12.sp) }
                        TextButton(onClick = {
                            val tok = accessToken ?: return@TextButton
                            scope.launch { loadDashboardData(tok) }
                        }) { Text("🔄", fontSize = 16.sp) }
                        TextButton(onClick = { accessToken = null; editingWork = null; viewingCollection = null; activeTab = 0 }) {
                            Text(tr("خروج", "Out"), color = MutedGray, fontSize = 12.sp)
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = SurfaceDark) {
                    val navItems = listOf(
                        "🏠" to tr("الرئيسية", "Home"),
                        "🖼" to tr("الأعمال", "Works"),
                        "⬆️" to tr("رفع", "Upload"),
                        "📁" to tr("المجموعات", "Sets"),
                        "💬" to tr("تعليقات", "Chat")
                    )
                    navItems.forEachIndexed { idx, (ico, lbl) ->
                        NavigationBarItem(
                            selected = editingWork == null && viewingCollection == null && activeTab == idx,
                            onClick = { editingWork = null; viewingCollection = null; activeTab = idx },
                            icon = { Text(ico, fontSize = 17.sp) },
                            label = { Text(lbl, fontSize = 10.sp) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
                if (updateVersion != null && updateApkUrl != null) {
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateApkUrl))) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text(tr("تحديث جديد متاح (v$updateVersion) - اضغط للتحميل", "Update v$updateVersion available - Tap to install")) }
                }

                val currentEdit = editingWork
                val currentCol = viewingCollection

                if (currentEdit != null) {
                    // Full Work Editor
                    var editAr by remember(currentEdit.id) { mutableStateOf(currentEdit.titleAr) }
                    var editEn by remember(currentEdit.id) { mutableStateOf(currentEdit.titleEn) }
                    var editOrigin by remember(currentEdit.id) { mutableStateOf(currentEdit.origin) }
                    var editColId by remember(currentEdit.id) { mutableStateOf(currentEdit.collectionId) }
                    var editPub by remember(currentEdit.id) { mutableStateOf(currentEdit.isPublished) }
                    var editFeat by remember(currentEdit.id) { mutableStateOf(currentEdit.isFeatured) }

                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(model = currentEdit.fullUrl, contentDescription = editAr, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceDark))
                        OutlinedTextField(value = editAr, onValueChange = { editAr = it }, label = { Text(tr("العنوان بالعربي", "Arabic Title")) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = editEn, onValueChange = { editEn = it }, label = { Text(tr("العنوان بالإنجليزي", "English Title")) }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("camera" to tr("📷 كاميرا", "📷 Camera"), "ai" to tr("✨ ذكاء", "✨ AI"), "design" to tr("🎨 تصميم", "🎨 Design")).forEach { (orig, lbl) ->
                                FilterChip(selected = editOrigin == orig, onClick = { editOrigin = orig }, label = { Text(lbl) })
                            }
                        }
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            collections.forEach { col ->
                                FilterChip(selected = editColId == col.id, onClick = { editColId = col.id }, label = { Text(if (isAr) col.titleAr else col.titleEn) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilterChip(selected = editPub, onClick = { editPub = !editPub }, label = { Text(if (editPub) tr("منشور ✓", "Published ✓") else tr("مؤرشف", "Archived")) })
                            FilterChip(selected = editFeat, onClick = { editFeat = !editFeat }, label = { Text(if (editFeat) tr("مميز ★", "Featured ★") else tr("عادي ☆", "Standard ☆")) })
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
                                        val req = Request.Builder().url("$SUPABASE_URL/rest/v1/works?id=eq.${currentEdit.id}").patch(body.toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                        client.newCall(req).execute().close()
                                        logAdminAction(token, "update_work", "work", currentEdit.slug, JSONObject().put("published", editPub).put("featured", editFeat))
                                        loadDashboardData(token)
                                    }
                                    editingWork = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)
                        ) { Text(tr("حفظ التعديلات", "Save Changes")) }
                        OutlinedButton(
                            onClick = {
                                val token = accessToken ?: return@OutlinedButton
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val req = Request.Builder().url("$SUPABASE_URL/rest/v1/works?id=eq.${currentEdit.id}").delete().addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                        client.newCall(req).execute().close()
                                        logAdminAction(token, "delete_work", "work", currentEdit.slug)
                                        loadDashboardData(token)
                                    }
                                    editingWork = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                        ) { Text(tr("حذف العمل نهائياً", "Delete Work Permanently")) }
                    }
                } else if (currentCol != null) {
                    // Collection Detail Screen: Browse Works + Toggle Publish/Hide + Delete
                    val colWorks = works.filter { it.collectionId == currentCol.id }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(12.dp)).padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (isAr) currentCol.titleAr else currentCol.titleEn, color = TextIvory, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${colWorks.size} ${tr("عمل", "works")} • ${if (currentCol.isPublished) tr("منشورة", "Published") else tr("مخفية", "Hidden")}", color = AccentGold, fontSize = 12.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        val token = accessToken ?: return@Button
                                        val nextPub = !currentCol.isPublished
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                val body = JSONObject().put("is_published", nextPub).toString()
                                                val req = Request.Builder().url("$SUPABASE_URL/rest/v1/collections?id=eq.${currentCol.id}").patch(body.toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                client.newCall(req).execute().close()
                                                logAdminAction(token, if (nextPub) "publish_collection" else "hide_collection", "collection", currentCol.slug)
                                                loadDashboardData(token)
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text(if (currentCol.isPublished) tr("إخفاء", "Hide") else tr("نشر", "Publish"), fontSize = 12.sp) }
                                OutlinedButton(
                                    onClick = { confirmDeleteCol = currentCol },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text(tr("حذف", "Del"), fontSize = 12.sp) }
                            }
                        }

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(colWorks) { w ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(10.dp)).clickable { editingWork = w }.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(model = w.thumbUrl, contentDescription = w.titleAr, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceDark))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(if (isAr) w.titleAr.ifBlank { w.titleEn } else w.titleEn.ifBlank { w.titleAr }, color = if (w.isPublished) TextIvory else MutedGray, fontWeight = FontWeight.Medium)
                                        Text("${w.origin.uppercase()} • ${w.qualityScore}%", color = AccentGold, fontSize = 11.sp)
                                    }
                                    Text(if (w.isFeatured) "★" else "✏️", fontSize = 16.sp, color = AccentGold)
                                }
                            }
                        }
                    }
                } else {
                    when (activeTab) {
                        0 -> {
                            // Dashboard + Audit Log
                            val pubWorks = works.count { it.isPublished }
                            val archWorks = works.count { !it.isPublished }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                item {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Image(painter = painterResource(R.drawable.ic_nb_home), contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                                        Column {
                                            Text(tr("مرحباً، $currentAdminName", "Welcome, $currentAdminName"), color = TextIvory, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            Text(tr("كل الإجراءات تُسجل تلقائياً في سجل النشاط", "All actions are logged in Audit Trail"), color = MutedGray, fontSize = 12.sp)
                                        }
                                    }
                                }
                                item {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        StatCard(tr("المجموعات", "Sets"), collections.size.toString(), Modifier.weight(1f))
                                        StatCard(tr("منشور", "Live"), pubWorks.toString(), Modifier.weight(1f))
                                        StatCard(tr("مؤرشف", "Hidden"), archWorks.toString(), Modifier.weight(1f))
                                    }
                                }
                                item {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { activeTab = 2 }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text(tr("⬆️ رفع صور", "⬆️ Upload")) }
                                        OutlinedButton(onClick = { activeTab = 3 }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text(tr("📁 المجموعات", "📁 Sets")) }
                                    }
                                }
                                item {
                                    Text(tr("سجل نشاط الأدمن (Audit Log)", "Admin Audit Log"), color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                items(auditLogs) { log ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(8.dp)).padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${log.adminName} → ${log.action}", color = TextIvory, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Text("${log.targetType}: ${log.targetId}", color = MutedGray, fontSize = 11.sp)
                                        }
                                        Text(log.createdAt, color = AccentGold, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                        1 -> {
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
                                OutlinedTextField(value = workSearch, onValueChange = { workSearch = it }, label = { Text(tr("بحث في الأعمال...", "Search works...")) }, modifier = Modifier.fillMaxWidth())
                                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("all" to tr("الكل", "All"), "published" to tr("منشور", "Live"), "archived" to tr("مؤرشف", "Hidden"), "featured" to tr("مميز ★", "Star ★")).forEach { (k, lbl) ->
                                        FilterChip(selected = workStatusFilter == k, onClick = { workStatusFilter = k }, label = { Text(lbl) })
                                    }
                                }
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(filtered) { w ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(10.dp)).clickable { editingWork = w }.padding(10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(model = w.thumbUrl, contentDescription = w.titleAr, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceDark))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(if (isAr) w.titleAr.ifBlank { w.titleEn } else w.titleEn.ifBlank { w.titleAr }, color = if (w.isPublished) TextIvory else MutedGray, fontWeight = FontWeight.Medium)
                                                Text("${w.origin.uppercase()} • ${w.qualityScore}%", color = AccentGold, fontSize = 11.sp)
                                            }
                                            Text(if (w.isFeatured) "★" else "✏️", fontSize = 16.sp, color = AccentGold)
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(tr("1. اختر المجموعة:", "1. Select Collection:"), color = MutedGray, fontSize = 13.sp)
                                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    collections.forEachIndexed { idx, col ->
                                        FilterChip(selected = selectedColIndex == idx, onClick = { selectedColIndex = idx }, label = { Text(if (isAr) col.titleAr else col.titleEn) })
                                    }
                                }
                                Text(tr("2. اختر الأصل:", "2. Select Origin:"), color = MutedGray, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("camera" to tr("📷 كاميرا", "📷 Camera"), "ai" to tr("✨ ذكاء", "✨ AI"), "design" to tr("🎨 تصميم", "🎨 Design")).forEach { (orig, lbl) ->
                                        FilterChip(selected = selectedOrigin == orig, onClick = { selectedOrigin = orig }, label = { Text(lbl) })
                                    }
                                }
                                OutlinedTextField(value = uploadTitleAr, onValueChange = { uploadTitleAr = it }, label = { Text(tr("العنوان بالعربي (اختياري)", "Arabic Title (optional)")) }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = uploadTitleEn, onValueChange = { uploadTitleEn = it }, label = { Text(tr("العنوان بالإنجليزي (اختياري)", "English Title (optional)")) }, modifier = Modifier.fillMaxWidth())
                                Button(onClick = { picker.launch("image/*") }, enabled = !isLoading, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(12.dp)) {
                                    Text(if (isLoading) tr("جاري الرفع...", "Uploading...") else tr("اختيار صور من المعرض ورفعها", "Pick Images & Upload"))
                                }
                                if (statusMsg.isNotBlank()) Text(statusMsg, color = AccentGold, fontSize = 14.sp)
                            }
                        }
                        3 -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                item {
                                    Column(modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(12.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(tr("إنشاء مجموعة جديدة", "New Collection"), color = AccentGold, fontWeight = FontWeight.Bold)
                                        OutlinedTextField(value = newColSlug, onValueChange = { newColSlug = it }, label = { Text("Slug") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = newColAr, onValueChange = { newColAr = it }, label = { Text(tr("الاسم بالعربي", "Arabic Title")) }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = newColEn, onValueChange = { newColEn = it }, label = { Text(tr("الاسم بالإنجليزي", "English Title")) }, modifier = Modifier.fillMaxWidth())
                                        Button(
                                            onClick = {
                                                val token = accessToken ?: return@Button
                                                scope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        val slug = newColSlug.trim()
                                                        val body = JSONObject().apply {
                                                            put("slug", slug)
                                                            put("title_en", newColEn.trim())
                                                            put("title_ar", newColAr.trim())
                                                            put("year", 2026)
                                                            put("sort_order", collections.size + 1)
                                                            put("is_published", true)
                                                        }
                                                        val req = Request.Builder().url("$SUPABASE_URL/rest/v1/collections").post(body.toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                        client.newCall(req).execute().close()
                                                        logAdminAction(token, "create_collection", "collection", slug)
                                                        loadDashboardData(token)
                                                    }
                                                    newColSlug = ""; newColEn = ""; newColAr = ""
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text(tr("إنشاء المجموعة", "Create Collection")) }
                                    }
                                }
                                items(collections) { c ->
                                    val count = works.count { it.collectionId == c.id }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(10.dp)).clickable { viewingCollection = c }.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(if (isAr) c.titleAr else c.titleEn, color = if (c.isPublished) TextIvory else MutedGray, fontWeight = FontWeight.SemiBold)
                                            Text("$count ${tr("عمل", "works")} • ${if (c.isPublished) tr("منشورة", "Published") else tr("مخفية", "Hidden")}", color = AccentGold, fontSize = 11.sp)
                                        }
                                        Row {
                                            TextButton(onClick = {
                                                val token = accessToken ?: return@TextButton
                                                val nextPub = !c.isPublished
                                                scope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        val body = JSONObject().put("is_published", nextPub).toString()
                                                        val req = Request.Builder().url("$SUPABASE_URL/rest/v1/collections?id=eq.${c.id}").patch(body.toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                        client.newCall(req).execute().close()
                                                        logAdminAction(token, if (nextPub) "publish_collection" else "hide_collection", "collection", c.slug)
                                                        loadDashboardData(token)
                                                    }
                                                }
                                            }) { Text(if (c.isPublished) tr("إخفاء", "Hide") else tr("نشر", "Publish"), color = if (c.isPublished) MutedGray else AccentGold) }
                                            TextButton(onClick = { confirmDeleteCol = c }) { Text(tr("حذف", "Del"), color = DangerRed) }
                                        }
                                    }
                                }
                            }
                        }
                        4 -> {
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
                                            val nextHide = !c.isHidden
                                            scope.launch {
                                                withContext(Dispatchers.IO) {
                                                    val body = JSONObject().put("is_hidden", nextHide).toString()
                                                    val req = Request.Builder().url("$SUPABASE_URL/rest/v1/comments?id=eq.${c.id}").patch(body.toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                    client.newCall(req).execute().close()
                                                    logAdminAction(token, if (nextHide) "hide_comment" else "unhide_comment", "comment", c.id)
                                                    loadDashboardData(token)
                                                }
                                            }
                                        }) { Text(if (c.isHidden) tr("إظهار", "Show") else tr("إخفاء", "Hide")) }
                                    }
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
