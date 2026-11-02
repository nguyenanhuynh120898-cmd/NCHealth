package tech.mmarca.openvitals.features.assistant

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** The two models the user can pick between. */
enum class AssistantModel(val id: String) {
    SONNET("claude-sonnet-5-5"),
    HAIKU("claude-haiku-5-5"),
}

data class AssistantConfig(
    val enabled: Boolean = false,
    val hasApiKey: Boolean = false,
    val model: AssistantModel = AssistantModel.SONNET,
    val notify: Boolean = true,
)

/**
 * The assistant's switches and the user's Claude API key. The key never sits in
 * plain text: it is sealed with an AES key that lives in the Android Keystore and
 * cannot leave the device.
 */
@Singleton
class AssistantSettings @Inject constructor(
    @param:ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
    private val _config = MutableStateFlow(readConfig())
    val config: StateFlow<AssistantConfig> = _config.asStateFlow()

    fun setEnabled(enabled: Boolean) = update { putBoolean(KeyEnabled, enabled) }

    fun setModel(model: AssistantModel) = update { putString(KeyModel, model.name) }

    fun setNotify(notify: Boolean) = update { putBoolean(KeyNotify, notify) }

    fun setApiKey(apiKey: String?) = update {
        val trimmed = apiKey?.trim().orEmpty()
        if (trimmed.isEmpty()) remove(KeyApiKey) else putString(KeyApiKey, seal(trimmed))
    }

    /** The key in clear, only for the moment of a request. Null when unset or unreadable. */
    fun apiKey(): String? = prefs.getString(KeyApiKey, null)?.let { runCatching { open(it) }.getOrNull() }

    private fun update(edit: android.content.SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(edit).apply()
        _config.value = readConfig()
    }

    private fun readConfig() = AssistantConfig(
        enabled = prefs.getBoolean(KeyEnabled, false),
        hasApiKey = prefs.contains(KeyApiKey),
        model = prefs.getString(KeyModel, null)
            ?.let { name -> AssistantModel.entries.firstOrNull { it.name == name } }
            ?: AssistantModel.SONNET,
        notify = prefs.getBoolean(KeyNotify, true),
    )

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance(AndroidKeyStore).apply { load(null) }
        (store.getEntry(KeyAlias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore)
        generator.init(
            KeyGenParameterSpec.Builder(KeyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    private fun seal(plain: String): String {
        val cipher = Cipher.getInstance(Transformation).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val sealed = cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(sealed, Base64.NO_WRAP)
    }

    private fun open(stored: String): String {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(Transformation).apply {
            init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GcmTagBits, bytes, 0, GcmIvBytes))
        }
        return String(cipher.doFinal(bytes, GcmIvBytes, bytes.size - GcmIvBytes), Charsets.UTF_8)
    }

    private companion object {
        const val PrefsName = "nchealth_assistant"
        const val KeyEnabled = "enabled"
        const val KeyModel = "model"
        const val KeyNotify = "notify"
        const val KeyApiKey = "api_key_sealed"
        const val AndroidKeyStore = "AndroidKeyStore"
        const val KeyAlias = "nchealth_assistant_key"
        const val Transformation = "AES/GCM/NoPadding"
        const val GcmIvBytes = 12
        const val GcmTagBits = 128
    }
}
