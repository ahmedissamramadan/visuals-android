package com.ahmedramadan.visuals

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.border
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

private val DangerRed = Color(0xFFEF4444)
private val GreenOk = Color(0xFF10B981)

data class ArtPalette(
    val bg: Color,
    val surface: Color,
    val card: Color,
    val border: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color
)

private val ObsidianPalette = ArtPalette(
    bg = Color(0xFF0B0B0B),
    surface = Color(0xFF141416),
    card = Color(0xFF1C1C20),
    border = Color(0xFF27272A),
    text = Color(0xFFFFFFFF),
    muted = Color(0xFFA1A1AA),
    accent = Color(0xFFFFFFFF),
    onAccent = Color(0xFF0B0B0B)
)

private val AcidPalette = ArtPalette(
    bg = Color(0xFF181C19),
    surface = Color(0xFF242C25),
    card = Color(0xFF2D372E),
    border = Color(0xFF39463B),
    text = Color(0xFFEFF3E7),
    muted = Color(0xFFB6C2B7),
    accent = Color(0xFFD5FF5F),
    onAccent = Color(0xFF181C19)
)

private val PaperPalette = ArtPalette(
    bg = Color(0xFFF6F5F1),
    surface = Color(0xFFFFFFFF),
    card = Color(0xFFF2E8DA),
    border = Color(0xFFE2D9CB),
    text = Color(0xFF0B0B0B),
    muted = Color(0xFF52525B),
    accent = Color(0xFF0B0B0B),
    onAccent = Color(0xFFFFFFFF)
)

private val CobaltPalette = ArtPalette(
    bg = Color(0xFFEEF1FF),
    surface = Color(0xFFFFFFFF),
    card = Color(0xFFE2E8FF),
    border = Color(0xFFC7D2FE),
    text = Color(0xFF0B0B0B),
    muted = Color(0xFF434B69),
    accent = Color(0xFF244BEE),
    onAccent = Color(0xFFFFFFFF)
)

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
    val anonTitleAr: String,
    val anonTitleEn: String,
    val isAnonymized: Boolean,
    val isIllustrative: Boolean,
    val isPublished: Boolean,
    val sectorAr: String,
    val sectorEn: String,
    val problemAr: String,
    val problemEn: String,
    val solutionAr: String,
    val solutionEn: String,
    val outcomeAr: String,
    val outcomeEn: String,
    val deliverablesAr: List<String>,
    val deliverablesEn: List<String>,
    val evidenceNoteAr: String,
    val evidenceNoteEn: String,
    val collectionSlug: String,
    val externalUrl: String,
    val coverUrl: String
)

data class ServiceOfferItem(
    val id: String,
    val slug: String,
    val badgeAr: String,
    val badgeEn: String,
    val titleAr: String,
    val titleEn: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val priceEgp: String,
    val priceUsd: String,
    val deliverablesAr: List<String>,
    val deliverablesEn: List<String>
)

data class DigitalProductItem(
    val id: String,
    val slug: String,
    val titleAr: String,
    val titleEn: String,
    val descAr: String,
    val descEn: String,
    val badgeAr: String,
    val badgeEn: String,
    val liveUrl: String
)

data class CrmDealItem(
    val id: String,
    val companyName: String,
    val decisionMaker: String,
    val sector: String,
    val stage: String,
    val nextActionAr: String,
    val proposedEgp: Double,
    val signedAmountEgp: Double,
    val signedMrrUsd: Double,
    val isSigned: Boolean
)

data class TeardownReqItem(
    val id: String,
    val senderName: String,
    val companyName: String,
    val url: String,
    val salesModel: String,
    val channel: String,
    val contact: String,
    val status: String
)

data class ContentItem(
    val id: String,
    val titleAr: String,
    val hookAr: String,
    val sourceProof: String,
    val currentGate: Int,
    val status: String
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

private fun jsonArrayToStringList(arr: JSONArray?): List<String> {
    if (arr == null) return emptyList()
    val out = mutableListOf<String>()
    for (i in 0 until arr.length()) out.add(arr.optString(i, ""))
    return out.filter { it.isNotBlank() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 0=Obsidian, 1=Acid (#D5FF5F), 2=Warm Paper, 3=Cobalt (#244BEE)
            var themeIdx by remember { mutableStateOf(0) }
            val themeNames = listOf("Obsidian", "Acid", "Paper", "Cobalt")
            val pal = when (themeIdx) {
                1 -> AcidPalette
                2 -> PaperPalette
                3 -> CobaltPalette
                else -> ObsidianPalette
            }
            val isDark = themeIdx == 0 || themeIdx == 1
            val scheme = if (isDark) {
                darkColorScheme(
                    background = pal.bg,
                    surface = pal.surface,
                    primary = pal.accent,
                    onPrimary = pal.onAccent,
                    onBackground = pal.text,
                    onSurface = pal.text
                )
            } else {
                lightColorScheme(
                    background = pal.bg,
                    surface = pal.surface,
                    primary = pal.accent,
                    onPrimary = pal.onAccent,
                    onBackground = pal.text,
                    onSurface = pal.text
                )
            }

            MaterialTheme(colorScheme = scheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = pal.bg) {
                    ArtelligenceApp(
                        pal = pal,
                        themeLabel = themeNames[themeIdx],
                        onCycleTheme = { themeIdx = (themeIdx + 1) % 4 }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtelligenceApp(pal: ArtPalette, themeLabel: String, onCycleTheme: () -> Unit) {
    val client = remember { OkHttpClient() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var isAr by remember { mutableStateOf(true) }
    fun tr(ar: String, en: String) = if (isAr) ar else en

    // Cloud Data
    var collections by remember { mutableStateOf<List<CollectionItem>>(emptyList()) }
    var works by remember { mutableStateOf<List<WorkItem>>(emptyList()) }
    var caseStudies by remember { mutableStateOf<List<CaseStudyItem>>(emptyList()) }
    var mindNotes by remember { mutableStateOf<List<MindNoteItem>>(emptyList()) }
    var offers by remember { mutableStateOf<List<ServiceOfferItem>>(emptyList()) }
    var products by remember { mutableStateOf<List<DigitalProductItem>>(emptyList()) }

    // Public Navigation: 0=Home, 1=Solutions, 2=Proof, 3=Mind, 4=Lab
    var publicTab by remember { mutableStateOf(0) }
    var proofSubTab by remember { mutableStateOf(0) } // 0=CaseStudies, 1=Collections
    var mindSubTab by remember { mutableStateOf(0) } // 0=MindNotes, 1=AIPyramid, 2=Academy
    var labSubTab by remember { mutableStateOf(0) } // 0=Quiz, 1=Readiness, 2=Connect

    var viewingCollection by remember { mutableStateOf<CollectionItem?>(null) }
    var viewingWork by remember { mutableStateOf<WorkItem?>(null) }
    var showFullPhotoArchive by remember { mutableStateOf(false) }

    // Solutions & Calculator State
    var useUsd by remember { mutableStateOf(false) }
    var missedWeekly by remember { mutableStateOf(12f) }
    var avgOrderVal by remember { mutableStateOf("15000") }
    var tdName by remember { mutableStateOf("") }
    var tdCompany by remember { mutableStateOf("") }
    var tdUrl by remember { mutableStateOf("") }
    var tdContact by remember { mutableStateOf("") }
    var tdFeedback by remember { mutableStateOf("") }

    // AI Pyramid Self-Reflection
    val pyrScores = remember { mutableStateListOf(4f, 4f, 4f, 4f, 4f) }

    // Business Readiness Check
    val readinessChecks = remember { mutableStateListOf(false, false, false, false, false, false) }

    // Challenge (Human or AI?)
    var gameRounds by remember { mutableStateOf<List<WorkItem>>(emptyList()) }
    var gameIdx by remember { mutableStateOf(0) }
    var gameScore by remember { mutableStateOf(0) }
    var gameAnswered by remember { mutableStateOf<Boolean?>(null) }

    // Direct Connect Form
    var inqName by remember { mutableStateOf("") }
    var inqContact by remember { mutableStateOf("") }
    var inqMsg by remember { mutableStateOf("") }
    var inqStatus by remember { mutableStateOf("") }

    // Admin Studio State
    var showAdminScreen by remember { mutableStateOf(false) }
    var accessToken by remember { mutableStateOf<String?>(null) }
    var currentAdminId by remember { mutableStateOf<String?>(null) }
    var currentAdminName by remember { mutableStateOf("admin") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Admin Tabs: 0=CRM, 1=Proof&Sets, 2=Works&Upload, 3=Content4Gate, 4=Inbox&Audit
    var adminTab by remember { mutableStateOf(0) }
    var editingWork by remember { mutableStateOf<WorkItem?>(null) }
    var confirmDeleteCol by remember { mutableStateOf<CollectionItem?>(null) }
    var crmDeals by remember { mutableStateOf<List<CrmDealItem>>(emptyList()) }
    var teardownReqs by remember { mutableStateOf<List<TeardownReqItem>>(emptyList()) }
    var contentItems by remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    var comments by remember { mutableStateOf<List<CommentItem>>(emptyList()) }
    var inquiries by remember { mutableStateOf<List<InquiryItem>>(emptyList()) }
    var auditLogs by remember { mutableStateOf<List<AuditLogItem>>(emptyList()) }

    // Admin Forms
    var newCrmComp by remember { mutableStateOf("") }
    var newCrmPerson by remember { mutableStateOf("") }
    var newCrmEgp by remember { mutableStateOf("40000") }

    var newCpTitle by remember { mutableStateOf("") }
    var newCpHook by remember { mutableStateOf("") }
    var newCpProof by remember { mutableStateOf("") }
    var cpStatusMsg by remember { mutableStateOf("") }

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
    var toastMsg by remember { mutableStateOf("") }

    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {}
    }

    fun copyAndShareText(text: String) {
        try {
            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cb.setPrimaryClip(ClipData.newPlainText("Artelligence", text))
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Artelligence"))
        } catch (_: Exception) {}
    }

    suspend fun notifyLeadEdge(kind: String, payload: JSONObject) {
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().put("kind", kind).put("payload", payload).toString()
                val req = Request.Builder()
                    .url("$SUPABASE_URL/functions/v1/notify-lead")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    .build()
                client.newCall(req).execute().close()
            } catch (_: Exception) {}
        }
    }

    fun startChallenge(pool: List<WorkItem>) {
        val cams = pool.filter { it.isPublished && it.origin == "camera" }.shuffled().take(5)
        val ais = pool.filter { it.isPublished && it.origin == "ai" }.shuffled().take(5)
        gameRounds = (cams + ais).shuffled()
        gameIdx = 0
        gameScore = 0
        gameAnswered = null
    }

    suspend fun loadAllData(token: String? = accessToken) {
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

            val csFilter = if (token != null) "" else "&is_published=eq.true"
            val csArr = getJson("case_studies?select=*$csFilter&order=sort_order")
            val csList = mutableListOf<CaseStudyItem>()
            for (i in 0 until csArr.length()) {
                val o = csArr.getJSONObject(i)
                csList.add(CaseStudyItem(
                    o.getString("id"), o.getString("slug"),
                    o.getString("title_ar"), o.getString("title_en"),
                    o.optString("anon_title_ar", ""), o.optString("anon_title_en", ""),
                    o.optBoolean("is_anonymized", false), o.optBoolean("is_illustrative", false),
                    o.optBoolean("is_published", true),
                    o.getString("sector_ar"), o.getString("sector_en"),
                    o.getString("problem_ar"), o.getString("problem_en"),
                    o.getString("solution_ar"), o.getString("solution_en"),
                    o.getString("outcome_ar"), o.getString("outcome_en"),
                    jsonArrayToStringList(o.optJSONArray("deliverables_ar")),
                    jsonArrayToStringList(o.optJSONArray("deliverables_en")),
                    o.optString("evidence_note_ar", ""), o.optString("evidence_note_en", ""),
                    o.optString("collection_slug", ""), o.optString("external_url", ""), o.optString("cover_url", "")
                ))
            }
            caseStudies = csList

            val offArr = getJson("services_offers?select=*&is_active=eq.true&order=step_order")
            val offList = mutableListOf<ServiceOfferItem>()
            for (i in 0 until offArr.length()) {
                val o = offArr.getJSONObject(i)
                offList.add(ServiceOfferItem(
                    o.getString("id"), o.getString("slug"),
                    o.optString("badge_ar", ""), o.optString("badge_en", ""),
                    o.getString("title_ar"), o.getString("title_en"),
                    o.optString("subtitle_ar", ""), o.optString("subtitle_en", ""),
                    o.optString("price_egp_label", ""), o.optString("price_usd_label", ""),
                    jsonArrayToStringList(o.optJSONArray("deliverables_ar")),
                    jsonArrayToStringList(o.optJSONArray("deliverables_en"))
                ))
            }
            offers = offList

            val prodArr = getJson("digital_products?select=*&is_published=eq.true&order=sort_order")
            val prodList = mutableListOf<DigitalProductItem>()
            for (i in 0 until prodArr.length()) {
                val o = prodArr.getJSONObject(i)
                prodList.add(DigitalProductItem(
                    o.getString("id"), o.getString("slug"),
                    o.getString("title_ar"), o.getString("title_en"),
                    o.getString("description_ar"), o.getString("description_en"),
                    o.optString("stats_badge_ar", ""), o.optString("stats_badge_en", ""),
                    o.optString("live_url", "")
                ))
            }
            products = prodList

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
                val crmArr = getJson("crm_deals?select=*&order=created_at.desc")
                val crmList = mutableListOf<CrmDealItem>()
                for (i in 0 until crmArr.length()) {
                    val o = crmArr.getJSONObject(i)
                    crmList.add(CrmDealItem(
                        o.getString("id"), o.getString("company_name"),
                        o.optString("decision_maker", ""), o.optString("sector", "factories"),
                        o.optString("stage", "queued"), o.optString("next_action_ar", ""),
                        o.optDouble("proposed_egp", 40000.0),
                        o.optDouble("signed_amount_egp", 0.0),
                        o.optDouble("signed_mrr_usd", 0.0),
                        o.optBoolean("is_signed", false)
                    ))
                }
                crmDeals = crmList

                val tdArr = getJson("teardown_requests?select=*&order=created_at.desc")
                val tdList = mutableListOf<TeardownReqItem>()
                for (i in 0 until tdArr.length()) {
                    val o = tdArr.getJSONObject(i)
                    tdList.add(TeardownReqItem(
                        o.getString("id"), o.getString("sender_name"),
                        o.optString("company_name", ""), o.getString("account_or_site_url"),
                        o.optString("sales_model", "b2b"), o.optString("primary_channel", "whatsapp"),
                        o.getString("contact_info"), o.optString("status", "new")
                    ))
                }
                teardownReqs = tdList

                val cpArr = getJson("content_pipeline?select=*&order=created_at.desc")
                val cpList = mutableListOf<ContentItem>()
                for (i in 0 until cpArr.length()) {
                    val o = cpArr.getJSONObject(i)
                    cpList.add(ContentItem(
                        o.getString("id"), o.getString("title_ar"),
                        o.optString("hook_ar", ""), o.optString("source_proof", ""),
                        o.optInt("current_gate", 1), o.optString("status", "draft")
                    ))
                }
                contentItems = cpList

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
        loadAllData(null)
        withContext(Dispatchers.IO) {
            try {
                val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVer = pkgInfo.versionName ?: "2.0.0"
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
                            for (i in 0 until assets.length()) {
                                val aObj = assets.getJSONObject(i)
                                if (aObj.optString("name", "") == "Artelligence.apk") {
                                    dlUrl = aObj.optString("browser_download_url", dlUrl)
                                    break
                                }
                            }
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
                loadAllData(token)
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
                text = { Text(tr("هل أنت متأكد من حذف \"${delCol.titleAr}\" نهائياً؟ سيتم تسجيل العملية في سجل التدقيق.", "Permanently delete \"${delCol.titleEn}\"?")) },
                confirmButton = {
                    TextButton(onClick = {
                        val token = accessToken ?: return@TextButton
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val req = Request.Builder().url("$SUPABASE_URL/rest/v1/collections?id=eq.${delCol.id}").delete().addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                client.newCall(req).execute().close()
                                logAdminAction(token, "delete_collection", "collection", delCol.slug)
                                loadAllData(token)
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
            containerColor = pal.bg,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = pal.surface),
                    navigationIcon = {
                        when {
                            editingWork != null -> TextButton(onClick = { editingWork = null }) { Text(tr("رجوع", "Back"), color = pal.text) }
                            viewingWork != null -> TextButton(onClick = { viewingWork = null }) { Text(tr("رجوع", "Back"), color = pal.text) }
                            viewingCollection != null -> TextButton(onClick = { viewingCollection = null; showFullPhotoArchive = false }) { Text(tr("رجوع", "Back"), color = pal.text) }
                            showAdminScreen -> TextButton(onClick = { showAdminScreen = false }) { Text(tr("المنصة", "Exit"), color = pal.text) }
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Image(painter = painterResource(R.drawable.ic_artelligence_symbol), contentDescription = null, modifier = Modifier.size(28.dp).clip(CircleShape))
                            Text(
                                if (showAdminScreen) tr("مركز القيادة", "Command Studio") else "Artelligence",
                                color = pal.text, fontWeight = FontWeight.Bold, fontSize = 17.sp
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = onCycleTheme) { Text(themeLabel, color = pal.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        TextButton(onClick = { isAr = !isAr }) { Text(if (isAr) "EN" else "عربي", color = pal.text, fontSize = 11.sp) }
                        IconButton(onClick = { scope.launch { loadAllData(accessToken) } }) {
                            Icon(painter = painterResource(R.drawable.ic_art_refresh), contentDescription = "Sync", tint = pal.text, modifier = Modifier.size(22.dp))
                        }
                        IconButton(onClick = { showAdminScreen = !showAdminScreen }) {
                            Icon(painter = painterResource(R.drawable.ic_art_settings), contentDescription = "Studio", tint = if (showAdminScreen) pal.accent else pal.text, modifier = Modifier.size(22.dp))
                        }
                    }
                )
            },
            bottomBar = {
                if (!showAdminScreen && viewingWork == null && viewingCollection == null) {
                    NavigationBar(containerColor = pal.surface) {
                        val tabs = listOf(
                            R.drawable.ic_art_home to tr("الرئيسية", "Home"),
                            R.drawable.ic_art_automation to tr("الحلول", "Solutions"),
                            R.drawable.ic_art_work to tr("الأدلة", "Proof"),
                            R.drawable.ic_art_knowledge to tr("عقلي", "Mind"),
                            R.drawable.ic_art_agents to tr("المختبر", "Lab")
                        )
                        tabs.forEachIndexed { idx, (icoRes, lbl) ->
                            val isSel = publicTab == idx
                            NavigationBarItem(
                                selected = isSel,
                                onClick = { publicTab = idx },
                                icon = { Icon(painter = painterResource(icoRes), contentDescription = lbl, tint = if (isSel) pal.accent else pal.muted, modifier = Modifier.size(22.dp)) },
                                label = { Text(lbl, fontSize = 10.sp) }
                            )
                        }
                    }
                } else if (showAdminScreen && accessToken != null && editingWork == null && viewingCollection == null) {
                    NavigationBar(containerColor = pal.surface) {
                        val tabs = listOf(
                            R.drawable.ic_art_roi to "CRM",
                            R.drawable.ic_art_work to tr("أدلة/مجموعات", "Sets"),
                            R.drawable.ic_art_creative to tr("أعمال/رفع", "Works"),
                            R.drawable.ic_art_strategy to tr("المحتوى", "Content"),
                            R.drawable.ic_art_clients to tr("رسائل/سجل", "Inbox")
                        )
                        tabs.forEachIndexed { idx, (icoRes, lbl) ->
                            val isSel = adminTab == idx
                            NavigationBarItem(
                                selected = isSel,
                                onClick = { adminTab = idx },
                                icon = { Icon(painter = painterResource(icoRes), contentDescription = lbl, tint = if (isSel) pal.accent else pal.muted, modifier = Modifier.size(22.dp)) },
                                label = { Text(lbl, fontSize = 9.sp) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(14.dp)) {
                if (updateVersion != null && updateApkUrl != null) {
                    Button(
                        onClick = { openUrl(updateApkUrl!!) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text(tr("تحديث جديد متاح (v$updateVersion) - اضغط للتثبيت المباشر", "Update v$updateVersion available - Tap to install")) }
                }
                if (toastMsg.isNotBlank()) {
                    Text(toastMsg, color = GreenOk, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                }

                val currentEdit = editingWork
                val currentPreview = viewingWork
                val currentCol = viewingCollection

                if (currentEdit != null && accessToken != null) {
                    var editAr by remember(currentEdit.id) { mutableStateOf(currentEdit.titleAr) }
                    var editEn by remember(currentEdit.id) { mutableStateOf(currentEdit.titleEn) }
                    var editOrigin by remember(currentEdit.id) { mutableStateOf(currentEdit.origin) }
                    var editColId by remember(currentEdit.id) { mutableStateOf(currentEdit.collectionId) }
                    var editPub by remember(currentEdit.id) { mutableStateOf(currentEdit.isPublished) }
                    var editFeat by remember(currentEdit.id) { mutableStateOf(currentEdit.isFeatured) }

                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(model = currentEdit.fullUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(12.dp)).background(pal.surface))
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
                            FilterChip(selected = editPub, onClick = { editPub = !editPub }, label = { Text(if (editPub) tr("منشور ✓", "Live ✓") else tr("مخفي", "Hidden")) })
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
                                        loadAllData(token)
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
                                        loadAllData(token)
                                    }
                                    editingWork = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                        ) { Text(tr("حذف نهائي", "Delete Permanently")) }
                    }
                } else if (currentPreview != null) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(model = currentPreview.fullUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF050507)))
                        Text(if (isAr) currentPreview.titleAr.ifBlank { currentPreview.titleEn } else currentPreview.titleEn.ifBlank { currentPreview.titleAr }, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = pal.text)
                        Text("${currentPreview.origin.uppercase()} · ${currentPreview.qualityScore}%", color = pal.muted, fontSize = 13.sp)
                        if (accessToken != null && showAdminScreen) {
                            Button(onClick = { editingWork = currentPreview; viewingWork = null }, modifier = Modifier.fillMaxWidth()) {
                                Text(tr("تعديل هذا العمل", "Edit Work"))
                            }
                        }
                    }
                } else if (currentCol != null) {
                    val baseWorks = works.filter { it.collectionId == currentCol.id }
                    val colWorks = if (currentCol.slug == "photography" && !showFullPhotoArchive && !showAdminScreen) {
                        baseWorks.filter { it.isCurated }
                    } else baseWorks

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(12.dp)).border(1.dp, pal.border, RoundedCornerShape(12.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (isAr) currentCol.titleAr else currentCol.titleEn, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = pal.text)
                            Text(if (isAr) currentCol.descAr else currentCol.descEn, fontSize = 13.sp, color = pal.muted)
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
                                                loadAllData(token)
                                            }
                                        }
                                    }) { Text(if (currentCol.isPublished) tr("إخفاء المجموعة", "Hide") else tr("نشر المجموعة", "Publish")) }
                                    OutlinedButton(onClick = { confirmDeleteCol = currentCol }, colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)) {
                                        Text(tr("حذف", "Delete"))
                                    }
                                }
                            }
                        }

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(colWorks) { w ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(10.dp)).border(1.dp, pal.border, RoundedCornerShape(10.dp)).clickable {
                                        if (showAdminScreen && accessToken != null) editingWork = w else viewingWork = w
                                    }.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(model = w.thumbUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(pal.card))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(if (isAr) w.titleAr.ifBlank { w.titleEn } else w.titleEn.ifBlank { w.titleAr }, color = if (w.isPublished) pal.text else pal.muted, fontWeight = FontWeight.Medium)
                                        Text("${w.origin.uppercase()} · ${w.qualityScore}%", color = pal.muted, fontSize = 11.sp)
                                    }
                                    if (w.isFeatured) Text("★", color = pal.accent)
                                }
                            }
                        }
                    }
                } else if (showAdminScreen) {
                    // EXECUTIVE COMMAND STUDIO
                    if (accessToken == null) {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(tr("دخول مركز القيادة", "Command Studio Sign-In"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = pal.text)
                            Spacer(Modifier.height(6.dp))
                            Text(tr("مخصص لإدارة الـ Mini-CRM والتشريح والمحتوى والمجموعات", "Authorized administrators only · Full audit trail active"), fontSize = 12.sp, color = pal.muted)
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text(tr("البريد الإلكتروني أو المعرّف", "Email or Handle")) }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text(tr("كلمة المرور", "Password")) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
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
                                                    loadAllData(tok)
                                                } else loginError = tr("بيانات الدخول غير صحيحة", "Invalid credentials")
                                            }
                                        }
                                        isLoading = false
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) { Text(if (isLoading) tr("جاري التحقق...", "Verifying...") else tr("تسجيل الدخول الآمن", "Secure Sign In")) }
                        }
                    } else {
                        when (adminTab) {
                            0 -> {
                                // TAB 0: Honest Mini-CRM & Teardown Queue
                                val signedEgp = crmDeals.filter { it.isSigned }.sumOf { it.signedAmountEgp }.toLong()
                                val signedUsd = crmDeals.filter { it.isSigned }.sumOf { it.signedMrrUsd }.toLong()
                                val stages = listOf("queued", "teardown_sent", "followup_day3", "followup_day7", "call_15m_booked", "proposal_sent", "signed", "paused")

                                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    item {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(tr("الـ Mini-CRM وماكينة التشريح ($currentAdminName)", "Mini-CRM & Teardowns ($currentAdminName)"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            TextButton(onClick = { accessToken = null }) { Text(tr("خروج", "Sign Out"), color = DangerRed) }
                                        }
                                    }
                                    item {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            StatCard(pal, tr("عقود موقعة (مصر)", "Signed EGP"), "$signedEgp EGP", Modifier.weight(1f))
                                            StatCard(pal, tr("MRR موقّع (USD)", "Signed MRR"), "$$signedUsd", Modifier.weight(1f))
                                        }
                                    }
                                    if (teardownReqs.isNotEmpty()) {
                                        item { Text(tr("طلبات فيديو التشريح الواردة (${teardownReqs.size})", "Inbound 2-Min Teardown Requests (${teardownReqs.size})"), fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                                        items(teardownReqs) { td ->
                                            val tdStatuses = listOf("new", "recording", "sent", "booked_call", "archived")
                                            Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(10.dp)).border(1.dp, pal.border, RoundedCornerShape(10.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("${td.senderName} · ${td.companyName} [${td.status.uppercase()}]", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("${td.url} · ${td.contact}", color = pal.muted, fontSize = 12.sp)
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedButton(onClick = { openUrl(td.url) }) { Text(tr("فتح الرابط", "Open URL"), fontSize = 11.sp) }
                                                    Button(onClick = {
                                                        val token = accessToken ?: return@Button
                                                        val nextSt = tdStatuses[(tdStatuses.indexOf(td.status) + 1) % tdStatuses.size]
                                                        scope.launch {
                                                            withContext(Dispatchers.IO) {
                                                                val req = Request.Builder().url("$SUPABASE_URL/rest/v1/teardown_requests?id=eq.${td.id}").patch(JSONObject().put("status", nextSt).toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                                client.newCall(req).execute().close()
                                                                logAdminAction(token, "update_teardown_status", "teardown_request", td.id)
                                                                loadAllData(token)
                                                            }
                                                        }
                                                    }) { Text(tr("تغيير الحالة", "Next Status"), fontSize = 11.sp) }
                                                }
                                            }
                                        }
                                    }
                                    item {
                                        Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(12.dp)).border(1.dp, pal.border, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(tr("إضافة عميل محتمل لخطة الـ 21 يوماً", "Add Prospect to 21-Day Pipeline"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            OutlinedTextField(value = newCrmComp, onValueChange = { newCrmComp = it }, label = { Text(tr("اسم الشركة أو المصنع", "Company Name")) }, modifier = Modifier.fillMaxWidth())
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(value = newCrmPerson, onValueChange = { newCrmPerson = it }, label = { Text(tr("صاحب القرار", "Decision Maker")) }, modifier = Modifier.weight(1f))
                                                OutlinedTextField(value = newCrmEgp, onValueChange = { newCrmEgp = it }, label = { Text("EGP") }, modifier = Modifier.weight(0.7f))
                                            }
                                            Button(onClick = {
                                                val token = accessToken ?: return@Button
                                                if (newCrmComp.isBlank()) return@Button
                                                scope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        val body = JSONObject().put("company_name", newCrmComp.trim()).put("decision_maker", newCrmPerson.trim()).put("proposed_egp", newCrmEgp.toDoubleOrNull() ?: 40000.0).put("stage", "queued")
                                                        val req = Request.Builder().url("$SUPABASE_URL/rest/v1/crm_deals").post(body.toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                        client.newCall(req).execute().close()
                                                        logAdminAction(token, "create_crm_deal", "crm_deal", newCrmComp.trim())
                                                        loadAllData(token)
                                                    }
                                                    newCrmComp = ""; newCrmPerson = ""
                                                }
                                            }, modifier = Modifier.fillMaxWidth()) { Text(tr("+ إضافة للـ Mini-CRM", "+ Add to CRM")) }
                                        }
                                    }
                                    items(crmDeals) { d ->
                                        Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(10.dp)).border(1.dp, if (d.isSigned) GreenOk else pal.border, RoundedCornerShape(10.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(d.companyName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text("${d.decisionMaker} · [${d.stage}]", color = pal.muted, fontSize = 11.sp)
                                                }
                                                Button(onClick = {
                                                    val token = accessToken ?: return@Button
                                                    val nextSign = !d.isSigned
                                                    scope.launch {
                                                        withContext(Dispatchers.IO) {
                                                            val body = JSONObject().put("is_signed", nextSign).put("stage", if (nextSign) "signed" else "proposal_sent").put("signed_amount_egp", if (nextSign) d.proposedEgp else 0.0)
                                                            val req = Request.Builder().url("$SUPABASE_URL/rest/v1/crm_deals?id=eq.${d.id}").patch(body.toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                            client.newCall(req).execute().close()
                                                            logAdminAction(token, if (nextSign) "mark_signed" else "unmark_signed", "crm_deal", d.companyName)
                                                            loadAllData(token)
                                                        }
                                                    }
                                                }) { Text(if (d.isSigned) tr("✓ عقد موقّع", "✓ Signed") else tr("توثيق عقد", "Sign"), fontSize = 11.sp) }
                                            }
                                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                OutlinedButton(onClick = {
                                                    val token = accessToken ?: return@OutlinedButton
                                                    val nextStage = stages[(stages.indexOf(d.stage) + 1) % stages.size]
                                                    scope.launch {
                                                        withContext(Dispatchers.IO) {
                                                            val req = Request.Builder().url("$SUPABASE_URL/rest/v1/crm_deals?id=eq.${d.id}").patch(JSONObject().put("stage", nextStage).toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                            client.newCall(req).execute().close()
                                                            loadAllData(token)
                                                        }
                                                    }
                                                }) { Text(tr("المرحلة التالية", "Next Stage"), fontSize = 11.sp) }
                                                OutlinedButton(onClick = {
                                                    copyAndShareText("أهلاً ${d.decisionMaker.ifBlank { "بك" }}، عملت تشريح سريع (دقيقتين) لحساب ${d.companyName}، وطلّعت فيه 3 أماكن ممكن يكون بيضيع فيها عميل، وحاجة تقدروا تعدّلوها النهارده من غير أي حد. مش عرض بيع، لو لقيته مفيد يسعدني أوريك الباقي في 15 دقيقة. أحمد · Artelligence")
                                                }) { Text(tr("إرسال رسالة 1", "Share Msg 1"), fontSize = 11.sp) }
                                                OutlinedButton(onClick = {
                                                    copyAndShareText("${d.decisionMaker.ifBlank { "أهلاً بك" }}، لحقت تشوف الفيديو؟ النقطة التالتة (وقت الرد) هي اللي غالباً بتفرق أكتر في التعاقدات.")
                                                }) { Text(tr("متابعة يوم 3", "Day 3"), fontSize = 11.sp) }
                                                OutlinedButton(onClick = {
                                                    copyAndShareText("مش هزنّ عليك تاني، لو الموضوع بقى أولوية بعدين الفيديو عندك، وأنا موجود على الواتساب +20 150 165 6010. أحمد · Artelligence")
                                                }) { Text(tr("متابعة يوم 7", "Day 7"), fontSize = 11.sp) }
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> {
                                // TAB 1: Case Studies Anonymization Governance + Collections CRUD
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    item { Text(tr("حوكمة دراسات الحالة وخصوصية العملاء", "Case Studies & Anonymization"), fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                                    items(caseStudies) { cs ->
                                        Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(10.dp)).border(1.dp, pal.border, RoundedCornerShape(10.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(if (cs.isAnonymized && cs.anonTitleAr.isNotBlank()) cs.anonTitleAr else cs.titleAr, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(if (cs.isAnonymized) tr("🔒 معروض بدون ذكر الاسم التجاري", "🔒 Anonymized Mode") else tr("🌐 معروض بالاسم الصريح", "🌐 Named Mode"), color = pal.muted, fontSize = 11.sp)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(onClick = {
                                                    val token = accessToken ?: return@Button
                                                    val nextAnon = !cs.isAnonymized
                                                    scope.launch {
                                                        withContext(Dispatchers.IO) {
                                                            val req = Request.Builder().url("$SUPABASE_URL/rest/v1/case_studies?id=eq.${cs.id}").patch(JSONObject().put("is_anonymized", nextAnon).toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                            client.newCall(req).execute().close()
                                                            logAdminAction(token, if (nextAnon) "anonymize_case_study" else "reveal_case_study", "case_study", cs.slug)
                                                            loadAllData(token)
                                                        }
                                                    }
                                                }) { Text(if (cs.isAnonymized) tr("تفعيل الاسم الصريح", "Reveal Name") else tr("إخفاء الاسم", "Anonymize"), fontSize = 11.sp) }
                                                OutlinedButton(onClick = {
                                                    val token = accessToken ?: return@OutlinedButton
                                                    val nextPub = !cs.isPublished
                                                    scope.launch {
                                                        withContext(Dispatchers.IO) {
                                                            val req = Request.Builder().url("$SUPABASE_URL/rest/v1/case_studies?id=eq.${cs.id}").patch(JSONObject().put("is_published", nextPub).toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                            client.newCall(req).execute().close()
                                                            logAdminAction(token, if (nextPub) "publish_case_study" else "hide_case_study", "case_study", cs.slug)
                                                            loadAllData(token)
                                                        }
                                                    }
                                                }) { Text(if (cs.isPublished) tr("إخفاء", "Hide") else tr("نشر", "Publish"), fontSize = 11.sp) }
                                            }
                                        }
                                    }

                                    item { Spacer(Modifier.height(6.dp)); Text(tr("إدارة المجموعات البصرية (${collections.size})", "Collections Governance (${collections.size})"), fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                                    item {
                                        Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(12.dp)).border(1.dp, pal.border, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(tr("إنشاء مجموعة جديدة", "New Collection"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            OutlinedTextField(value = newColSlug, onValueChange = { newColSlug = it }, label = { Text("Slug") }, modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = newColAr, onValueChange = { newColAr = it }, label = { Text(tr("الاسم بالعربي", "Arabic Title")) }, modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = newColEn, onValueChange = { newColEn = it }, label = { Text(tr("الاسم بالإنجليزي", "English Title")) }, modifier = Modifier.fillMaxWidth())
                                            Button(onClick = {
                                                val token = accessToken ?: return@Button
                                                if (newColSlug.isBlank()) return@Button
                                                scope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        val slug = newColSlug.trim()
                                                        val body = JSONObject().put("slug", slug).put("title_ar", newColAr.trim()).put("title_en", newColEn.trim()).put("year", 2026).put("sort_order", collections.size + 1).put("is_published", true)
                                                        val req = Request.Builder().url("$SUPABASE_URL/rest/v1/collections").post(body.toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                        client.newCall(req).execute().close()
                                                        logAdminAction(token, "create_collection", "collection", slug)
                                                        loadAllData(token)
                                                    }
                                                    newColSlug = ""; newColAr = ""; newColEn = ""
                                                }
                                            }, modifier = Modifier.fillMaxWidth()) { Text(tr("إنشاء المجموعة", "Create Collection")) }
                                        }
                                    }
                                    items(collections) { c ->
                                        Row(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(10.dp)).border(1.dp, pal.border, RoundedCornerShape(10.dp)).clickable { viewingCollection = c }.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(if (isAr) c.titleAr else c.titleEn, fontWeight = FontWeight.SemiBold, color = if (c.isPublished) pal.text else pal.muted)
                                                Text(if (c.isPublished) tr("منشورة · اضغط للاستعراض", "Published · Tap to inspect") else tr("مخفية · اضغط للاستعراض", "Hidden · Tap to inspect"), color = pal.muted, fontSize = 11.sp)
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
                                                            loadAllData(token)
                                                        }
                                                    }
                                                }) { Text(if (c.isPublished) tr("إخفاء", "Hide") else tr("نشر", "Publish")) }
                                                TextButton(onClick = { confirmDeleteCol = c }) { Text(tr("حذف", "Del"), color = DangerRed) }
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // TAB 2: Upload & Works Editor
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    item {
                                        Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(12.dp)).border(1.dp, pal.border, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(tr("رفع أعمال جديدة (تحويل تلقائي لـ WebP)", "Upload Works (Auto WebP)"), fontWeight = FontWeight.Bold)
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
                                            Button(onClick = { picker.launch("image/*") }, enabled = !isLoading, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                                                Text(if (isLoading) tr("جاري الرفع...", "Uploading...") else tr("اختيار صور ورفعها", "Pick Images & Upload"))
                                            }
                                            if (uploadStatus.isNotBlank()) Text(uploadStatus, color = GreenOk, fontSize = 12.sp)
                                        }
                                    }
                                    items(works.take(80)) { w ->
                                        Row(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(10.dp)).border(1.dp, pal.border, RoundedCornerShape(10.dp)).clickable { editingWork = w }.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            AsyncImage(model = w.thumbUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(if (isAr) w.titleAr.ifBlank { w.titleEn } else w.titleEn.ifBlank { w.titleAr }, color = if (w.isPublished) pal.text else pal.muted)
                                                Text("${w.origin.uppercase()} · ${w.qualityScore}%", color = pal.muted, fontSize = 11.sp)
                                            }
                                            Text(if (w.isFeatured) "★" else "✏️")
                                        }
                                    }
                                }
                            }
                            3 -> {
                                // TAB 3: 4-Gate Content Studio
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    item {
                                        Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(12.dp)).border(1.dp, pal.border, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(tr("مصنع المحتوى (4 بوابات + فحص الحمض النووي)", "4-Gate Content Mission Studio"), fontWeight = FontWeight.Bold)
                                            OutlinedTextField(value = newCpTitle, onValueChange = { newCpTitle = it }, label = { Text(tr("عنوان الكاروسيل أو التجربة", "Carousel / Story Title")) }, modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = newCpHook, onValueChange = { newCpHook = it }, label = { Text(tr("خطّاف الشريحة الأولى", "Slide 1 Hook")) }, modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = newCpProof, onValueChange = { newCpProof = it }, label = { Text(tr("مصدر الدليل الموثق", "Verified Evidence Source")) }, modifier = Modifier.fillMaxWidth())
                                            if (cpStatusMsg.isNotBlank()) Text(cpStatusMsg, color = DangerRed, fontSize = 12.sp)
                                            Button(onClick = {
                                                val token = accessToken ?: return@Button
                                                if (newCpTitle.contains("\u2014") || newCpHook.contains("\u2014")) {
                                                    cpStatusMsg = tr("مرفوض: يحتوي على شرطة طويلة محظورة!", "Rejected: Contains forbidden Em Dash!")
                                                    return@Button
                                                }
                                                cpStatusMsg = ""
                                                scope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        val body = JSONObject().put("title_ar", newCpTitle.trim()).put("hook_ar", newCpHook.trim()).put("source_proof", newCpProof.trim()).put("current_gate", 1).put("status", "draft")
                                                        val req = Request.Builder().url("$SUPABASE_URL/rest/v1/content_pipeline").post(body.toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                        client.newCall(req).execute().close()
                                                        logAdminAction(token, "create_content_item", "content_pipeline", newCpTitle.trim())
                                                        loadAllData(token)
                                                    }
                                                    newCpTitle = ""; newCpHook = ""; newCpProof = ""
                                                }
                                            }, modifier = Modifier.fillMaxWidth()) { Text(tr("فحص وحفظ في البوابة 1", "Validate DNA & Save Gate 1")) }
                                        }
                                    }
                                    items(contentItems) { cp ->
                                        Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(10.dp)).border(1.dp, pal.border, RoundedCornerShape(10.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("Gate ${cp.currentGate}/4 · ${cp.titleAr} [${cp.status}]", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("${cp.hookAr} · (${cp.sourceProof})", color = pal.muted, fontSize = 12.sp)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(onClick = {
                                                    val token = accessToken ?: return@OutlinedButton
                                                    val nextGate = (cp.currentGate + 1).coerceAtMost(4)
                                                    scope.launch {
                                                        withContext(Dispatchers.IO) {
                                                            val req = Request.Builder().url("$SUPABASE_URL/rest/v1/content_pipeline?id=eq.${cp.id}").patch(JSONObject().put("current_gate", nextGate).toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                            client.newCall(req).execute().close()
                                                            loadAllData(token)
                                                        }
                                                    }
                                                }) { Text(tr("ترقية البوابة", "Next Gate"), fontSize = 11.sp) }
                                                Button(onClick = {
                                                    val token = accessToken ?: return@Button
                                                    val nextSt = if (cp.status == "approved") "draft" else "approved"
                                                    scope.launch {
                                                        withContext(Dispatchers.IO) {
                                                            val req = Request.Builder().url("$SUPABASE_URL/rest/v1/content_pipeline?id=eq.${cp.id}").patch(JSONObject().put("status", nextSt).toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                            client.newCall(req).execute().close()
                                                            logAdminAction(token, "approve_content", "content_pipeline", cp.id)
                                                            loadAllData(token)
                                                        }
                                                    }
                                                }) { Text(if (cp.status == "approved") tr("✓ معتمد", "✓ Approved") else tr("اعتماد للنشر", "Approve"), fontSize = 11.sp) }
                                            }
                                        }
                                    }
                                }
                            }
                            4 -> {
                                // TAB 4: Mind Publisher, Inquiries Inbox & Audit Trail
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    item {
                                        Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(12.dp)).border(1.dp, pal.border, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(tr("نشر تدوينة حية في «داخل عقلي»", "Publish Live Mind Note"), fontWeight = FontWeight.Bold)
                                            OutlinedTextField(value = newNoteAr, onValueChange = { newNoteAr = it }, label = { Text(tr("العنوان بالعربي", "Arabic Title")) }, modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = newNoteEn, onValueChange = { newNoteEn = it }, label = { Text(tr("العنوان بالإنجليزي", "English Title")) }, modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = newNoteBodyAr, onValueChange = { newNoteBodyAr = it }, label = { Text(tr("النص بالعربي", "Arabic Body")) }, modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = newNoteBodyEn, onValueChange = { newNoteBodyEn = it }, label = { Text(tr("النص بالإنجليزي", "English Body")) }, modifier = Modifier.fillMaxWidth())
                                            Button(onClick = {
                                                val token = accessToken ?: return@Button
                                                scope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        val slug = "note-${System.currentTimeMillis()}"
                                                        val body = JSONObject()
                                                            .put("slug", slug).put("category", "now")
                                                            .put("title_ar", newNoteAr.replace("\u2014", "-").trim())
                                                            .put("title_en", newNoteEn.replace("\u2014", "-").trim())
                                                            .put("body_ar", newNoteBodyAr.replace("\u2014", "-").trim())
                                                            .put("body_en", newNoteBodyEn.replace("\u2014", "-").trim())
                                                            .put("sort_order", 1)
                                                        val req = Request.Builder().url("$SUPABASE_URL/rest/v1/mind_notes").post(body.toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                        client.newCall(req).execute().close()
                                                        logAdminAction(token, "publish_mind_note", "mind_note", slug)
                                                        loadAllData(token)
                                                    }
                                                    newNoteAr = ""; newNoteEn = ""; newNoteBodyAr = ""; newNoteBodyEn = ""
                                                }
                                            }, modifier = Modifier.fillMaxWidth()) { Text(tr("نشر في عقلي الآن", "Publish Note")) }
                                        }
                                    }
                                    item { Text(tr("الرسائل الواردة (${inquiries.size})", "Direct Inquiries (${inquiries.size})"), fontWeight = FontWeight.Bold) }
                                    items(inquiries) { inq ->
                                        Row(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(10.dp)).border(1.dp, pal.border, RoundedCornerShape(10.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${inq.senderName} (${inq.senderContact})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                Text(inq.message, color = pal.muted, fontSize = 12.sp)
                                            }
                                            TextButton(onClick = {
                                                val token = accessToken ?: return@TextButton
                                                scope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        val req = Request.Builder().url("$SUPABASE_URL/rest/v1/inquiries?id=eq.${inq.id}").patch(JSONObject().put("is_resolved", !inq.isResolved).toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $token").build()
                                                        client.newCall(req).execute().close()
                                                        loadAllData(token)
                                                    }
                                                }
                                            }) { Text(if (inq.isResolved) tr("مكتمل", "Done") else tr("تمييز كمكتمل", "Resolve")) }
                                        }
                                    }
                                    item { Text(tr("سجل التدقيق الإداري (${auditLogs.size})", "Admin Audit Trail (${auditLogs.size})"), fontWeight = FontWeight.Bold) }
                                    items(auditLogs) { log ->
                                        Row(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(8.dp)).border(1.dp, pal.border, RoundedCornerShape(8.dp)).padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${log.adminName} -> ${log.action}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                Text("${log.targetType}: ${log.targetId}", color = pal.muted, fontSize = 11.sp)
                                            }
                                            Text(log.createdAt, color = pal.muted, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // PUBLIC UNIFIED ARTELLIGENCE SUPER-APP (5 TABS)
                    when (publicTab) {
                        0 -> {
                            // TAB 0: Command Home & Audience Compass
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                item {
                                    Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(16.dp)).border(1.dp, pal.border, RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                            AsyncImage(model = HEADSHOT_URL, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(68.dp).clip(CircleShape))
                                            Column {
                                                Text(tr("أحمد عصام رمضان", "Ahmed Issam Ramadan"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                Text(tr("مستشار تحوّل الأعمال ودمج الذكاء الاصطناعي · مؤسس Artelligence", "Business Transformation & AI Consultant · Founder of Artelligence"), color = pal.muted, fontSize = 12.sp)
                                            }
                                        }
                                        Text(tr("اللي بياخد وقتك كل يوم، يشتغل لوحده.", "What eats your time every day, runs on its own."), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            tr("أحوّل الشغل اليومي المتلخبط في الشركات الصغيرة والمتوسطة والمصانع وشركات التشطيبات لأنظمة بسيطة تشتغل لوحدها، بأرقام حقيقية ومن غير تعقيد.", "Turning messy daily operations in factories, finishing firms, and growing SMEs into simple autonomous systems."),
                                            color = pal.muted, fontSize = 13.sp
                                        )
                                        Button(onClick = { publicTab = 1 }, modifier = Modifier.fillMaxWidth()) {
                                            Text(tr("اطلب فيديو تشريح مجاني (دقيقتين)", "Request Free 2-Min Teardown"))
                                        }
                                    }
                                }

                                item {
                                    Text(tr("اختر مسارك داخل Artelligence", "Choose Your Path"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }

                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(
                                            Triple(1, tr("01 · لأصحاب المصانع وشركات التشطيب والـ SMEs", "01 · Factories, Finishing & SMEs"), tr("سُلّم الحلول، حاسبة الاستفسارات الضائعة، وطلب فيديو التشريح المجاني.", "Service ladder, lost-lead calculator & free 2-min teardown.")),
                                            Triple(2, tr("02 · خزينة الأدلة والمتحف البصري", "02 · Verified Proof & Visual Archive"), tr("دراسات الحالة الموثقة (WASL، KHEM، منظومة التشطيبات، 92 ريل) والهويات.", "Production case studies, brand identities & 60 AI plates.")),
                                            Triple(3, tr("03 · داخل عقلي وهرم الذكاء الاصطناعي والأكاديمية", "03 · Mind, AI Pyramid & Academy"), tr("مبادئ التفكير، مقياس النضج الذاتي، والمناهج المفتوحة.", "Live thinking notes, 5-tier AI maturity check & LMS portals."))
                                        ).forEach { (targetIdx, title, sub) ->
                                            Column(
                                                modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(14.dp)).border(1.dp, pal.border, RoundedCornerShape(14.dp)).clickable { publicTab = targetIdx }.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(sub, color = pal.muted, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }

                                item {
                                    Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(14.dp)).border(1.dp, pal.border, RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(tr("ميثاق الصدق: ما لا نفعله", "What We Never Do"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(tr("• لا نؤلف أرقام عائد مالي أو قصص عملاء غير موثقة.", "• Zero fabricated ROI or unverified client claims."), color = pal.muted, fontSize = 12.sp)
                                        Text(tr("• لا نبيع أدوات معقدة لا يحتاجها فريقك.", "• Zero bloated AI tool stacks your team does not need."), color = pal.muted, fontSize = 12.sp)
                                        Text(tr("• لا التزام شهري قبل إثبات تحسن خط الأساس في 30 يوماً.", "• Zero long retainers before proving 30-day baseline impact."), color = pal.muted, fontSize = 12.sp)
                                    }
                                }

                                item {
                                    Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(14.dp)).border(1.dp, pal.border, RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(tr("من وراء هذه المنصة؟", "Who Built This Platform?"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            tr("خريج ليسانس الحقوق - جامعة الزقازيق (2022)، لم أمارس المحاماة يوماً، وبنيت كامل خبرتي في التصميم والبرمجة وقواعد البيانات وأتمتة الذكاء الاصطناعي بالتعلم الذاتي والتطبيق العملي المباشر.", "LL.B., Zagazig University (2022), never practiced law. 100% self-taught across UI/UX, full-stack web/mobile, databases, and AI workflow automation."),
                                            color = pal.muted, fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            // TAB 1: Solutions (ABTI x Growth OS + Calculator + Free 2-Min Teardown)
                            val orderNum = avgOrderVal.toLongOrNull() ?: 15000L
                            val rescuedDeals = ((missedWeekly * 4f * 0.25f).toInt()).coerceAtLeast(1)
                            val rescuedRev = rescuedDeals * orderNum

                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                item {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(tr("سُلّم الحلول و Growth OS", "ABTI x Growth OS Ladder"), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            FilterChip(selected = !useUsd, onClick = { useUsd = false; avgOrderVal = "15000" }, label = { Text(tr("مصر (ج.م)", "EGP")) })
                                            FilterChip(selected = useUsd, onClick = { useUsd = true; avgOrderVal = "1200" }, label = { Text("GCC (USD)") })
                                        }
                                    }
                                }
                                items(offers) { off ->
                                    Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(14.dp)).border(1.dp, pal.border, RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(if (isAr) off.badgeAr else off.badgeEn, color = pal.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(if (isAr) off.titleAr else off.titleEn, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(if (useUsd) off.priceUsd else off.priceEgp, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = pal.accent)
                                        Text(if (isAr) off.subtitleAr else off.subtitleEn, color = pal.muted, fontSize = 12.sp)
                                        (if (isAr) off.deliverablesAr else off.deliverablesEn).forEach { d ->
                                            Text("• $d", color = pal.muted, fontSize = 12.sp)
                                        }
                                    }
                                }
                                item {
                                    Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(14.dp)).border(1.dp, pal.border, RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(tr("حاسبة الاستفسارات الضائعة (شفافة 100%)", "Transparent Lost-Inquiry Calculator"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(tr("استفسارات أسبوعية متأخرة ليلاً أو الإجازات: ${missedWeekly.toInt()}", "Weekly delayed/after-hours inquiries: ${missedWeekly.toInt()}"), fontSize = 12.sp)
                                        Slider(value = missedWeekly, onValueChange = { missedWeekly = it }, valueRange = 2f..60f)
                                        OutlinedTextField(value = avgOrderVal, onValueChange = { avgOrderVal = it }, label = { Text(tr("متوسط قيمة الطلب أو التعاقد", "Average Order Value")) }, modifier = Modifier.fillMaxWidth())
                                        Column(modifier = Modifier.fillMaxWidth().background(pal.card, RoundedCornerShape(10.dp)).padding(12.dp)) {
                                            Text(tr("فرص شهرية قابلة للاسترداد بالرد الفوري (تقدير محافظ 25%):", "Monthly recoverable value (conservative 25% rescue):"), fontSize = 11.sp, color = pal.muted)
                                            Text("$rescuedRev ${if (useUsd) "USD" else "EGP"} / ${tr("شهرياً", "mo")} (~$rescuedDeals ${tr("طلبات", "orders")})", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                        }
                                    }
                                }
                                item {
                                    Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(14.dp)).border(1.dp, pal.accent, RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(tr("اطلب فيديو تشريح مجاني (دقيقتين) خلال 24 ساعة", "Request Free 2-Minute Video Teardown"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        OutlinedTextField(value = tdName, onValueChange = { tdName = it }, label = { Text(tr("الاسم الكريم", "Your Name")) }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = tdCompany, onValueChange = { tdCompany = it }, label = { Text(tr("اسم الشركة أو المصنع", "Company / Factory")) }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = tdUrl, onValueChange = { tdUrl = it }, label = { Text(tr("رابط الحساب أو الموقع", "Account or Website URL")) }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = tdContact, onValueChange = { tdContact = it }, label = { Text(tr("رقم الواتساب لاستلام الفيديو", "WhatsApp or Email")) }, modifier = Modifier.fillMaxWidth())
                                        Button(
                                            onClick = {
                                                if (tdName.isBlank() || tdUrl.isBlank() || tdContact.isBlank()) return@Button
                                                scope.launch {
                                                    val payload = JSONObject()
                                                        .put("sender_name", tdName.trim())
                                                        .put("company_name", tdCompany.trim())
                                                        .put("account_or_site_url", tdUrl.trim())
                                                        .put("contact_info", tdContact.trim())
                                                    withContext(Dispatchers.IO) {
                                                        val req = Request.Builder().url("$SUPABASE_URL/rest/v1/teardown_requests").post(payload.toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).build()
                                                        client.newCall(req).execute().close()
                                                    }
                                                    notifyLeadEdge("teardown_request", payload)
                                                    tdName = ""; tdCompany = ""; tdUrl = ""; tdContact = ""
                                                    tdFeedback = tr("تم استلام طلبك بنجاح! سأسجل لك فيديو التشريح خلال 24 ساعة.", "Teardown request received! Sending your 2-min audit within 24h.")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        ) { Text(tr("إرسال طلب التشريح المجاني", "Send Free Teardown Request")) }
                                        if (tdFeedback.isNotBlank()) Text(tdFeedback, color = GreenOk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        2 -> {
                            // TAB 2: Verified Proof Vault + Visual Archive Collections
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(selected = proofSubTab == 0, onClick = { proofSubTab = 0 }, label = { Text(tr("خزينة الأدلة (${caseStudies.size})", "Proof Vault (${caseStudies.size})")) })
                                    FilterChip(selected = proofSubTab == 1, onClick = { proofSubTab = 1 }, label = { Text(tr("المتحف والمجموعات (${collections.size})", "Visual Archive (${collections.size})")) })
                                }
                                if (proofSubTab == 0) {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(caseStudies) { cs ->
                                            val title = if (cs.isAnonymized) {
                                                if (isAr) cs.anonTitleAr.ifBlank { cs.titleAr } else cs.anonTitleEn.ifBlank { cs.titleEn }
                                            } else {
                                                if (isAr) cs.titleAr else cs.titleEn
                                            }
                                            Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(14.dp)).border(1.dp, pal.border, RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                if (cs.coverUrl.isNotBlank()) {
                                                    AsyncImage(model = cs.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(145.dp).clip(RoundedCornerShape(10.dp)))
                                                }
                                                Text(
                                                    "${if (isAr) cs.sectorAr else cs.sectorEn} · ${if (cs.isIllustrative) tr("سيناريو توضيحي", "Illustrative") else tr("دليل موثق ✓", "Verified ✓")}",
                                                    color = pal.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold
                                                )
                                                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                Text("${tr("المشكلة:", "Problem:")} ${if (isAr) cs.problemAr else cs.problemEn}", fontSize = 12.sp, color = pal.muted)
                                                Text("${tr("الحل:", "Architecture:")} ${if (isAr) cs.solutionAr else cs.solutionEn}", fontSize = 12.sp, color = pal.muted)
                                                Text("${tr("المخرج الموثق:", "Outcome:")} ${if (isAr) cs.outcomeAr else cs.outcomeEn}", fontSize = 12.sp, color = pal.text)
                                                (if (isAr) cs.deliverablesAr else cs.deliverablesEn).take(3).forEach { d ->
                                                    Text("• $d", fontSize = 11.sp, color = pal.muted)
                                                }
                                                val evNote = if (isAr) cs.evidenceNoteAr else cs.evidenceNoteEn
                                                if (evNote.isNotBlank()) Text(evNote, fontSize = 11.sp, color = pal.muted)
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    if (cs.collectionSlug.isNotBlank() && !cs.isAnonymized) {
                                                        Button(onClick = { viewingCollection = collections.find { it.slug == cs.collectionSlug } }) {
                                                            Text(tr("معاينة الهوية", "View System"), fontSize = 12.sp)
                                                        }
                                                    }
                                                    if (cs.externalUrl.isNotBlank()) {
                                                        OutlinedButton(onClick = { openUrl(cs.externalUrl) }) { Text(tr("فتح المنصة ↗", "Open Live ↗"), fontSize = 12.sp) }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        listOf("client" to tr("هويات وأنظمة العملاء", "Client Brand Systems"), "personal" to tr("استكشافات الفن والذكاء الاصطناعي", "Personal & AI Plates"), "educational" to tr("السلاسل النفسية والتوعوية", "Visual Psychology")).forEach { (cat, header) ->
                                            val catCols = collections.filter { it.category == cat }
                                            if (catCols.isNotEmpty()) {
                                                item { Text(header, fontWeight = FontWeight.Bold, color = pal.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
                                                items(catCols) { c ->
                                                    val cnt = works.count { it.collectionId == c.id && (c.slug != "photography" || it.isCurated) }
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(12.dp)).border(1.dp, pal.border, RoundedCornerShape(12.dp)).clickable { viewingCollection = c }.padding(14.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(if (isAr) c.titleAr else c.titleEn, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                        Text("${if (isAr) c.mediumAr else c.mediumEn} · $cnt ${tr("عمل", "works")}", color = pal.muted, fontSize = 11.sp)
                                                        Text(if (isAr) c.descAr else c.descEn, color = pal.muted, fontSize = 12.sp, maxLines = 2)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        3 -> {
                            // TAB 3: Inside My Mind + AI Maturity Pyramid + Academy & Systems
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(selected = mindSubTab == 0, onClick = { mindSubTab = 0 }, label = { Text(tr("داخل عقلي", "Mind Notes")) })
                                    FilterChip(selected = mindSubTab == 1, onClick = { mindSubTab = 1 }, label = { Text(tr("هرم الذكاء الاصطناعي", "AI Pyramid")) })
                                    FilterChip(selected = mindSubTab == 2, onClick = { mindSubTab = 2 }, label = { Text(tr("الأكاديمية والأنظمة", "Academy & Apps")) })
                                }
                                when (mindSubTab) {
                                    0 -> {
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            items(mindNotes) { n ->
                                                Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(12.dp)).border(1.dp, pal.border, RoundedCornerShape(12.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(n.category.uppercase(), color = pal.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    Text(if (isAr) n.titleAr else n.titleEn, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                    Text(if (isAr) n.bodyAr else n.bodyEn, color = pal.muted, fontSize = 13.sp)
                                                }
                                            }
                                        }
                                    }
                                    1 -> {
                                        val totalScore = pyrScores.sum().toInt()
                                        val tierLabel = when {
                                            totalScore >= 22 -> tr("المستوى 5 · مهندس المنظومات السيادية", "Tier 5 · Sovereign Systems Architect")
                                            totalScore >= 17 -> tr("المستوى 4 · مفوِّض الوكلاء والأتمتة", "Tier 4 · Agent Orchestrator")
                                            totalScore >= 12 -> tr("المستوى 3 · باني هندسة السياق", "Tier 3 · Context Engineer")
                                            totalScore >= 8 -> tr("المستوى 2 · منظم الأوامر و SOPs", "Tier 2 · SOP Builder")
                                            else -> tr("المستوى 1 · مستهلك الأدوات", "Tier 1 · Tool Consumer")
                                        }
                                        val pyrQs = listOf(
                                            tr("1. الانطلاق من ملف سياق وهوية موثق", "1. Documented context & DNA library"),
                                            tr("2. تفويض مهام متكاملة للوكلاء", "2. Multi-step agent delegation"),
                                            tr("3. بوابة فحص جودة ضد تأليف الأرقام", "3. Strict source & tone QA gate"),
                                            tr("4. ربط القنوات وقواعد البيانات آلياً", "4. Live DB & channel automation"),
                                            tr("5. قياس الأثر بوقت موفر واستفسارات مخدومة", "5. Measuring real saved hours & leads")
                                        )
                                        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(14.dp)).border(1.dp, pal.accent, RoundedCornerShape(14.dp)).padding(14.dp)) {
                                                Text("$totalScore / 25 · $tierLabel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            }
                                            pyrQs.forEachIndexed { idx, qText ->
                                                Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(12.dp)).border(1.dp, pal.border, RoundedCornerShape(12.dp)).padding(12.dp)) {
                                                    Text("$qText (${pyrScores[idx].toInt()}/5)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                    Slider(value = pyrScores[idx], onValueChange = { pyrScores[idx] = it }, valueRange = 1f..5f, steps = 3)
                                                }
                                            }
                                        }
                                    }
                                    2 -> {
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            items(products) { prod ->
                                                Column(modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(14.dp)).border(1.dp, pal.border, RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(if (isAr) prod.badgeAr else prod.badgeEn, color = pal.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Text(if (isAr) prod.titleAr else prod.titleEn, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                    Text(if (isAr) prod.descAr else prod.descEn, color = pal.muted, fontSize = 12.sp)
                                                    if (prod.liveUrl.startsWith("http")) {
                                                        OutlinedButton(onClick = { openUrl(prod.liveUrl) }) { Text(tr("فتح البوابة التفاعلية ↗", "Open Live Portal ↗"), fontSize = 12.sp) }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        4 -> {
                            // TAB 4: Interactive Lab (Human or AI + SME Readiness + Direct Connect)
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(selected = labSubTab == 0, onClick = { labSubTab = 0 }, label = { Text(tr("إنسان أم آلة؟", "Human or AI?")) })
                                    FilterChip(selected = labSubTab == 1, onClick = { labSubTab = 1 }, label = { Text(tr("فحص الجاهزية", "Readiness")) })
                                    FilterChip(selected = labSubTab == 2, onClick = { labSubTab = 2 }, label = { Text(tr("تواصل مباشر", "Connect")) })
                                }
                                when (labSubTab) {
                                    0 -> {
                                        if (gameRounds.isEmpty()) {
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(tr("جاري التحميل...", "Loading...")) }
                                        } else if (gameIdx >= gameRounds.size) {
                                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(tr("النتيجة النهائية", "Final Score"), color = pal.muted)
                                                Text("$gameScore / ${gameRounds.size}", fontSize = 44.sp, fontWeight = FontWeight.Bold)
                                                Spacer(Modifier.height(16.dp))
                                                Button(onClick = { startChallenge(works) }) { Text(tr("العب جولة جديدة", "Play Again")) }
                                            }
                                        } else {
                                            val w = gameRounds[gameIdx]
                                            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("${tr("جولة", "Round")} ${gameIdx + 1}/${gameRounds.size}", color = pal.muted)
                                                    Text("${tr("النتيجة:", "Score:")} $gameScore", fontWeight = FontWeight.Bold)
                                                }
                                                AsyncImage(model = w.fullUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF050507)))
                                                if (gameAnswered == null) {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                        Button(onClick = { val ok = w.origin == "camera"; if (ok) gameScore++; gameAnswered = ok }, modifier = Modifier.weight(1f)) { Text(tr("📷 كاميرا", "📷 Camera")) }
                                                        Button(onClick = { val ok = w.origin == "ai"; if (ok) gameScore++; gameAnswered = ok }, modifier = Modifier.weight(1f)) { Text(tr("✨ ذكاء اصطناعي", "✨ AI")) }
                                                    }
                                                } else {
                                                    Text(if (gameAnswered == true) tr("إجابة صحيحة ✓", "Correct ✓") else tr("إجابة خاطئة ✕", "Wrong ✕"), color = if (gameAnswered == true) GreenOk else DangerRed, fontWeight = FontWeight.Bold)
                                                    Button(onClick = { gameIdx++; gameAnswered = null }, modifier = Modifier.fillMaxWidth()) { Text(tr("التالي", "Next")) }
                                                }
                                            }
                                        }
                                    }
                                    1 -> {
                                        val rdyQuestions = listOf(
                                            tr("الرد على رسائل المساء والإجازات في أقل من دقيقة", "After-hours inquiries answered in under 60s"),
                                            tr("وضوح الكتالوج والمواصفات في أول 10 ثوانٍ", "Catalog & specs clear within 10 seconds"),
                                            tr("متابعة منظمة تلقائية في اليوم 3 واليوم 7", "Structured Day-3 and Day-7 follow-ups"),
                                            tr("تحويل العميل الجاد لفريق المبيعات بعد تأهيله", "Sales team receives pre-qualified leads"),
                                            tr("نظام محتوى مربوط بالمبيعات لا النشر العشوائي", "Content system tied to conversion"),
                                            tr("تتبع دقيق للعقود الموقعة فقط بدون تقديرات", "Exact signed-contract tracking")
                                        )
                                        val yesCnt = readinessChecks.count { it }
                                        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(tr("فحص جاهزية شركتك للأتمتة ($yesCnt / 6)", "SME Automation Readiness ($yesCnt / 6)"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            rdyQuestions.forEachIndexed { idx, q ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().background(pal.surface, RoundedCornerShape(10.dp)).border(1.dp, pal.border, RoundedCornerShape(10.dp)).clickable { readinessChecks[idx] = !readinessChecks[idx] }.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Checkbox(checked = readinessChecks[idx], onCheckedChange = { readinessChecks[idx] = it })
                                                    Text(q, fontSize = 13.sp)
                                                }
                                            }
                                            Button(onClick = { publicTab = 1 }, modifier = Modifier.fillMaxWidth()) {
                                                Text(tr("اطلب فيديو التشريح المجاني الآن", "Request Free 2-Min Teardown"))
                                            }
                                        }
                                    }
                                    2 -> {
                                        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text(tr("قنوات التواصل الرسمية الموحدة", "Unified Direct Channels"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            Button(onClick = { openUrl("https://wa.me/201501656010") }, modifier = Modifier.fillMaxWidth()) {
                                                Text(tr("💬 واتساب · مكالمة 15 دقيقة (+20 150 165 6010)", "💬 WhatsApp · 15m Call (+20 150 165 6010)"))
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(onClick = { openUrl("mailto:ahmedissam.work@gmail.com") }, modifier = Modifier.weight(1f)) { Text("Email ↗", fontSize = 12.sp) }
                                                OutlinedButton(onClick = { openUrl("https://www.linkedin.com/in/ahmdesam96") }, modifier = Modifier.weight(1f)) { Text("LinkedIn ↗", fontSize = 12.sp) }
                                            }
                                            HorizontalDivider(color = pal.border)
                                            Text(tr("رسالة فورية لمركز القيادة:", "Instant Message to Command Studio:"), fontWeight = FontWeight.Bold)
                                            OutlinedTextField(value = inqName, onValueChange = { inqName = it }, label = { Text(tr("الاسم الكريم", "Your Name")) }, modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = inqContact, onValueChange = { inqContact = it }, label = { Text(tr("رقم الواتساب أو البريد", "WhatsApp or Email")) }, modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = inqMsg, onValueChange = { inqMsg = it }, label = { Text(tr("تفاصيل رسالتك", "Message")) }, modifier = Modifier.fillMaxWidth())
                                            Button(
                                                onClick = {
                                                    if (inqName.isBlank() || inqContact.isBlank() || inqMsg.isBlank()) return@Button
                                                    scope.launch {
                                                        val payload = JSONObject().put("sender_name", inqName.trim()).put("sender_contact", inqContact.trim()).put("message", inqMsg.trim())
                                                        withContext(Dispatchers.IO) {
                                                            val req = Request.Builder().url("$SUPABASE_URL/rest/v1/inquiries").post(payload.toString().toRequestBody("application/json".toMediaType())).addHeader("apikey", SUPABASE_ANON_KEY).build()
                                                            client.newCall(req).execute().close()
                                                        }
                                                        notifyLeadEdge("direct_inquiry", payload)
                                                        inqName = ""; inqContact = ""; inqMsg = ""
                                                        inqStatus = tr("تم استلام رسالتك بنجاح!", "Brief received successfully!")
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) { Text(tr("إرسال الرسالة", "Send Direct Message")) }
                                            if (inqStatus.isNotBlank()) Text(inqStatus, color = GreenOk, fontSize = 13.sp)
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
}

@Composable
fun StatCard(pal: ArtPalette, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(pal.surface, RoundedCornerShape(12.dp)).border(1.dp, pal.border, RoundedCornerShape(12.dp)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = pal.text)
        Text(label, fontSize = 11.sp, color = pal.muted)
    }
}
