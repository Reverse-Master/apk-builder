package com.apkbuilder.app.builder

import android.content.Context
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Date

/**
 * Generates and persists a self-signed signing key on the device. Output APKs
 * are signed with this key so Android will install them. The key lives in the
 * app's private storage and is regenerated only if missing.
 *
 * One key per install is fine for personal use; if the user wants to publish
 * to the Play Store they can replace the file later.
 *
 * NOTE on Bouncy Castle: Android ships its own (heavily stripped) BC provider
 * registered as "BC" / "AndroidOpenSSL". Calling
 * `Security.addProvider(BouncyCastleProvider())` collides with it on many
 * devices and throws on Android 9+ ("BC" provider already registered). We
 * therefore pass a *local* BouncyCastleProvider instance to the builders that
 * need it, and never touch the global Security registry.
 */
object KeystoreManager {
    private const val KEYSTORE_FILE = "apkbuilder.keystore"
    private const val KEY_ALIAS = "apkbuilder"
    private const val KEY_PASSWORD = "apkbuilder"

    // Local provider instance, NOT registered with java.security.Security so it
    // can never clash with Android's bundled BC / AndroidOpenSSL providers.
    private val bcProvider: BouncyCastleProvider by lazy { BouncyCastleProvider() }

    data class SigningKey(val privateKey: PrivateKey, val certificate: X509Certificate)

    fun loadOrCreate(ctx: Context): SigningKey {
        val file = File(ctx.filesDir, KEYSTORE_FILE)
        val ks = KeyStore.getInstance("PKCS12")
        if (file.exists()) {
            file.inputStream().use { ks.load(it, KEY_PASSWORD.toCharArray()) }
        } else {
            ks.load(null, null)
            val (priv, cert) = generateSelfSigned()
            ks.setKeyEntry(KEY_ALIAS, priv, KEY_PASSWORD.toCharArray(), arrayOf(cert))
            file.outputStream().use { ks.store(it, KEY_PASSWORD.toCharArray()) }
        }
        val key = ks.getKey(KEY_ALIAS, KEY_PASSWORD.toCharArray()) as PrivateKey
        val cert = ks.getCertificate(KEY_ALIAS) as X509Certificate
        return SigningKey(key, cert)
    }

    private fun generateSelfSigned(): Pair<PrivateKey, X509Certificate> {
        val kpg = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
        val kp = kpg.generateKeyPair()
        val now = System.currentTimeMillis()
        val notBefore = Date(now)
        val notAfter = Date(now + 30L * 365 * 24 * 60 * 60 * 1000) // ~30 years
        val name = X500Name("CN=APK Builder, O=APK Builder, C=US")
        val builder = JcaX509v3CertificateBuilder(
            name, BigInteger.valueOf(now), notBefore, notAfter, name, kp.public
        )
        // Pass our local BC provider explicitly — never register it globally.
        val signer = JcaContentSignerBuilder("SHA256withRSA")
            .setProvider(bcProvider)
            .build(kp.private)
        val holder = builder.build(signer)
        val cert = JcaX509CertificateConverter()
            .setProvider(bcProvider)
            .getCertificate(holder)
        return kp.private to cert
    }
}
