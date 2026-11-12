package com.cmstudio.player.ui.vault

import android.content.DialogInterface
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cmstudio.player.ui.AppSettings
import com.cmstudio.player.ui.model.MediaVideoItem
import com.cmstudio.player.ui.theme.extendedColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun VaultScreen(
    allVideos: List<MediaVideoItem> = emptyList(),
    onVideoClick: (MediaVideoItem) -> Unit = {},
    onCloseClick: () -> Unit = {},
    onPlayVideo: (MediaVideoItem) -> Unit = onVideoClick,
    onVaultMembershipChanged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isUnlocked by AppSettings.isVaultUnlocked.collectAsState()
    val hasCustomPin by AppSettings.hasCustomVaultPin.collectAsState()
    val vaultUriSet by AppSettings.vaultVideoUris.collectAsState()
    val vaultEntryHidden by AppSettings.vaultEntryHidden.collectAsState()
    val vaultBiometricEnabled by AppSettings.vaultBiometricEnabled.collectAsState()
    val savedQuestion by AppSettings.vaultSecurityQuestion.collectAsState()

    val vaultVideos = remember(allVideos, vaultUriSet) {
        allVideos.filter { it.id in vaultUriSet }
    }

    var pin by remember { mutableStateOf("") }
    var firstSetupPin by remember { mutableStateOf<String?>(null) }
    var pinHint by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showSecurityRecoveryDialog by remember { mutableStateOf(false) }
    var showSecurityQuestionSetupDialog by remember { mutableStateOf(false) }
    var showAddMediaDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showVaultSettingsMenu by remember { mutableStateOf(false) }

    fun triggerShake(message: String) {
        pinHint = message
        coroutineScope.launch {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    0f at 0
                    -20f at 50
                    20f at 100
                    -15f at 150
                    15f at 200
                    -8f at 250
                    8f at 300
                    0f at 350
                }
            )
            delay(200)
            pin = ""
        }
    }

    fun launchBiometricUnlock() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Toast.makeText(context, "Biometric prompt requires Android 9 or newer", Toast.LENGTH_SHORT).show()
            return
        }
        val canBio = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bm = context.getSystemService(BiometricManager::class.java)
            @Suppress("DEPRECATION")
            bm != null && bm.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
        } else {
            true
        }
        if (!canBio) {
            Toast.makeText(context, "No biometrics enrolled on this device. Enter your 4-digit Vault PIN.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val prompt = BiometricPrompt.Builder(context)
                .setTitle("Unlock Privacy Vault")
                .setSubtitle("Authenticate to view hidden videos")
                .setNegativeButton("Use PIN", context.mainExecutor, DialogInterface.OnClickListener { _, _ -> })
                .build()
            prompt.authenticate(
                CancellationSignal(),
                context.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        AppSettings.setVaultUnlocked(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED &&
                            errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                            !errString.isNullOrBlank()
                        ) {
                            Toast.makeText(context, errString, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        } catch (_: Throwable) {
            Toast.makeText(context, "Use your 4-digit Vault PIN to unlock", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleKeyPress(key: String) {
        if (pin.length >= 4) return
        pinHint = null
        val updated = pin + key
        pin = updated
        if (updated.length == 4) {
            if (!hasCustomPin) {
                val first = firstSetupPin
                if (first == null) {
                    firstSetupPin = updated
                    pin = ""
                    pinHint = "Re-enter the same 4 digits to confirm PIN"
                } else if (first == updated) {
                    AppSettings.setVaultPin(updated)
                    firstSetupPin = null
                    pin = ""
                    pinHint = null
                    AppSettings.setVaultUnlocked(true)
                } else {
                    firstSetupPin = null
                    triggerShake("PINs did not match. Choose a 4-digit PIN.")
                }
            } else {
                if (AppSettings.verifyVaultPin(updated)) {
                    pin = ""
                    pinHint = null
                    AppSettings.setVaultUnlocked(true)
                } else {
                    triggerShake("Incorrect PIN")
                }
            }
        }
    }

    fun handleBackspace() {
        if (pin.isNotEmpty()) {
            pin = pin.dropLast(1)
        }
    }

    if (isUnlocked && hasCustomPin) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.extendedColors.badgeBlueBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LockOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Privacy Vault",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (vaultVideos.size == 1) "1 hidden video" else "${vaultVideos.size} hidden videos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { showAddMediaDialog = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Hide Media",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Box {
                    IconButton(onClick = { showVaultSettingsMenu = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Vault Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showVaultSettingsMenu,
                        onDismissRequest = { showVaultSettingsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Change PIN") },
                            leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                            onClick = {
                                showVaultSettingsMenu = false
                                showChangePinDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(if (savedQuestion.isBlank()) "Set Security Question" else "Update Security Question")
                            },
                            leadingIcon = { Icon(Icons.Rounded.Security, contentDescription = null) },
                            onClick = {
                                showVaultSettingsMenu = false
                                showSecurityQuestionSetupDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Biometric Unlock")
                                    Spacer(modifier = Modifier.weight(1f))
                                    Switch(
                                        checked = vaultBiometricEnabled,
                                        onCheckedChange = null
                                    )
                                }
                            },
                            leadingIcon = { Icon(Icons.Rounded.Fingerprint, contentDescription = null) },
                            onClick = {
                                AppSettings.setVaultBiometricEnabled(!vaultBiometricEnabled)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (vaultEntryHidden) "Show Vault Tab in Nav Bar"
                                    else "Hide Vault Tab (Stealth Mode)"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (vaultEntryHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showVaultSettingsMenu = false
                                val nextHidden = !vaultEntryHidden
                                AppSettings.setVaultEntryHidden(nextHidden)
                                Toast.makeText(
                                    context,
                                    if (nextHidden) "Vault tab hidden. Double-tap 'MPVX Player' on Home to open Vault."
                                    else "Vault tab restored to bottom bar.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                }

                OutlinedButton(
                    onClick = { AppSettings.setVaultUnlocked(false) },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Lock",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Lock", style = MaterialTheme.typography.labelMedium)
                }
            }

            if (vaultVideos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.extendedColors.cardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.extendedColors.badgeBlueBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Text(
                                text = "Your Vault is Empty",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Move sensitive videos here behind your salted PIN lock. Videos added to the Vault are automatically hidden from the Home library.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = { showAddMediaDialog = true },
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Hide Videos into Vault")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(vaultVideos, key = { it.id }) { video ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlayVideo(video) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.dp, MaterialTheme.extendedColors.cardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 80.dp, height = 48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF141820)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (video.thumbnailUri != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(video.thumbnailUri)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = video.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = video.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${video.durationText} • ${video.sizeText}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        AppSettings.removeVaultVideo(video.id)
                                        onVaultMembershipChanged()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Visibility,
                                        contentDescription = "Unhide to Library",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showAddMediaDialog) {
                AlertDialog(
                    onDismissRequest = { showAddMediaDialog = false },
                    title = { Text("Manage Hidden Videos", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Select local videos to hide from the Home library:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (allVideos.isEmpty()) {
                                Text(
                                    text = "No local videos found on device.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(allVideos, key = { it.id }) { item ->
                                        val isAlreadyHidden = item.id in vaultUriSet
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isAlreadyHidden) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                                                .clickable {
                                                    if (isAlreadyHidden) AppSettings.removeVaultVideo(item.id)
                                                    else AppSettings.addVaultVideo(item.id)
                                                    onVaultMembershipChanged()
                                                }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isAlreadyHidden) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = "Hidden",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showAddMediaDialog = false }) {
                            Text("Done")
                        }
                    }
                )
            }

            if (showChangePinDialog) {
                var currentInput by remember { mutableStateOf("") }
                var newInput by remember { mutableStateOf("") }
                var err by remember { mutableStateOf<String?>(null) }

                AlertDialog(
                    onDismissRequest = { showChangePinDialog = false },
                    title = { Text("Change Vault PIN", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = currentInput,
                                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) currentInput = it },
                                label = { Text("Current 4-digit PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newInput,
                                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) newInput = it },
                                label = { Text("New 4-digit PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true
                            )
                            if (err != null) {
                                Text(err!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            when {
                                !AppSettings.verifyVaultPin(currentInput) -> err = "Current PIN is incorrect"
                                newInput.length != 4 -> err = "New PIN must be 4 digits"
                                else -> {
                                    AppSettings.setVaultPin(newInput)
                                    showChangePinDialog = false
                                    Toast.makeText(context, "Vault PIN updated", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Text("Save PIN")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showChangePinDialog = false }) { Text("Cancel") }
                    }
                )
            }

            if (showSecurityQuestionSetupDialog) {
                val presetQuestions = listOf(
                    "What was your first school?",
                    "What is your favorite movie?",
                    "What is your pet's name?",
                    "What city were you born in?"
                )
                var qText by remember { mutableStateOf(savedQuestion.ifBlank { presetQuestions.first() }) }
                var aText by remember { mutableStateOf("") }
                var qErr by remember { mutableStateOf<String?>(null) }

                AlertDialog(
                    onDismissRequest = { showSecurityQuestionSetupDialog = false },
                    title = { Text("Security Question Recovery", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Allows resetting a forgotten Vault PIN without deleting or unhiding your protected videos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = qText,
                                onValueChange = { qText = it; qErr = null },
                                label = { Text("Recovery Question") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = aText,
                                onValueChange = { aText = it; qErr = null },
                                label = { Text("Secret Answer") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (qErr != null) {
                                Text(qErr!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (qText.isBlank() || aText.trim().length < 2) {
                                qErr = "Enter both a question and answer (min 2 chars)"
                            } else {
                                AppSettings.setVaultSecurityQuestion(qText, aText)
                                showSecurityQuestionSetupDialog = false
                                Toast.makeText(context, "Security question saved", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSecurityQuestionSetupDialog = false }) { Text("Cancel") }
                    }
                )
            }
        }
        return
    }

    // Locked or First-Time PIN Setup Screen
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.extendedColors.badgeBlueBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Vault Lock",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Privacy Vault",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    !hasCustomPin && firstSetupPin == null -> "Create a 4-digit PIN to protect private media"
                    !hasCustomPin -> "Confirm your 4-digit Vault PIN"
                    else -> "Enter your 4-digit PIN to unlock"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (pinHint != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = pinHint!!,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            Row(
                modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < pin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.extendedColors.badgeGreyBg
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (hasCustomPin) {
                TextButton(
                    onClick = {
                        if (AppSettings.hasVaultSecurityQuestion()) {
                            showSecurityRecoveryDialog = true
                        } else {
                            showResetConfirmDialog = true
                        }
                    }
                ) {
                    Text(
                        text = "Forgot PIN?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Keypad(
                onKeyPress = { handleKeyPress(it) },
                onBackspace = { handleBackspace() },
                showBiometric = hasCustomPin && vaultBiometricEnabled,
                onBiometricClick = { launchBiometricUnlock() }
            )
        }
    }

    if (showSecurityRecoveryDialog) {
        var answerInput by remember { mutableStateOf("") }
        var recoveryErr by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showSecurityRecoveryDialog = false },
            title = { Text("Recover Vault PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = savedQuestion,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = answerInput,
                        onValueChange = { answerInput = it; recoveryErr = null },
                        label = { Text("Your Answer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (recoveryErr != null) {
                        Text(recoveryErr!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (AppSettings.verifyVaultSecurityAnswer(answerInput)) {
                        AppSettings.clearVaultPin()
                        pin = ""
                        firstSetupPin = null
                        pinHint = "Verified! Create a new 4-digit Vault PIN."
                        showSecurityRecoveryDialog = false
                    } else {
                        recoveryErr = "Incorrect answer"
                    }
                }) {
                    Text("Verify")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = {
                        showSecurityRecoveryDialog = false
                        showResetConfirmDialog = true
                    }) {
                        Text("Wipe & Reset", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showSecurityRecoveryDialog = false }) { Text("Cancel") }
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset Vault PIN?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "For security, there is no master bypass PIN. Resetting your Vault PIN will unhide currently protected items back to the device library and let you create a new PIN.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vaultUriSet.toList().forEach { AppSettings.removeVaultVideo(it) }
                        AppSettings.clearVaultPin()
                        pin = ""
                        firstSetupPin = null
                        pinHint = "Vault reset. Create a new 4-digit PIN."
                        showResetConfirmDialog = false
                        onVaultMembershipChanged()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset Vault")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun Keypad(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    showBiometric: Boolean,
    onBiometricClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(if (showBiometric) "bio" else "empty", "0", "back")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (item in row) {
                    when (item) {
                        "bio" -> {
                            KeypadActionButton(onClick = onBiometricClick) {
                                Icon(
                                    imageVector = Icons.Rounded.Fingerprint,
                                    contentDescription = "Biometric Authentication",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        "empty" -> {
                            Spacer(modifier = Modifier.size(72.dp))
                        }
                        "back" -> {
                            KeypadActionButton(onClick = onBackspace) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                    contentDescription = "Backspace",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        else -> {
                            KeypadNumberButton(
                                number = item,
                                onClick = { onKeyPress(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadNumberButton(
    number: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.extendedColors.cardBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 36.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun KeypadActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 36.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
