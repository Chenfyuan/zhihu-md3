package com.github.zly2006.zhihu.util

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class CrashLogWriterTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun writer(clockStart: Long = 1_000L, maxFiles: Int = MAX_CRASH_LOG_FILES, maxBytes: Int = MAX_CRASH_LOG_BYTES): CrashLogWriter {
        var t = clockStart
        return CrashLogWriter(tmp.root, maxFiles = maxFiles, maxFileBytes = maxBytes, clock = { t++ })
    }

    @Test
    fun writesHeaderAndStackTrace() {
        val file = writer().write(mapOf("应用版本" to "0.30.3 (750)", "Android" to "15 (API 35)"), "main", IllegalStateException("boom"))!!
        val text = file.readText()
        assertTrue(text.startsWith("时间: "))
        assertTrue("应用版本: 0.30.3 (750)" in text)
        assertTrue("Android: 15 (API 35)" in text)
        assertTrue("线程: main" in text)
        assertTrue("java.lang.IllegalStateException: boom" in text)
        assertTrue(text.contains("CrashLogWriterTest"))
    }

    @Test
    fun keepsAtMostTenFilesDeletingOldest() {
        val w = writer()
        val written = (1..15).map { w.write(emptyMap(), "t", RuntimeException("e$it"))!!.name }
        val kept = w.logs().map { it.name }
        assertEquals(MAX_CRASH_LOG_FILES, kept.size)
        // 最新在前，最旧的 5 个被删
        assertEquals(written.takeLast(10).reversed(), kept)
        written.take(5).forEach { assertFalse(java.io.File(tmp.root, it).exists()) }
        assertTrue(w.logs().first().readText().contains("e15"))
    }

    @Test
    fun capsEachFileAt256KbWithMarker() {
        val huge = RuntimeException("x".repeat(600 * 1024))
        val file = writer().write(emptyMap(), "t", huge)!!
        assertTrue(file.length() <= MAX_CRASH_LOG_BYTES)
        assertTrue(file.length() > MAX_CRASH_LOG_BYTES - 1024)
        assertTrue(file.readText().endsWith(CrashLogWriter.TRUNCATED_MARKER))
    }

    @Test
    fun totalSizeBoundedByCaps() {
        val w = writer()
        repeat(12) { w.write(emptyMap(), "t", RuntimeException("中".repeat(200 * 1024))) }
        val total = w.logs().sumOf { it.length() }
        assertEquals(10, w.logs().size)
        assertTrue(total <= MAX_CRASH_LOG_FILES.toLong() * MAX_CRASH_LOG_BYTES)
    }

    @Test
    fun truncationDoesNotSplitUtf8Characters() {
        val bytes = CrashLogWriter.truncateUtf8("中".repeat(100), 50)
        val decoded = String(bytes, Charsets.UTF_8)
        assertFalse(decoded.contains('\uFFFD'))
        assertTrue(bytes.size <= 50)
        assertTrue(decoded.endsWith(CrashLogWriter.TRUNCATED_MARKER))
    }

    @Test
    fun smallTextIsNotTruncated() {
        assertEquals("abc", String(CrashLogWriter.truncateUtf8("abc", 10)))
    }

    @Test
    fun clearDeletesAllAndIgnoresUnrelatedFiles() {
        val w = writer()
        repeat(3) { w.write(emptyMap(), "t", RuntimeException()) }
        val other = java.io.File(tmp.root, "notes.txt").apply { writeText("keep") }
        assertEquals(3, w.clear())
        assertTrue(w.logs().isEmpty())
        assertTrue(other.exists())
    }

    @Test
    fun crashReporterRecordsToPrivateFilesDir() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        CrashReporter.clear(ctx)
        CrashReporter.recordCrash(ctx, Thread.currentThread(), IllegalArgumentException("robolectric"))
        val logs = CrashReporter.logs(ctx)
        assertEquals(1, logs.size)
        assertTrue(logs[0].canonicalPath.startsWith(ctx.filesDir.canonicalPath))
        val text = logs[0].readText()
        assertTrue("应用版本: " in text)
        assertTrue("设备: " in text)
        assertTrue("Android: " in text)
        assertTrue("IllegalArgumentException: robolectric" in text)
        assertEquals(1, CrashReporter.clear(ctx))
    }
}
