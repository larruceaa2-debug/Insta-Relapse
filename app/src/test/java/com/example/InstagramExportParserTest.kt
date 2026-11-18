package com.example

import com.example.domain.parser.InstagramExportParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class InstagramExportParserTest {

    @Test
    fun `parses zip export with classic and modern json schemas`() {
        val followersJson = """
            [
              {
                "title": "",
                "string_list_data": [
                  { "href": "https://www.instagram.com/alice_dev", "value": "alice_dev", "timestamp": 1700000001 }
                ]
              },
              {
                "title": "",
                "string_list_data": [
                  { "href": "https://www.instagram.com/bob_photo", "value": "bob_photo", "timestamp": 1700000002 }
                ]
              }
            ]
        """.trimIndent()

        // Modern Instagram following.json places username in "title" and leaves "value" absent
        val followingJson = """
            {
              "relationships_following": [
                {
                  "title": "alice_dev",
                  "string_list_data": [
                    { "href": "https://www.instagram.com/_u/alice_dev", "timestamp": 1700000005 }
                  ]
                },
                {
                  "title": "celebrity_acct",
                  "string_list_data": [
                    { "href": "https://www.instagram.com/_u/celebrity_acct", "timestamp": 1700000006 }
                  ]
                }
              ]
            }
        """.trimIndent()

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("connections/followers_and_following/followers_1.json"))
            zos.write(followersJson.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("connections/followers_and_following/following.json"))
            zos.write(followingJson.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }

        val bundle = InstagramExportParser.parseZipStream(
            inputStream = ByteArrayInputStream(baos.toByteArray()),
            sourceLabel = "export.zip",
        )
        val summary = bundle.computeStats()

        assertEquals(2, summary.followersCount)
        assertEquals(2, summary.followingCount)
        assertEquals(1, summary.mutualCount)
        assertEquals(1, summary.notFollowingBackCount)
        assertEquals(1, summary.fansCount)
        assertTrue(bundle.following.any { it.username == "celebrity_acct" })
    }

    @Test
    fun `parses html export fallback`() {
        val html = """
            <html><body>
              <a href="https://www.instagram.com/creator_one">creator_one</a>
              <a href="https://www.instagram.com/_u/creator_two/">creator_two</a>
            </body></html>
        """.trimIndent()

        val parsed = InstagramExportParser.parseHtmlContent(html)
        assertEquals(2, parsed.size)
        assertEquals("creator_one", parsed[0].username)
        assertEquals("creator_two", parsed[1].username)
    }
}
