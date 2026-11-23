package com.ahmedramadan.visuals

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
private val AccentGold = Color(0xFFC8A46E)
private val TextIvory = Color(0xFFF3EFE6)
private val MutedGray = Color(0xFF9A958C)

data class CollectionItem(val id: String, val slug: String, val titleEn: String, val titleAr: String)
data class CommentItem(val id: String, val body: String, val isHidden: Boolean, val author: String, val workTitle: String)

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
    var comments by remember { mutableStateOf<List<CommentItem>>(emptyList()) }
    var selectedColIndex by remember { mutableStateOf(0) }
    var selectedOrigin by remember { mutableStateOf("camera") }
    var uploadTitle by remember { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf("") }

    var newColSlug by remember { mutableStateOf("") }
    var newColEn by remember { mutableStateOf("") }
    var newColAr by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) }

    suspend fun loadDashboardData(token: String) {
        withContext(Dispatchers.IO) {
            val colReq = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/collections?select=id,slug,title_en,title_ar&order=sort_order")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $token")
                .build()
            client.newCall(colReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val arr = JSONArray(resp.body?.string() ?: "[]")
                    val list = mutableListOf<CollectionItem>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        list.add(CollectionItem(o.getString("id"), o.getString("slug"), o.getString("title_en"), o.getString("title_ar")))
                    }
                    collections = list
                }
            }

            val comReq = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/comments?select=id,body,is_hidden,profiles(display_name),works(title_en)&order=created_at.desc&limit=30")
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
        val col = collections[selectedColIndex]

        scope.launch {
            isLoading = true
            withContext(Dispatchers.IO) {
                for ((idx, uri) in uris.withIndex()) {
                    statusMsg = "Processing ${idx + 1}/${uris.size}..."
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

                    val title = uploadTitle.ifBlank { "Work ${idx + 1}" }
                    val body = JSONObject().apply {
                        put("slug", slug)
                        put("collection_id", col.id)
                        put("origin", selectedOrigin)
                        put("media_type", "image")
                        put("title_en", title)
                        put("title_ar", title)
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
                statusMsg = "Uploaded ${uris.size} work(s) to ${col.titleEn}!"
            }
            isLoading = false
        }
    }

    if (accessToken == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Visuals Admin", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AccentGold)
            Spacer(Modifier.height(8.dp))
            Text("Ahmed Ramadan — Central Visual Home", fontSize = 14.sp, color = MutedGray)
            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username (boda890) or Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            if (loginError != null) {
                Spacer(Modifier.height(8.dp))
                Text(loginError!!, color = Color(0xFFE06C75), fontSize = 13.sp)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        loginError = null
                        val email = if (username.trim().equals("boda890", ignoreCase = true)) "bodykhaled5000@gmail.com" else username.trim()
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
                                    loginError = json.optString("error_description", "Login failed")
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
                Text(if (isLoading) "Signing in..." else "Sign In")
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Visuals Admin", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                TextButton(onClick = { accessToken = null }) { Text("Sign Out", color = MutedGray) }
            }

            TabRow(selectedTabIndex = activeTab, containerColor = SurfaceDark, contentColor = AccentGold) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Upload") })
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Collections") })
                Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("Comments") })
            }
            Spacer(Modifier.height(16.dp))

            when (activeTab) {
                0 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (collections.isNotEmpty()) {
                            Text("Collection: ${collections[selectedColIndex].titleEn}", color = AccentGold, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { selectedColIndex = (selectedColIndex + 1) % collections.size }) {
                                    Text("Next Collection")
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("camera", "ai", "design").forEach { orig ->
                                FilterChip(
                                    selected = selectedOrigin == orig,
                                    onClick = { selectedOrigin = orig },
                                    label = { Text(orig.uppercase()) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = uploadTitle,
                            onValueChange = { uploadTitle = it },
                            label = { Text("Title (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { picker.launch("image/*") },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isLoading) "Uploading..." else "Pick Images from Gallery & Upload")
                        }
                        if (statusMsg.isNotBlank()) {
                            Text(statusMsg, color = AccentGold, fontSize = 14.sp)
                        }
                    }
                }
                1 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = newColSlug, onValueChange = { newColSlug = it }, label = { Text("Slug (e.g. street-cairo)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = newColEn, onValueChange = { newColEn = it }, label = { Text("Title (EN)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = newColAr, onValueChange = { newColAr = it }, label = { Text("Title (AR)") }, modifier = Modifier.fillMaxWidth())
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
                        ) { Text("Create Collection") }
                    }
                }
                2 -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(comments) { c ->
                            Row(
                                modifier = Modifier.fillMaxWidth().background(SurfaceDark, RoundedCornerShape(10.dp)).padding(12.dp),
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
                                }) {
                                    Text(if (c.isHidden) "Unhide" else "Hide")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
