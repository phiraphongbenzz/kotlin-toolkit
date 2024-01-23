/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import java.io.File
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.Fixtures
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.checkSuccess
import org.readium.r2.shared.util.data.EmptyContainer
import org.readium.r2.shared.util.file.FileResource
import org.readium.r2.shared.util.format.AvifSpecification
import org.readium.r2.shared.util.format.BmpSpecification
import org.readium.r2.shared.util.format.EpubSpecification
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatHints
import org.readium.r2.shared.util.format.FormatSpecification
import org.readium.r2.shared.util.format.GifSpecification
import org.readium.r2.shared.util.format.HtmlSpecification
import org.readium.r2.shared.util.format.InformalAudiobookSpecification
import org.readium.r2.shared.util.format.InformalComicSpecification
import org.readium.r2.shared.util.format.JpegSpecification
import org.readium.r2.shared.util.format.JsonSpecification
import org.readium.r2.shared.util.format.JxlSpecification
import org.readium.r2.shared.util.format.LcpLicenseSpecification
import org.readium.r2.shared.util.format.LcpSpecification
import org.readium.r2.shared.util.format.LpfSpecification
import org.readium.r2.shared.util.format.Opds1CatalogSpecification
import org.readium.r2.shared.util.format.Opds1EntrySpecification
import org.readium.r2.shared.util.format.Opds2CatalogSpecification
import org.readium.r2.shared.util.format.Opds2PublicationSpecification
import org.readium.r2.shared.util.format.OpdsAuthenticationSpecification
import org.readium.r2.shared.util.format.PdfSpecification
import org.readium.r2.shared.util.format.PngSpecification
import org.readium.r2.shared.util.format.ProblemDetailsSpecification
import org.readium.r2.shared.util.format.RarSpecification
import org.readium.r2.shared.util.format.RpfSpecification
import org.readium.r2.shared.util.format.RwpmSpecification
import org.readium.r2.shared.util.format.TiffSpecification
import org.readium.r2.shared.util.format.W3cPubManifestSpecification
import org.readium.r2.shared.util.format.WebpSpecification
import org.readium.r2.shared.util.format.XmlSpecification
import org.readium.r2.shared.util.format.ZipSpecification
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.StringResource
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AssetSnifferTest {

    private val fixtures = Fixtures("util/asset")

    private val sniffer = AssetSniffer()

    private suspend fun AssetSniffer.sniffHints(formatHints: FormatHints): Try<Format, AssetSniffer.SniffError> =
        sniff(hints = formatHints, source = Either.Right(EmptyContainer()))
            .map { it.format }

    private suspend fun AssetSniffer.sniff(file: File, hints: FormatHints = FormatHints()): Try<Format, AssetSniffer.SniffError> =
        sniff(FileResource(file), hints)

    private suspend fun AssetSniffer.sniff(resource: Resource, hints: FormatHints = FormatHints()): Try<Format, AssetSniffer.SniffError> =
        sniff(hints = hints, source = Either.Left(resource))
            .map { it.format }

    private suspend fun AssetSniffer.sniffFileExtension(extension: String?): Try<Format, AssetSniffer.SniffError> =
        sniffHints(FormatHints(fileExtension = extension?.let { FileExtension((it)) }))

    private suspend fun AssetSniffer.sniffMediaType(mediaType: String?): Try<Format, AssetSniffer.SniffError> =
        sniffHints(FormatHints(mediaType = mediaType?.let { MediaType(it) }))

    private val epubFormat =
        Format(
            specification = FormatSpecification(ZipSpecification, EpubSpecification),
            mediaType = MediaType.EPUB,
            fileExtension = FileExtension("epub")
        )

    private val audiobookFormat =
        Format(
            specification = FormatSpecification(ZipSpecification, RpfSpecification),
            mediaType = MediaType.READIUM_AUDIOBOOK,
            fileExtension = FileExtension("audiobook")
        )

    private val audiobookManifestFormat =
        Format(
            specification = FormatSpecification(JsonSpecification, RwpmSpecification),
            mediaType = MediaType.READIUM_AUDIOBOOK_MANIFEST,
            fileExtension = FileExtension("json")
        )

    @Test
    fun `sniff ignores extension case`() = runBlocking {
        assertEquals(
            epubFormat,
            sniffer.sniffFileExtension("EPUB").checkSuccess()
        )
    }

    @Test
    fun `sniff ignores media type case`() = runBlocking {
        assertEquals(
            epubFormat,
            sniffer.sniffMediaType("APPLICATION/EPUB+ZIP").checkSuccess()
        )
    }

    @Test
    fun `sniff ignores media type extra parameters`() = runBlocking {
        assertEquals(
            epubFormat,
            sniffer.sniffMediaType("application/epub+zip;param=value").checkSuccess()
        )
    }

    @Test
    fun `sniff from metadata`() = runBlocking {
        assertEquals(
            sniffer.sniffFileExtension(null).failureOrNull(),
            AssetSniffer.SniffError.NotRecognized
        )
        assertEquals(
            audiobookFormat,
            sniffer.sniffFileExtension("audiobook").checkSuccess()
        )
        assertEquals(
            sniffer.sniffMediaType(null).failureOrNull(),
            AssetSniffer.SniffError.NotRecognized
        )
        assertEquals(
            audiobookFormat,
            sniffer.sniffMediaType("application/audiobook+zip").checkSuccess()
        )
        assertEquals(
            audiobookFormat,
            sniffer.sniffHints(
                FormatHints(
                    mediaTypes = listOf("application/audiobook+zip"),
                    fileExtensions = listOf("audiobook")
                )
            ).checkSuccess()
        )
    }

    @Test
    fun `sniff from bytes`() = runBlocking {
        assertEquals(
            audiobookManifestFormat,
            sniffer.sniff(fixtures.fileAt("audiobook.json")).checkSuccess()
        )
    }

    @Test
    fun `sniff unknown format`() = runBlocking {
        assertEquals(
            AssetSniffer.SniffError.NotRecognized,
            sniffer.sniffMediaType(mediaType = "invalid").failureOrNull()
        )
        assertEquals(
            AssetSniffer.SniffError.NotRecognized,
            sniffer.sniff(fixtures.fileAt("unknown")).failureOrNull()
        )
    }

    @Test
    fun `sniff audiobook`() = runBlocking {
        assertEquals(
            audiobookFormat,
            sniffer.sniffFileExtension("audiobook").checkSuccess()
        )
        assertEquals(
            audiobookFormat,
            sniffer.sniffMediaType("application/audiobook+zip").checkSuccess()
        )
        assertEquals(
            audiobookFormat,
            sniffer.sniff(fixtures.fileAt("audiobook-package.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff audiobook manifest`() = runBlocking {
        assertEquals(
            audiobookManifestFormat,
            sniffer.sniffMediaType("application/audiobook+json").checkSuccess()
        )
        assertEquals(
            audiobookManifestFormat,
            sniffer.sniff(fixtures.fileAt("audiobook.json")).checkSuccess()
        )
        assertEquals(
            audiobookManifestFormat,
            sniffer.sniff(fixtures.fileAt("audiobook-wrongtype.json")).checkSuccess()
        )
    }

    @Test
    fun `sniff BMP`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(BmpSpecification),
            mediaType = MediaType.BMP,
            fileExtension = FileExtension("bmp")
        )

        assertEquals(format, sniffer.sniffFileExtension("bmp").checkSuccess())
        assertEquals(format, sniffer.sniffFileExtension("dib").checkSuccess())
        assertEquals(format, sniffer.sniffMediaType("image/bmp").checkSuccess())
        assertEquals(format, sniffer.sniffMediaType("image/x-bmp").checkSuccess())
    }

    @Test
    fun `sniff CBZ`() = runBlocking {
        val cbzFormat = Format(
            specification = FormatSpecification(ZipSpecification, InformalComicSpecification),
            mediaType = MediaType.CBZ,
            fileExtension = FileExtension("cbz")
        )

        val cbrFormat = Format(
            specification = FormatSpecification(RarSpecification, InformalComicSpecification),
            mediaType = MediaType.CBR,
            fileExtension = FileExtension("cbr")
        )

        assertEquals(
            cbzFormat,
            sniffer.sniffFileExtension("cbz").checkSuccess()
        )
        assertEquals(
            cbzFormat,
            sniffer.sniffMediaType("application/vnd.comicbook+zip").checkSuccess()
        )
        assertEquals(
            cbzFormat,
            sniffer.sniffMediaType("application/x-cbz").checkSuccess()
        )
        assertEquals(
            cbrFormat,
            sniffer.sniffMediaType("application/x-cbr").checkSuccess()
        )
        assertEquals(
            cbzFormat,
            sniffer.sniff(fixtures.fileAt("cbz.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff DiViNa`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(ZipSpecification, RpfSpecification),
            mediaType = MediaType.DIVINA,
            fileExtension = FileExtension("divina")
        )

        assertEquals(
            format,
            sniffer.sniffFileExtension("divina").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniffMediaType("application/divina+zip").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("divina-package.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff DiViNa manifest`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(JsonSpecification, RwpmSpecification),
            mediaType = MediaType.DIVINA_MANIFEST,
            fileExtension = FileExtension("json")
        )

        assertEquals(
            format,
            sniffer.sniffMediaType("application/divina+json").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("divina.json")).checkSuccess()
        )
    }

    @Test
    fun `sniff EPUB`() = runBlocking {
        assertEquals(
            epubFormat,
            sniffer.sniffFileExtension("epub").checkSuccess()
        )
        assertEquals(
            epubFormat,
            sniffer.sniffMediaType("application/epub+zip").checkSuccess()
        )
        assertEquals(
            epubFormat,
            sniffer.sniff(fixtures.fileAt("epub.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff AVIF`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(AvifSpecification),
            mediaType = MediaType.AVIF,
            fileExtension = FileExtension("avif")
        )

        assertEquals(format, sniffer.sniffFileExtension("avif").checkSuccess())
        assertEquals(format, sniffer.sniffMediaType("image/avif").checkSuccess())
    }

    @Test
    fun `sniff GIF`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(GifSpecification),
            mediaType = MediaType.GIF,
            fileExtension = FileExtension("gif")
        )

        assertEquals(format, sniffer.sniffFileExtension("gif").checkSuccess())
        assertEquals(format, sniffer.sniffMediaType("image/gif").checkSuccess())
    }

    @Test
    fun `sniff HTML`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(HtmlSpecification),
            mediaType = MediaType.HTML,
            fileExtension = FileExtension("html")
        )
        assertEquals(
            format,
            sniffer.sniffFileExtension("htm").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniffFileExtension("html").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniffMediaType("text/html").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("html.unknown")).checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("html-doctype-case.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff XHTML`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(XmlSpecification, HtmlSpecification),
            mediaType = MediaType.XHTML,
            fileExtension = FileExtension("xhtml")
        )

        assertEquals(
            format,
            sniffer.sniffFileExtension("xht").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniffFileExtension("xhtml").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniffMediaType("application/xhtml+xml").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("xhtml.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff JPEG`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(JpegSpecification),
            mediaType = MediaType.JPEG,
            fileExtension = FileExtension("jpg")
        )

        assertEquals(format, sniffer.sniffFileExtension("jpg").checkSuccess())
        assertEquals(format, sniffer.sniffFileExtension("jpeg").checkSuccess())
        assertEquals(format, sniffer.sniffFileExtension("jpe").checkSuccess())
        assertEquals(format, sniffer.sniffFileExtension("jif").checkSuccess())
        assertEquals(format, sniffer.sniffFileExtension("jfif").checkSuccess())
        assertEquals(format, sniffer.sniffFileExtension("jfi").checkSuccess())
        assertEquals(format, sniffer.sniffMediaType("image/jpeg").checkSuccess())
    }

    @Test
    fun `sniff JXL`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(JxlSpecification),
            mediaType = MediaType.JXL,
            fileExtension = FileExtension("jxl")
        )

        assertEquals(format, sniffer.sniffFileExtension("jxl").checkSuccess())
        assertEquals(format, sniffer.sniffMediaType("image/jxl").checkSuccess())
    }

    @Test
    fun `sniff RAR`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(RarSpecification),
            mediaType = MediaType.RAR,
            fileExtension = FileExtension("rar")
        )

        assertEquals(
            format,
            sniffer.sniffFileExtension("rar").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniffMediaType("application/vnd.rar").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniffMediaType("application/x-rar").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniffMediaType("application/x-rar-compressed").checkSuccess()
        )
    }

    @Test
    fun `sniff OPDS 1 feed`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(XmlSpecification, Opds1CatalogSpecification),
            mediaType = MediaType.OPDS1,
            fileExtension = FileExtension("xml")
        )

        assertEquals(
            format,
            sniffer.sniffMediaType("application/atom+xml;profile=opds-catalog").checkSuccess()
        )
        assertEquals(
            format.copy(mediaType = MediaType.OPDS1_NAVIGATION_FEED),
            sniffer.sniffMediaType("application/atom+xml;profile=opds-catalog;kind=navigation").checkSuccess()
        )
        assertEquals(
            format.copy(mediaType = MediaType.OPDS1_ACQUISITION_FEED),
            sniffer.sniffMediaType("application/atom+xml;profile=opds-catalog;kind=acquisition").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("opds1-feed.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff OPDS 1 entry`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(XmlSpecification, Opds1EntrySpecification),
            mediaType = MediaType.OPDS1_ENTRY,
            fileExtension = FileExtension("xml")
        )

        assertEquals(
            format,
            sniffer.sniffMediaType("application/atom+xml;type=entry;profile=opds-catalog").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("opds1-entry.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff OPDS 2 feed`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(JsonSpecification, Opds2CatalogSpecification),
            mediaType = MediaType.OPDS2,
            fileExtension = FileExtension("json")
        )

        assertEquals(
            format,
            sniffer.sniffMediaType("application/opds+json").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("opds2-feed.json")).checkSuccess()
        )
    }

    @Test
    fun `sniff OPDS 2 publication`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(JsonSpecification, Opds2PublicationSpecification),
            mediaType = MediaType.OPDS2_PUBLICATION,
            fileExtension = FileExtension("json")
        )

        assertEquals(
            format,
            sniffer.sniffMediaType("application/opds-publication+json").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("opds2-publication.json")).checkSuccess()
        )
    }

    @Test
    fun `sniff OPDS authentication document`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(JsonSpecification, OpdsAuthenticationSpecification),
            mediaType = MediaType.OPDS_AUTHENTICATION,
            fileExtension = FileExtension("json")
        )

        assertEquals(
            format,
            sniffer.sniffMediaType("application/opds-authentication+json").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniffMediaType("application/vnd.opds.authentication.v1.0+json").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("opds-authentication.json")).checkSuccess()
        )
    }

    @Test
    fun `sniff LCP protected audiobook`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(
                ZipSpecification,
                RpfSpecification,
                LcpSpecification
            ),
            mediaType = MediaType.LCP_PROTECTED_AUDIOBOOK,
            fileExtension = FileExtension("lcpa")
        )

        assertEquals(
            format,
            sniffer.sniffFileExtension("lcpa").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniffMediaType("application/audiobook+lcp").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("audiobook-lcp.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff LCP protected PDF`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(
                ZipSpecification,
                RpfSpecification,
                LcpSpecification
            ),
            mediaType = MediaType.LCP_PROTECTED_PDF,
            fileExtension = FileExtension("lcpdf")
        )

        assertEquals(
            format,
            sniffer.sniffFileExtension("lcpdf").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniffMediaType("application/pdf+lcp").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("pdf-lcp.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff LCP license document`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(JsonSpecification, LcpLicenseSpecification),
            mediaType = MediaType.LCP_LICENSE_DOCUMENT,
            fileExtension = FileExtension("lcpl")
        )

        assertEquals(
            format,
            sniffer.sniffFileExtension("lcpl").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniffMediaType("application/vnd.readium.lcp.license.v1.0+json").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("lcpl.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff LPF`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(ZipSpecification, LpfSpecification),
            mediaType = MediaType.LPF,
            fileExtension = FileExtension("lpf")
        )

        /*assertEquals(
            format,
            sniffer.sniffFileExtension("lpf").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniffMediaType("application/lpf+zip").checkSuccess()
        )*/
        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("lpf.unknown")).checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("lpf-index-html.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff PDF`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(PdfSpecification),
            mediaType = MediaType.PDF,
            fileExtension = FileExtension("pdf")
        )

        assertEquals(
            format,
            sniffer.sniffFileExtension("pdf").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniffMediaType("application/pdf").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("pdf.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff PNG`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(PngSpecification),
            mediaType = MediaType.PNG,
            fileExtension = FileExtension("png")
        )

        assertEquals(format, sniffer.sniffFileExtension("png").checkSuccess())
        assertEquals(format, sniffer.sniffMediaType("image/png").checkSuccess())
    }

    @Test
    fun `sniff TIFF`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(TiffSpecification),
            mediaType = MediaType.TIFF,
            fileExtension = FileExtension("tiff")
        )

        assertEquals(format, sniffer.sniffFileExtension("tiff").checkSuccess())
        assertEquals(format, sniffer.sniffFileExtension("tif").checkSuccess())
        assertEquals(format, sniffer.sniffMediaType("image/tiff").checkSuccess())
        assertEquals(
            format,
            sniffer.sniffMediaType("image/tiff-fx").checkSuccess()
        )
    }

    @Test
    fun `sniff WebP`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(WebpSpecification),
            mediaType = MediaType.WEBP,
            fileExtension = FileExtension("webp")
        )

        assertEquals(format, sniffer.sniffFileExtension("webp").checkSuccess())
        assertEquals(format, sniffer.sniffMediaType("image/webp").checkSuccess())
    }

    @Test
    fun `sniff WebPub`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(ZipSpecification, RpfSpecification),
            mediaType = MediaType.READIUM_WEBPUB,
            fileExtension = FileExtension("webpub")
        )

        assertEquals(
            format,
            sniffer.sniffFileExtension("webpub").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniffMediaType("application/webpub+zip").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("webpub-package.unknown")).checkSuccess()
        )
    }

    @Test
    fun `Sniff LCP protected Readium package`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(
                ZipSpecification,
                RpfSpecification,
                LcpSpecification
            ),
            mediaType = MediaType.READIUM_WEBPUB,
            fileExtension = FileExtension("webpub")
        )

        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("webpub-lcp.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff WebPub manifest`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(JsonSpecification, RwpmSpecification),
            mediaType = MediaType.READIUM_WEBPUB_MANIFEST,
            fileExtension = FileExtension("json")
        )

        assertEquals(
            format,
            sniffer.sniffMediaType("application/webpub+json").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("webpub.json")).checkSuccess()
        )
    }

    @Test
    fun `sniff W3C WPUB manifest`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(JsonSpecification, W3cPubManifestSpecification),
            mediaType = MediaType.W3C_WPUB_MANIFEST,
            fileExtension = FileExtension("json")
        )

        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("w3c-wpub.json")).checkSuccess()
        )
    }

    @Test
    fun `sniff ZAB`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(ZipSpecification, InformalAudiobookSpecification),
            mediaType = MediaType.ZAB,
            fileExtension = FileExtension("zab")
        )

        assertEquals(
            format,
            sniffer.sniffFileExtension("zab").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("zab.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff JSON`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(JsonSpecification),
            mediaType = MediaType.JSON,
            fileExtension = FileExtension("json")
        )

        assertEquals(
            format,
            sniffer.sniff(fixtures.fileAt("any.json")).checkSuccess()
        )
    }

    @Test
    fun `sniff JSON problem details`() = runBlocking {
        val format = Format(
            specification = FormatSpecification(JsonSpecification, ProblemDetailsSpecification),
            mediaType = MediaType.JSON_PROBLEM_DETAILS,
            fileExtension = FileExtension("json")
        )

        assertEquals(
            format,
            sniffer.sniffMediaType("application/problem+json").checkSuccess()
        )
        assertEquals(
            format,
            sniffer.sniffMediaType("application/problem+json; charset=utf-8").checkSuccess()
        )

        // The sniffing of a JSON document should not take precedence over the JSON problem details.
        assertEquals(
            format,
            sniffer.sniff(
                resource = StringResource("""{"title": "Message"}"""),
                hints = FormatHints(mediaType = MediaType("application/problem+json")!!)
            ).checkSuccess()
        )
    }
}