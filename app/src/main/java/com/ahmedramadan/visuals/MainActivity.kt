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
import androidx.compose.foundation.shape.CircleShape
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
private const val HEADSHOT_URL = "https://owlglaqltdniywcprzab.supabase.co/storage/v1/object/public/visuals/brand/ahmed-headshot-480.webp"

private val BgDark = Color(0xFF0A0A0A)
private val SurfaceDark = Color(0xFF141416)
private val SurfaceCard = Color(0xFF1C1C20)
private val AccentWhite = Color(0xFFFFFFFF)
private val TextIvory = Color(0xFFFFFFFF)
private val MutedGray = Color(0xFFA1A1AA)
private val DangerRed = Color(0xFFEF4444)
private val GreenOk = Color(0xFF22C55E)

data class CollectionItem(
    val id: String,
    val slug: String,
    val category: String,
    val titleEn: String,
    val titleAr: String,
    val descEn: String,
    val descAr: String,
    val mediumEn: String,
    val mediumAr: String,
    val isPublished: Boolean
)

data class WorkItem(
    val id: String,
    val slug: String,
    val titleEn: String,
    val titleAr: String,
    val origin: String,
    val chapter: String,
    val collectionId: String,
    val thumbUrl: String,
    val fullUrl: String,
    val isPublished: Boolean,
    val isFeatured: Boolean,
    val isCurated: Boolean,
    val qualityScore: Int
)

data class CaseStudyItem(
    val id: String,
    val slug: String,
    val titleAr: String,
    val titleEn: String,
    val sectorAr: String,
    val sectorEn: String,
    val problemAr: String,
    val problemEn: String,
    val solutionAr: String,
    val solutionEn: String,
    val outcomeAr: String,
    val outcomeEn: String,
    val collectionSlug: String,
    val externalUrl: String,
    val coverUrl: String
)

data class MindNoteItem(
    val id: String,
    val slug: String,
    val category: String,
    val titleAr: String,
    val titleEn: String,
    val bodyAr: String,
    val bodyEn: String
)

data class CommentItem(val id: String, val body: String, val isHidden: Boolean, val author: String, val workTitle: String)
data class InquiryItem(val id: String, val senderName: String, val senderContact: String, val message: String, val isResolved: Boolean, val createdAt: String)
data class AuditLogItem(val id: String, val adminName: String, val action: String, val targetType: String, val targetId: String, val createdAt: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = BgDark,
                    surface = SurfaceDark,
                    primary = AccentWhite,
                    onPrimary = BgDark,
                    onBackground = TextIvory,
                    onSurface = TextIvory
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = BgDark) {
                    ArtelligenceApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtelligenceApp() {
    val client = remember { OkHttpClient() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var isAr by remember { mutableStateOf(true) }
    fun tr(ar: String, en: String) = if (isAr) ar else en

    // Public Data
    var collections by remember { mutableStateOf<List<CollectionItem>>(emptyList()) }
    var works by remember { mutableStateOf<List<WorkItem>>(emptyList()) }
    var caseStudies by remember { mutableStateOf<List<CaseStudyItem>>(emptyList()) }
    var mindNotes by remember { mutableStateOf<List<MindNoteItem>>(emptyList()) }

    // Navigation: 0=Mind, 1=Work, 2=Challenge, 3=Connect
    var publicTab by remember { mutableStateOf(0) }
    var workViewMode by remember { mutableStateOf(0) } // 0=CaseStudies, 1=Collections
    var viewingCollection by remember { mutableStateOf<CollectionItem?>(null) }
    var viewingWork by remember { mutableStateOf<WorkItem?>(null) }
    var showFullPhotoArchive by remember { mutableStateOf(false) }

    // Challenge (Human or AI?) state
    var gameRounds by remember { mutableStateOf<List<WorkItem>>(emptyList()) }
    var gameIdx by remember { mutableStateOf(0) }
    var gameScore by remember { mutableStateOf(0) }
    var gameAnswered by remember { mutableStateOf<Boolean?>(null) }

    // Connect form state
    var inqName by remember { mutableStateOf("") }
    var inqContact by remember { mutableStateOf("") }
    var inqMsg by remember { mutableStateOf("") }
    var inqStatus by remember { mutableStateOf("") }

    // Admin Mode state
    var showAdminScreen by remember { mutableStateOf(false) }
    var accessToken by remember { mutableStateOf<String?>(null) }
    var currentAdminId by remember { mutableStateOf<String?>(null) }
    var currentAdminName by remember { mutableStateOf("admin") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var adminTab by remember { mutableStateOf(0) } // 0=Overview/Audit, 1=Works, 2=Upload, 3=Collections, 4=Mind&Inquiries
    var editingWork by remember { mutableStateOf<WorkItem?>(null) }
    var confirmDeleteCol by remember { mutableStateOf<CollectionItem?>(null) }
    var comments by remember { mutableStateOf<List<CommentItem>>(emptyList()) }
    var inquiries by remember { mutableStateOf<List<InquiryItem>>(emptyList()) }
    var auditLogs by remember { mutableStateOf<List<AuditLogItem>>(emptyList()) }

    // Admin Upload & Forms
    var selectedColIndex by remember { mutableStateOf(0) }
    var selectedOrigin by remember { mutableStateOf("camera") }
    var uploadTitleAr by remember { mutableStateOf("") }
    var uploadTitleEn by remember { mutableStateOf("") }
    var uploadStatus by remember { mutableStateOf("") }

    var newNoteAr by remember { mutableStateOf("") }
    var newNoteEn by remember { mutableStateOf("") }
    var newNoteBodyAr by remember { mutableStateOf("") }
    var newNoteBodyEn by remember { mutableStateOf("") }

    var newColSlug by remember { mutableStateOf("") }
    var newColAr by remember { mutableStateOf("") }
    var newColEn by remember { mutableStateOf("") }

    var updateVersion by remember { mutableStateOf<String?>(null) }
    var updateApkUrl by remember { mutableStateOf<String?>(null) }

    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {}
    }

    fun startChallenge(pool: List<WorkItem>) {
        val cams = pool.filter { it.isPublished && it.origin == "camera" }.shuffled().take(5)
        val ais = pool.filter { it.isPublished && it.origin == "ai" }.shuffled().take(5)
        gameRounds = (cams + ais).shuffled()
        gameIdx = 0
        gameScore = 0
        gameAnswered = null
    }

    suspend fun loadPublicData(token: String? = accessToken) {
        withContext(Dispatchers.IO) {
            val authHeader = token ?: SUPABASE_ANON_KEY

            fun getJson(path: String): JSONArray {
                val req = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/$path")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer $authHeader")
                    .build()
                client.newCall(req).execute().use { resp ->
                    return if (resp.isSuccessful) JSONArray(resp.body?.string() ?: "[]") else JSONArray()
                }
            }

            val colFilter = if (token != null) "" else "&is_published=eq.true"
            val colArr = getJson("collections?select=id,slug,category,title_en,title_ar,description_en,description_ar,medium_en,medium_ar,is_published$colFilter&order=sort_order")
            val cList = mutableListOf<CollectionItem>()
            for (i in 0 until colArr.length()) {
                val o = colArr.getJSONObject(i)
                cList.add(CollectionItem(
                    o.getString("id"), o.getString("slug"), o.optString("category", "personal"),
                    o.getString("title_en"), o.getString("title_ar"),
                    o.optString("description_en", ""), o.optString("description_ar", ""),
                    o.optString("medium_en", ""), o.optString("medium_ar", ""),
                    o.optBoolean("is_published", true)
                ))
            }
            collections = cList
            viewingCollection?.let { vc -> viewingCollection = cList.find { it.id == vc.id } }

            val workFilter = if (token != null) "" else "&is_published=eq.true"
            val workArr = getJson("works?select=id,slug,title_en,title_ar,origin,chapter,collection_id,image_urls,is_published,is_featured,is_curated,quality_score$workFilter&order=sort_order&limit=400")
            val wList = mutableListOf<WorkItem>()
            for (i in 0 until workArr.length()) {
                val o = workArr.getJSONObject(i)
                val imgs = o.optJSONObject("image_urls")
                val thumb = imgs?.optString("480")?.ifBlank { imgs.optString("1200", "") } ?: ""
                val full = imgs?.optString("1200")?.ifBlank { thumb } ?: thumb
                wList.add(WorkItem(
                    o.getString("id"), o.getString("slug"),
                    o.optString("title_en", ""), o.optString("title_ar", ""),
                    o.getString("origin"), o.optString("chapter", ""),
                    o.getString("collection_id"), thumb, full,
                    o.optBoolean("is_published", true), o.optBoolean("is_featured", false),
                    o.optBoolean("is_curated", true), o.optInt("quality_score", 85)
                ))
            }
            works = wList
            if (gameRounds.isEmpty() && wList.isNotEmpty()) startChallenge(wList)

            val csArr = getJson("case_studies?select=*&is_published=eq.true&order=sort_order")
            val csList = mutableListOf<CaseStudyItem>()
            for (i in 0 until csArr.length()) {
                val o = csArr.getJSONObject(i)
                csList.add(CaseStudyItem(
                    o.getString("id"), o.getString("slug"),
                    o.getString("title_ar"), o.getString("title_en"),
                    o.getString("sector_ar"), o.getString("sector_en"),
                    o.getString("problem_ar"), o.getString("problem_en"),
                    o.getString("solution_ar"), o.getString("solution_en"),
                    o.getString("outcome_ar"), o.getString("outcome_en"),
                    o.optString("collection_slug", ""), o.optString("external_url", ""), o.optString("cover_url", "")
                ))
            }
            caseStudies = csList

            val mnArr = getJson("mind_notes?select=*&is_published=eq.true&order=sort_order")
            val mnList = mutableListOf<MindNoteItem>()
            for (i in 0 until mnArr.length()) {
                val o = mnArr.getJSONObject(i)
                mnList.add(MindNoteItem(
                    o.getString("id"), o.getString("slug"), o.getString("category"),
                    o.getString("title_ar"), o.getString("title_en"),
                    o.getString("body_ar"), o.getString("body_en")
                ))
            }
            mindNotes = mnList

            if (token != null) {
                val comArr = getJson("comments?select=id,body,is_hidden,profiles(display_name),works(title_en)&order=created_at.desc&limit=40")
                val comList = mutableListOf<CommentItem>()
                for (i in 0 until comArr.length()) {
                    val o = comArr.getJSONObject(i)
                    comList.add(CommentItem(
                        o.getString("id"), o.getString("body"), o.getBoolean("is_hidden"),
                        o.optJSONObject("profiles")?.optString("display_name") ?: "Visitor",
                        o.optJSONObject("works")?.optString("title_en") ?: ""
                    ))
                }
                comments = comList

                val inqArr = getJson("inquiries?select=*&order=created_at.desc&limit=30")
                val inqList = mutableListOf<InquiryItem>()
                for (i in 0 until inqArr.length()) {
                    val o = inqArr.getJSONObject(i)
                    inqList.add(InquiryItem(
                        o.getString("id"), o.getString("sender_name"), o.getString("sender_contact"),
                        o.getString("message"), o.optBoolean("is_resolved", false),
                        o.optString("created_at", "").take(16).replace("T", " ")
                    ))
                }
                inquiries = inqList

                val logArr = getJson("admin_audit_logs?select=id,admin_name,action,target_type,target_id,created_at&order=created_at.desc&limit=25")
                val lList = mutableListOf<AuditLogItem>()
                for (i in 0 until logArr.length()) {
                    val o = logArr.getJSONObject(i)
                    lList.add(AuditLogItem(
                        o.getString("id"), o.getString("admin_name"), o.getString("action"),
                        o.getString("target_type"), o.getString("target_id"),
                        o.optString("created_at", "").take(16).replace("T", " ")
                    ))
                }
                auditLogs = lList
            }
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

    LaunchedEffect(Unit) {
        loadPublicData(null)
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

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        val token = accessToken ?: return@rememberLauncherForActivityResult
        if (uris.isEmpty() || collections.isEmpty()) return@rememberLauncherForActivityResult
        val col = collections[selectedColIndex.coerceIn(0, collections.lastIndex)]

        scope.launch {
            isLoading = true
            withContext(Dispatchers.IO) {
                for ((idx, uri) in uris.withIndex()) {
                    uploadStatus = tr("جاري رفع ${idx + 1}/${uris.size}...", "Uploading ${idx + 1}/${uris.size}...")
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
                loadPublicData(token)
                uploadStatus = tr("تم رفع ${uris.size} عمل بنجاح!", "Uploaded ${uris.size} work(s)!")
            }
            isLoading = false
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides if (isAr) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        BackHandler(enabled = editingWork != null || viewingWork != null || viewingCollection != null || showAdminScreen) {
            when {
                editingWork != null -> editingWork = null
                viewingWork != null -> viewingWork = null
                viewingCollection != null -> { viewingCollection = null; showFullPhotoArchive = false }
                showAdminScreen -> showAdminScreen = false
            }
        }

        val delCol = confirmDeleteCol
        if (delCol != null) {
            AlertDialog(
                onDismissRequest = { confirmDeleteCol = null },
                title = { Text(tr("تأكيد حذف المجموعة", "Confirm Delete Collection")) },
                text = { Text(tr("هل أنت متأكد من حذف \"${delCol.titleAr}\" نهائياً؟", "Permanently delete \"${delCol.titleEn}\"?")) },
                confirmButton = {
                    TextButton(onClick = {
                        val token = accessToken ?: return@TextButton
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val req = Request.Builder().url("$SUPABASE_URL/rest/v1/collections?id=eq.${delCol.id}").delete().addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                client.newCall(req).execute().close()
                                logAdminAction(token, "delete_collection", "collection", delCol.slug)
                                loadPublicData(token)
                            }
                            if (viewingCollection?.id == delCol.id) viewingCollection = null
                            confirmDeleteCol = null
                        }
                    }) { Text(tr("حذف نهائي", "Delete"), color = DangerRed) }
                },
                dismissButton = { TextButton(onClick = { confirmDeleteCol = null }) { Text(tr("إلغاء", "Cancel")) } }
            )
        }

        Scaffold(
            containerColor = BgDark,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
                    navigationIcon = {
                        when {
                            editingWork != null -> TextButton(onClick = { editingWork = null }) { Text(tr("رجوع", "Back"), color = AccentWhite) }
                            viewingWork != null -> TextButton(onClick = { viewingWork = null }) { Text(tr("رجوع", "Back"), color = AccentWhite) }
                            viewingCollection != null -> TextButton(onClick = { viewingCollection = null; showFullPhotoArchive = false }) { Text(tr("رجوع", "Back"), color = AccentWhite) }
                            showAdminScreen -> TextButton(onClick = { showAdminScreen = false }) { Text(tr("العودة للمنصة", "Exit Studio"), color = AccentWhite) }
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Image(painter = painterResource(R.drawable.ic_nb_projector), contentDescription = null, modifier = Modifier.size(28.dp).clip(CircleShape))
                            Text(
                                if (showAdminScreen) tr("استوديو الإدارة", "Admin Studio") else "Artelligence",
                                color = TextIvory, fontWeight = FontWeight.Bold, fontSize = 17.sp
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = { isAr = !isAr }) { Text(if (isAr) "EN" else "عربي", color = AccentWhite, fontSize = 12.sp) }
                        TextButton(onClick = { scope.launch { loadPublicData(accessToken) } }) { Text("🔄", fontSize = 15.sp) }
                        TextButton(onClick = { showAdminScreen = !showAdminScreen }) { Text("⚙️", fontSize = 16.sp) }
                    }
                )
            },
            bottomBar = {
                if (!showAdminScreen && viewingWork == null && viewingCollection == null) {
                    NavigationBar(containerColor = SurfaceDark) {
                        val tabs = listOf(
                            "🧠" to tr("عقلي", "Mind"),
                            "💼" to tr("أعمالي", "Work"),
                            "✨" to tr("التحدي", "Quiz"),
                            "💬" to tr("تواصل", "Connect")
                        )
                        tabs.forEachIndexed { idx, (ico, lbl) ->
                            NavigationBarItem(
                                selected = publicTab == idx,
                                onClick = { publicTab = idx },
                                icon = { Text(ico, fontSize = 17.sp) },
                                label = { Text(lbl, fontSize = 11.sp) }
                            )
                        }
                    }
                } else if (showAdminScreen && accessToken != null && editingWork == null && viewingCollection == null) {
                    NavigationBar(containerColor = SurfaceDark) {
                        val tabs = listOf(
                            "📊" to tr("سجل", "Audit"),
                            "🖼" to tr("الأعمال", "Works"),
                            "⬆️" to tr("رفع", "Upload"),
                            "📁" to tr("مجموعات", "Sets"),
                            "🧠" to tr("عقلي/رسائل", "Inbox")
                        )
                        tabs.forEachIndexed { idx, (ico, lbl) ->
                            NavigationBarItem(
                                selected = adminTab == idx,
                                onClick = { adminTab = idx },
                                icon = { Text(ico, fontSize = 16.sp) },
                                label = { Text(lbl, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
                if (updateVersion != null && updateApkUrl != null) {
                    Button(
                        onClick = { openUrl(updateApkUrl!!) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text(tr("تحديث جديد متاح (v$updateVersion) - اضغط للتثبيت", "Update v$updateVersion available - Tap to install")) }
                }

                val currentEdit = editingWork
                val currentPreview = viewingWork
                val currentCol = viewingCollection

                if (currentEdit != null && accessToken != null) {
                    // Admin Work Editor
                    var editAr by remember(currentEdit.id) { mutableStateOf(currentEdit.titleAr) }
                    var editEn by remember(currentEdit.id) { mutableStateOf(currentEdit.titleEn) }
                    var editOrigin by remember(currentEdit.id) { mutableStateOf(currentEdit.origin) }
                    var editColId by remember(currentEdit.id) { mutableStateOf(currentEdit.collectionId) }
                    var editPub by remember(currentEdit.id) { mutableStateOf(currentEdit.isPublished) }
                    var editFeat by remember(currentEdit.id) { mutableStateOf(currentEdit.isFeatured) }

                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(model = currentEdit.fullUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceDark))
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
                            FilterChip(selected = editPub, onClick = { editPub = !editPub }, label = { Text(if (editPub) tr("منشور ✓", "Live ✓") else tr("مؤرشف", "Hidden")) })
                            FilterChip(selected = editFeat, onClick = { editFeat = !editFeat }, label = { Text(if (editFeat) tr("مميز ★", "Star ★") else tr("عادي", "Standard")) })
                        }
                        Button(
                            onClick = {
                                val token = accessToken ?: return@Button
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val body = JSONObject().apply {
                                            put("title_ar", editAr.trim()); put("title_en", editEn.trim())
                                            put("origin", editOrigin); put("collection_id", editColId)
                                            put("is_published", editPub); put("is_featured", editFeat)
                                        }
                                        val req = Request.Builder().url("$SUPABASE_URL/rest/v1/works?id=eq.${currentEdit.id}").patch(body.toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                        client.newCall(req).execute().close()
                                        logAdminAction(token, "update_work", "work", currentEdit.slug)
                                        loadPublicData(token)
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
                                        loadPublicData(token)
                                    }
                                    editingWork = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                        ) { Text(tr("حذف نهائي", "Delete Permanently")) }
                    }
                } else if (currentPreview != null) {
                    // Fullscreen Public Preview
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        AsyncImage(model = currentPreview.fullUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(14.dp)).background(SurfaceDark))
                        Text(if (isAr) currentPreview.titleAr.ifBlank { currentPreview.titleEn } else currentPreview.titleEn.ifBlank { currentPreview.titleAr }, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextIvory)
                        Text("${currentPreview.origin.uppercase()} · ${currentPreview.qualityScore}%", color = MutedGray, fontSize = 13.sp)
                        if (accessToken != null && showAdminScreen) {
                            Button(onClick = { editingWork = currentPreview; viewingWork = null }, modifier = Modifier.fillMaxWidth()) {
                                Text(tr("تعديل هذا العمل", "Edit Work"))
                            }
                        }
                    }
                } else if (currentCol != null) {
                    // Collection Detail Screen (Public Browse + Admin Controls if Admin)
                    val baseWorks = works.filter { it.collectionId == currentCol.id }
                    val colWorks = if (currentCol.slug == "photography" && !showFullPhotoArchive && !showAdminScreen) {
                        baseWorks.filter { it.isCurated }
                    } else baseWorks

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(12.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (isAr) currentCol.titleAr else currentCol.titleEn, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextIvory)
                            Text(if (isAr) currentCol.descAr else currentCol.descEn, fontSize = 13.sp, color = MutedGray)
                            if (currentCol.slug == "photography" && !showAdminScreen) {
                                OutlinedButton(onClick = { showFullPhotoArchive = !showFullPhotoArchive }) {
                                    Text(if (showFullPhotoArchive) tr("العودة للمختارات (36)", "Show 36 Selected") else tr("عرض الأرشيف الكامل (${baseWorks.size})", "Full Archive (${baseWorks.size})"))
                                }
                            }
                            if (showAdminScreen && accessToken != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        val token = accessToken ?: return@Button
                                        val nextPub = !currentCol.isPublished
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                val body = JSONObject().put("is_published", nextPub).toString()
                                                val req = Request.Builder().url("$SUPABASE_URL/rest/v1/collections?id=eq.${currentCol.id}").patch(body.toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                client.newCall(req).execute().close()
                                                logAdminAction(token, if (nextPub) "publish_collection" else "hide_collection", "collection", currentCol.slug)
                                                loadPublicData(token)
                                            }
                                        }
                                    }) { Text(if (currentCol.isPublished) tr("إخفاء", "Hide") else tr("نشر", "Publish")) }
                                    OutlinedButton(onClick = { confirmDeleteCol = currentCol }, colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)) {
                                        Text(tr("حذف", "Delete"))
                                    }
                                }
                            }
                        }

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(colWorks) { w ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(10.dp)).clickable {
                                        if (showAdminScreen && accessToken != null) editingWork = w else viewingWork = w
                                    }.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(model = w.thumbUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceDark))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(if (isAr) w.titleAr.ifBlank { w.titleEn } else w.titleEn.ifBlank { w.titleAr }, color = if (w.isPublished) TextIvory else MutedGray, fontWeight = FontWeight.Medium)
                                        Text("${w.origin.uppercase()} · ${w.qualityScore}%", color = MutedGray, fontSize = 11.sp)
                                    }
                                    if (w.isFeatured) Text("★", color = AccentWhite)
                                }
                            }
                        }
                    }
                } else if (showAdminScreen) {
                    // ADMIN MODE
                    if (accessToken == null) {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(tr("دخول الإدارة", "Admin Studio Login"), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text(tr("اسم المستخدم أو البريد الإلكتروني", "Username or Email")) }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text(tr("الرمز السري", "Password")) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                            if (loginError != null) Text(loginError!!, color = DangerRed, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        isLoading = true; loginError = null
                                        val email = when {
                                            username.trim().equals("boda890", ignoreCase = true) -> "bodykhaled5000@gmail.com"
                                            username.trim().equals("admin", ignoreCase = true) -> "ahmedissam.personal@gmail.com"
                                            else -> username.trim()
                                        }
                                        val payload = JSONObject().put("email", email).put("password", password).toString()
                                        withContext(Dispatchers.IO) {
                                            val req = Request.Builder().url("$SUPABASE_URL/auth/v1/token?grant_type=password").post(payload.toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).build()
                                            client.newCall(req).execute().use { resp ->
                                                val json = JSONObject(resp.body?.string() ?: "{}")
                                                if (resp.isSuccessful && json.has("access_token")) {
                                                    val tok = json.getString("access_token")
                                                    currentAdminId = json.optJSONObject("user")?.optString("id")
                                                    currentAdminName = if (email.startsWith("bodykhaled")) "boda890" else "admin"
                                                    accessToken = tok
                                                    loadPublicData(tok)
                                                } else loginError = tr("بيانات الدخول غير صحيحة", "Invalid credentials")
                                            }
                                        }
                                        isLoading = false
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) { Text(if (isLoading) tr("جاري الدخول...", "Signing in...") else tr("تسجيل الدخول", "Sign In")) }
                        }
                    } else {
                        when (adminTab) {
                            0 -> {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(tr("سجل نشاط الأدمن ($currentAdminName)", "Audit Log ($currentAdminName)"), fontWeight = FontWeight.Bold)
                                            TextButton(onClick = { accessToken = null }) { Text(tr("خروج", "Sign Out"), color = DangerRed) }
                                        }
                                    }
                                    items(auditLogs) { log ->
                                        Row(modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(8.dp)).padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${log.adminName} -> ${log.action}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                Text("${log.targetType}: ${log.targetId}", color = MutedGray, fontSize = 11.sp)
                                            }
                                            Text(log.createdAt, color = MutedGray, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                            1 -> {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(works) { w ->
                                        Row(modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(10.dp)).clickable { editingWork = w }.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            AsyncImage(model = w.thumbUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(if (isAr) w.titleAr.ifBlank { w.titleEn } else w.titleEn.ifBlank { w.titleAr }, color = if (w.isPublished) TextIvory else MutedGray)
                                                Text("${w.origin.uppercase()} · ${w.qualityScore}%", color = MutedGray, fontSize = 11.sp)
                                            }
                                            Text(if (w.isFeatured) "★" else "✏️")
                                        }
                                    }
                                }
                            }
                            2 -> {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        collections.forEachIndexed { idx, col ->
                                            FilterChip(selected = selectedColIndex == idx, onClick = { selectedColIndex = idx }, label = { Text(if (isAr) col.titleAr else col.titleEn) })
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("camera" to tr("📷 كاميرا", "Camera"), "ai" to tr("✨ ذكاء", "AI"), "design" to tr("🎨 تصميم", "Design")).forEach { (o, l) ->
                                            FilterChip(selected = selectedOrigin == o, onClick = { selectedOrigin = o }, label = { Text(l) })
                                        }
                                    }
                                    OutlinedTextField(value = uploadTitleAr, onValueChange = { uploadTitleAr = it }, label = { Text(tr("العنوان بالعربي", "Arabic Title")) }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = uploadTitleEn, onValueChange = { uploadTitleEn = it }, label = { Text(tr("العنوان بالإنجليزي", "English Title")) }, modifier = Modifier.fillMaxWidth())
                                    Button(onClick = { picker.launch("image/*") }, enabled = !isLoading, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                                        Text(if (isLoading) tr("جاري الرفع...", "Uploading...") else tr("اختيار صور ورفعها", "Pick Images & Upload"))
                                    }
                                    if (uploadStatus.isNotBlank()) Text(uploadStatus, color = GreenOk)
                                }
                            }
                            3 -> {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    item {
                                        Column(modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(tr("مجموعة جديدة", "New Collection"), fontWeight = FontWeight.Bold)
                                            OutlinedTextField(value = newColSlug, onValueChange = { newColSlug = it }, label = { Text("Slug") }, modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = newColAr, onValueChange = { newColAr = it }, label = { Text(tr("الاسم بالعربي", "Arabic Title")) }, modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = newColEn, onValueChange = { newColEn = it }, label = { Text(tr("الاسم بالإنجليزي", "English Title")) }, modifier = Modifier.fillMaxWidth())
                                            Button(onClick = {
                                                val token = accessToken ?: return@Button
                                                scope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        val slug = newColSlug.trim()
                                                        val body = JSONObject().put("slug", slug).put("title_ar", newColAr.trim()).put("title_en", newColEn.trim()).put("year", 2026).put("sort_order", collections.size + 1).put("is_published", true)
                                                        val req = Request.Builder().url("$SUPABASE_URL/rest/v1/collections").post(body.toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                        client.newCall(req).execute().close()
                                                        logAdminAction(token, "create_collection", "collection", slug)
                                                        loadPublicData(token)
                                                    }
                                                    newColSlug = ""; newColAr = ""; newColEn = ""
                                                }
                                            }, modifier = Modifier.fillMaxWidth()) { Text(tr("إنشاء", "Create")) }
                                        }
                                    }
                                    items(collections) { c ->
                                        Row(modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(10.dp)).clickable { viewingCollection = c }.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(if (isAr) c.titleAr else c.titleEn, fontWeight = FontWeight.SemiBold, color = if (c.isPublished) TextIvory else MutedGray)
                                                Text(if (c.isPublished) tr("منشورة", "Published") else tr("مخفية", "Hidden"), color = MutedGray, fontSize = 11.sp)
                                            }
                                            Row {
                                                TextButton(onClick = {
                                                    val token = accessToken ?: return@TextButton
                                                    val nextPub = !c.isPublished
                                                    scope.launch {
                                                        withContext(Dispatchers.IO) {
                                                            val req = Request.Builder().url("$SUPABASE_URL/rest/v1/collections?id=eq.${c.id}").patch(JSONObject().put("is_published", nextPub).toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                            client.newCall(req).execute().close()
                                                            logAdminAction(token, if (nextPub) "publish_collection" else "hide_collection", "collection", c.slug)
                                                            loadPublicData(token)
                                                        }
                                                    }
                                                }) { Text(if (c.isPublished) tr("إخفاء", "Hide") else tr("نشر", "Publish")) }
                                                TextButton(onClick = { confirmDeleteCol = c }) { Text(tr("حذف", "Del"), color = DangerRed) }
                                            }
                                        }
                                    }
                                }
                            }
                            4 -> {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    item {
                                        Column(modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(tr("نشر تحديث في عقلي (Live Mind)", "Publish Live Mind Note"), fontWeight = FontWeight.Bold)
                                            OutlinedTextField(value = newNoteAr, onValueChange = { newNoteAr = it }, label = { Text(tr("العنوان بالعربي", "Arabic Title")) }, modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = newNoteEn, onValueChange = { newNoteEn = it }, label = { Text(tr("العنوان بالإنجليزي", "English Title")) }, modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = newNoteBodyAr, onValueChange = { newNoteBodyAr = it }, label = { Text(tr("النص بالعربي", "Arabic Body")) }, modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = newNoteBodyEn, onValueChange = { newNoteBodyEn = it }, label = { Text(tr("النص بالإنجليزي", "English Body")) }, modifier = Modifier.fillMaxWidth())
                                            Button(onClick = {
                                                val token = accessToken ?: return@Button
                                                scope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        val slug = "note-${System.currentTimeMillis()}"
                                                        val body = JSONObject().put("slug", slug).put("category", "now").put("title_ar", newNoteAr.trim()).put("title_en", newNoteEn.trim()).put("body_ar", newNoteBodyAr.trim()).put("body_en", newNoteBodyEn.trim()).put("sort_order", mindNotes.size + 1)
                                                        val req = Request.Builder().url("$SUPABASE_URL/rest/v1/mind_notes").post(body.toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                        client.newCall(req).execute().close()
                                                        logAdminAction(token, "publish_mind_note", "mind_note", slug)
                                                        loadPublicData(token)
                                                    }
                                                    newNoteAr = ""; newNoteEn = ""; newNoteBodyAr = ""; newNoteBodyEn = ""
                                                }
                                            }, modifier = Modifier.fillMaxWidth()) { Text(tr("نشر الآن", "Publish")) }
                                        }
                                    }
                                    item { Text(tr("الرسائل الواردة (${inquiries.size})", "Client Inquiries (${inquiries.size})"), fontWeight = FontWeight.Bold) }
                                    items(inquiries) { inq ->
                                        Row(modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(10.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${inq.senderName} (${inq.senderContact})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                Text(inq.message, color = MutedGray, fontSize = 12.sp)
                                            }
                                            TextButton(onClick = {
                                                val token = accessToken ?: return@TextButton
                                                scope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        val req = Request.Builder().url("$SUPABASE_URL/rest/v1/inquiries?id=eq.${inq.id}").patch(JSONObject().put("is_resolved", !inq.isResolved).toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                        client.newCall(req).execute().close()
                                                        loadPublicData(token)
                                                    }
                                                }
                                            }) { Text(if (inq.isResolved) tr("تم", "Done") else tr("إنهاء", "Resolve")) }
                                        }
                                    }
                                    item { Text(tr("التعليقات (${comments.size})", "Comments (${comments.size})"), fontWeight = FontWeight.Bold) }
                                    items(comments) { c ->
                                        Row(modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(10.dp)).padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${c.author} · ${c.workTitle}", color = MutedGray, fontSize = 11.sp)
                                                Text(c.body, color = if (c.isHidden) MutedGray else TextIvory)
                                            }
                                            TextButton(onClick = {
                                                val token = accessToken ?: return@TextButton
                                                scope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        val req = Request.Builder().url("$SUPABASE_URL/rest/v1/comments?id=eq.${c.id}").patch(JSONObject().put("is_hidden", !c.isHidden).toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                        client.newCall(req).execute().close()
                                                        loadPublicData(token)
                                                    }
                                                }
                                            }) { Text(if (c.isHidden) tr("إظهار", "Show") else tr("إخفاء", "Hide")) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // PUBLIC ENCYCLOPEDIC ARTELLIGENCE APP
                    when (publicTab) {
                        0 -> {
                            // 01: Mind & Ecosystem
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(16.dp)).padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(model = HEADSHOT_URL, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(72.dp).clip(CircleShape))
                                        Column {
                                            Text(tr("أحمد عصام رمضان", "Ahmed Issam Ramadan"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            Text(tr("مستشار تحول الأعمال والأنظمة الذكية · مؤسس Artelligence", "AI Transformation Consultant · Founder of Artelligence"), color = MutedGray, fontSize = 12.sp)
                                        }
                                    }
                                }
                                item {
                                    Text(tr("أفكاري الحية ومبادئ الشغل", "Inside My Mind & Live Principles"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                                items(mindNotes) { n ->
                                    Column(modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(12.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(n.category.uppercase(), color = MutedGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(if (isAr) n.titleAr else n.titleEn, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(if (isAr) n.bodyAr else n.bodyEn, color = MutedGray, fontSize = 13.sp)
                                    }
                                }
                                item {
                                    Column(modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(12.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(tr("منصات منظومة Artelligence", "Artelligence Ecosystem"), fontWeight = FontWeight.Bold)
                                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(
                                                "WASL Network" to "https://wasl-network.com",
                                                "Artelligence OS" to "https://ahmedissamramadan.github.io/Artelligence-OS/",
                                                "ABTI" to "https://ahmedissamramadan.github.io/ai-business-transformation/",
                                                "AI Pyramid" to "https://ahmedissamramadan.github.io/ai-pyramid/"
                                            ).forEach { (lbl, url) ->
                                                OutlinedButton(onClick = { openUrl(url) }) { Text("$lbl ↗", fontSize = 12.sp) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // 02: Work (Case Studies + Categorized Collections)
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(selected = workViewMode == 0, onClick = { workViewMode = 0 }, label = { Text(tr("دراسات الحالة (${caseStudies.size})", "Case Studies (${caseStudies.size})")) })
                                    FilterChip(selected = workViewMode == 1, onClick = { workViewMode = 1 }, label = { Text(tr("المجموعات (${collections.size})", "Collections (${collections.size})")) })
                                }
                                if (workViewMode == 0) {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(caseStudies) { cs ->
                                            Column(modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                if (cs.coverUrl.isNotBlank()) {
                                                    AsyncImage(model = cs.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp)))
                                                }
                                                Text(if (isAr) cs.sectorAr else cs.sectorEn, color = MutedGray, fontSize = 11.sp)
                                                Text(if (isAr) cs.titleAr else cs.titleEn, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                Text("${tr("المشكلة:", "Problem:")} ${if (isAr) cs.problemAr else cs.problemEn}", fontSize = 12.sp, color = MutedGray)
                                                Text("${tr("الحل:", "Solution:")} ${if (isAr) cs.solutionAr else cs.solutionEn}", fontSize = 12.sp, color = MutedGray)
                                                Text("${tr("النتيجة:", "Outcome:")} ${if (isAr) cs.outcomeAr else cs.outcomeEn}", fontSize = 12.sp, color = TextIvory)
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    if (cs.collectionSlug.isNotBlank()) {
                                                        Button(onClick = { viewingCollection = collections.find { it.slug == cs.collectionSlug } }) {
                                                            Text(tr("معاينة الأصول", "View Assets"), fontSize = 12.sp)
                                                        }
                                                    }
                                                    if (cs.externalUrl.isNotBlank()) {
                                                        OutlinedButton(onClick = { openUrl(cs.externalUrl) }) { Text(tr("فتح ↗", "Open ↗"), fontSize = 12.sp) }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        listOf("client" to tr("شغل العملاء والأنظمة", "Client Work"), "personal" to tr("مشاريع فنية وذكاء اصطناعي", "Personal & AI"), "educational" to tr("محتوى توعوي", "Educational")).forEach { (cat, header) ->
                                            val catCols = collections.filter { it.category == cat }
                                            if (catCols.isNotEmpty()) {
                                                item { Text(header, fontWeight = FontWeight.Bold, color = MutedGray, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
                                                items(catCols) { c ->
                                                    val cnt = works.count { it.collectionId == c.id && (c.slug != "photography" || it.isCurated) }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(12.dp)).clickable { viewingCollection = c }.padding(14.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(if (isAr) c.titleAr else c.titleEn, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                            Text("${if (isAr) c.mediumAr else c.mediumEn} · $cnt ${tr("عمل", "works")}", color = MutedGray, fontSize = 11.sp)
                                                            Text(if (isAr) c.descAr else c.descEn, color = MutedGray, fontSize = 12.sp, maxLines = 2)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // 03: Human or AI Challenge
                            if (gameRounds.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(tr("جاري التحميل...", "Loading...")) }
                            } else if (gameIdx >= gameRounds.size) {
                                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(tr("النتيجة النهائية", "Final Score"), color = MutedGray)
                                    Text("$gameScore / ${gameRounds.size}", fontSize = 44.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(16.dp))
                                    Button(onClick = { startChallenge(works) }) { Text(tr("العب مرة أخرى", "Play Again")) }
                                }
                            } else {
                                val w = gameRounds[gameIdx]
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${tr("جولة", "Round")} ${gameIdx + 1}/${gameRounds.size}", color = MutedGray)
                                        Text("${tr("النتيجة:", "Score:")} $gameScore", fontWeight = FontWeight.Bold)
                                    }
                                    AsyncImage(model = w.fullUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(14.dp)).background(SurfaceDark))
                                    if (gameAnswered == null) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Button(onClick = {
                                                val ok = w.origin == "camera"; if (ok) gameScore++; gameAnswered = ok
                                            }, modifier = Modifier.weight(1f)) { Text(tr("📷 كاميرا", "📷 Camera")) }
                                            Button(onClick = {
                                                val ok = w.origin == "ai"; if (ok) gameScore++; gameAnswered = ok
                                            }, modifier = Modifier.weight(1f)) { Text(tr("✨ ذكاء اصطناعي", "✨ AI")) }
                                        }
                                    } else {
                                        Text(if (gameAnswered == true) tr("إجابة صحيحة ✓", "Correct ✓") else tr("إجابة خاطئة ✕", "Wrong ✕"), color = if (gameAnswered == true) GreenOk else DangerRed, fontWeight = FontWeight.Bold)
                                        Button(onClick = { gameIdx++; gameAnswered = null }, modifier = Modifier.fillMaxWidth()) { Text(tr("التالي", "Next")) }
                                    }
                                }
                            }
                        }
                        3 -> {
                            // 04: Direct Connect & Inquiry
                            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(tr("تواصل معي مباشرة", "Connect Directly"), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(tr("مكالمة استكشافية 15 دقيقة لمشروعك أو أتمتة شركتك.", "15-minute discovery call for your SME automation or brand system."), color = MutedGray, fontSize = 13.sp)
                                Button(onClick = { openUrl("https://wa.me/201501656010") }, modifier = Modifier.fillMaxWidth()) {
                                    Text(tr("💬 واتساب (+20 150 165 6010)", "💬 WhatsApp (+20 150 165 6010)"))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { openUrl("mailto:ahmedissam.work@gmail.com") }, modifier = Modifier.weight(1f)) { Text("Email ↗", fontSize = 12.sp) }
                                    OutlinedButton(onClick = { openUrl("https://www.linkedin.com/in/ahmdesam96") }, modifier = Modifier.weight(1f)) { Text("LinkedIn ↗", fontSize = 12.sp) }
                                }
                                HorizontalDivider(color = SurfaceCard)
                                Text(tr("أو أرسل رسالة سريعة من هنا:", "Or send an in-app brief:"), fontWeight = FontWeight.Bold)
                                OutlinedTextField(value = inqName, onValueChange = { inqName = it }, label = { Text(tr("الاسم", "Your Name")) }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = inqContact, onValueChange = { inqContact = it }, label = { Text(tr("الإيميل أو الواتساب", "Email or WhatsApp")) }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = inqMsg, onValueChange = { inqMsg = it }, label = { Text(tr("تفاصيل رسالتك", "Message")) }, modifier = Modifier.fillMaxWidth())
                                Button(
                                    onClick = {
                                        if (inqName.isBlank() || inqContact.isBlank() || inqMsg.isBlank()) return@Button
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                val body = JSONObject().put("sender_name", inqName.trim()).put("sender_contact", inqContact.trim()).put("message", inqMsg.trim())
                                                val req = Request.Builder().url("$SUPABASE_URL/rest/v1/inquiries").post(body.toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).build()
                                                client.newCall(req).execute().close()
                                            }
                                            inqName = ""; inqContact = ""; inqMsg = ""
                                            inqStatus = tr("تم إرسال رسالتك بنجاح!", "Brief sent successfully!")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(tr("إرسال الرسالة", "Send Brief")) }
                                if (inqStatus.isNotBlank()) Text(inqStatus, color = GreenOk, fontSize = 13.sp)
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
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AccentWhite)
        Text(label, fontSize = 12.sp, color = MutedGray)
    }
}
