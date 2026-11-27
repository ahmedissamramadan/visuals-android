package com.ahmedramadan.visuals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class PlanMilestone(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val status: String,
    val progressPct: Int
)

data class PlanTask(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val status: String,
    val assignedToName: String,
    val priority: String
)

data class ActivityItem(
    val id: String,
    val actorName: String,
    val actorRole: String,
    val actionTextAr: String,
    val actionTextEn: String,
    val createdAt: String
)

@Composable
fun SuperAppProgressScreen(
    supabaseUrl: String,
    anonKey: String,
    accessToken: String?,
    actorName: String,
    isArabic: Boolean,
    bg: Color,
    surface: Color,
    card: Color,
    border: Color,
    text: Color,
    muted: Color,
    accent: Color,
    onAccent: Color
) {
    val scope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }

    var milestones by remember { mutableStateOf<List<PlanMilestone>>(emptyList()) }
    var tasks by remember { mutableStateOf<List<PlanTask>>(emptyList()) }
    var feed by remember { mutableStateOf<List<ActivityItem>>(emptyList()) }
    var newTaskTitle by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }

    suspend fun fetchAll() {
        val token = accessToken ?: return
        isSyncing = true
        withContext(Dispatchers.IO) {
            try {
                fun getJsonArray(path: String): JSONArray {
                    val req = Request.Builder()
                        .url("$supabaseUrl/rest/v1/$path")
                        .addHeader("apikey", anonKey)
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                    client.newCall(req).execute().use { res ->
                        if (!res.isSuccessful) return JSONArray()
                        return JSONArray(res.body?.string() ?: "[]")
                    }
                }

                val mArr = getJsonArray("plan_milestones?select=*&order=sort_order.asc")
                val mList = (0 until mArr.length()).map { i ->
                    val o = mArr.getJSONObject(i)
                    PlanMilestone(
                        id = o.optString("id"),
                        titleAr = o.optString("title_ar"),
                        titleEn = o.optString("title_en"),
                        status = o.optString("status"),
                        progressPct = o.optInt("progress_pct", 0)
                    )
                }

                val tArr = getJsonArray("plan_tasks?select=*&order=updated_at.desc")
                val tList = (0 until tArr.length()).map { i ->
                    val o = tArr.getJSONObject(i)
                    PlanTask(
                        id = o.optString("id"),
                        titleAr = o.optString("title_ar"),
                        titleEn = o.optString("title_en"),
                        status = o.optString("status"),
                        assignedToName = o.optString("assigned_to_name", "Ahmed"),
                        priority = o.optString("priority", "normal")
                    )
                }

                val fArr = getJsonArray("activity_feed?select=*&order=created_at.desc&limit=15")
                val fList = (0 until fArr.length()).map { i ->
                    val o = fArr.getJSONObject(i)
                    ActivityItem(
                        id = o.optString("id"),
                        actorName = o.optString("actor_name"),
                        actorRole = o.optString("actor_role"),
                        actionTextAr = o.optString("action_text_ar"),
                        actionTextEn = o.optString("action_text_en"),
                        createdAt = o.optString("created_at")
                    )
                }

                withContext(Dispatchers.Main) {
                    milestones = mList
                    tasks = tList
                    feed = fList
                }
            } catch (_: Exception) {
            } finally {
                withContext(Dispatchers.Main) { isSyncing = false }
            }
        }
    }

    suspend fun logActivity(ar: String, en: String) {
        val token = accessToken ?: return
        withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("actor_name", actorName)
                put("actor_role", "owner")
                put("action_text_ar", ar)
                put("action_text_en", en)
                put("entity_type", "task")
            }.toString().toRequestBody("application/json".toMediaType())

            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/activity_feed")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $token")
                .post(body)
                .build()
            client.newCall(req).execute().close()
        }
    }

    LaunchedEffect(accessToken) {
        while (accessToken != null) {
            fetchAll()
            delay(5000)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ARTELLIGENCE  ·  ART • INTELLIGENCE • IMPACT",
                    color = muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "التقدم اللحظي والفريق" else "Live Progress & Team",
                        color = text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isSyncing) accent else surface)
                            .border(1.dp, border, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isSyncing) (if (isArabic) "مزامنة..." else "Syncing...") else (if (isArabic) "متصل لحظياً" else "Live"),
                            color = if (isSyncing) onAccent else Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        if (accessToken == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(card)
                        .border(1.dp, border, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (isArabic) "سجل الدخول من مركز القيادة لمتابعة التقدم والفريق لحظياً." else "Sign in from Studio to track live progress and team activity.",
                        color = muted,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            item {
                Text(
                    text = if (isArabic) "مراحل الخطة" else "Plan Milestones",
                    color = muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(milestones, key = { it.id }) { m ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(card)
                        .border(1.dp, border, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isArabic) m.titleAr else m.titleEn,
                            color = text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${m.progressPct}%",
                            color = accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (m.progressPct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = accent,
                        trackColor = surface
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isArabic) "إضافة مهمة سريعة" else "Quick Add Task",
                    color = muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        placeholder = { Text(if (isArabic) "اكتب المهمة..." else "Task title...", color = muted) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = text,
                            unfocusedTextColor = text,
                            focusedBorderColor = accent,
                            unfocusedBorderColor = border
                        )
                    )
                    Button(
                        onClick = {
                            val title = newTaskTitle.trim()
                            if (title.isNotEmpty()) {
                                newTaskTitle = ""
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val body = JSONObject().apply {
                                            put("title_ar", title)
                                            put("title_en", title)
                                            put("status", "in_progress")
                                            put("assigned_to_name", actorName)
                                        }.toString().toRequestBody("application/json".toMediaType())

                                        val req = Request.Builder()
                                            .url("$supabaseUrl/rest/v1/plan_tasks")
                                            .addHeader("apikey", anonKey)
                                            .addHeader("Authorization", "Bearer $accessToken")
                                            .post(body)
                                            .build()
                                        client.newCall(req).execute().close()
                                    }
                                    logActivity("أضاف مهمة: $title", "Added task: $title")
                                    fetchAll()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = onAccent)
                    ) {
                        Text(if (isArabic) "إضافة" else "Add", fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(tasks, key = { it.id }) { t ->
                val nextStatus = when (t.status) {
                    "pending" -> "in_progress"
                    "in_progress" -> "done"
                    else -> "pending"
                }
                val statusColor = when (t.status) {
                    "done" -> Color(0xFF10B981)
                    "in_progress" -> accent
                    else -> muted
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(surface)
                        .border(1.dp, border, RoundedCornerShape(12.dp))
                        .clickable {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val body = JSONObject().apply {
                                        put("status", nextStatus)
                                        put("updated_by", actorName)
                                    }.toString().toRequestBody("application/json".toMediaType())

                                    val req = Request.Builder()
                                        .url("$supabaseUrl/rest/v1/plan_tasks?id=eq.${t.id}")
                                        .addHeader("apikey", anonKey)
                                        .addHeader("Authorization", "Bearer $accessToken")
                                        .patch(body)
                                        .build()
                                    client.newCall(req).execute().close()
                                }
                                logActivity("حدّث حالة '${t.titleAr}' إلى $nextStatus", "Updated '${t.titleEn}' to $nextStatus")
                                fetchAll()
                            }
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) t.titleAr else t.titleEn,
                            color = text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = t.assignedToName,
                            color = muted,
                            fontSize = 11.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(statusColor.copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = t.status,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isArabic) "نشاط الفريق اللحظي" else "Live Team Feed",
                    color = muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(feed, key = { it.id }) { f ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(card)
                        .border(1.dp, border, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Column {
                        Text(
                            text = f.actorName,
                            color = accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isArabic) f.actionTextAr else f.actionTextEn,
                            color = text,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0A0A0A))
                        .border(1.dp, Color(0xFF27272A), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ARTELLIGENCE · SSOT v2.0",
                        color = Color(0xFFFFFFFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ahmed Issam Ramadan • VentureCycle Studio",
                        color = Color(0xFFA1A1AA),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
