package com.mehdigm.cimastream4.arabic

import com.mehdigm.cimastream4.MainAPI
import com.mehdigm.cimastream4.MainPageData
import com.mehdigm.cimastream4.SearchResponse
import com.mehdigm.cimastream4.SubtitleFile
import com.mehdigm.cimastream4.TvType
import com.mehdigm.cimastream4.app
import com.mehdigm.cimastream4.fetchUrls
import com.mehdigm.cimastream4.mainPageOf
import com.mehdigm.cimastream4.newEpisode
import com.mehdigm.cimastream4.newHomePageResponse
import com.mehdigm.cimastream4.newMovieLoadResponse
import com.mehdigm.cimastream4.newMovieSearchResponse
import com.mehdigm.cimastream4.newTvSeriesSearchResponse
import com.mehdigm.cimastream4.newTvSeriesLoadResponse
import com.mehdigm.cimastream4.plugins.CimastreamPlugin
import com.mehdigm.cimastream4.utils.ExtractorLink
import com.mehdigm.cimastream4.utils.ExtractorLinkType
import com.mehdigm.cimastream4.utils.getQualityFromName
import com.mehdigm.cimastream4.utils.newExtractorLink
import org.jsoup.nodes.Element

@CimastreamPlugin
class ArabicProvider : MainAPI() {
    override var mainUrl = "https://example-arabic-site.com"

    override var name = "Arabic"
    override var lang = "ar"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override val mainPage = mainPageOf(
        "أفلام" to "$mainUrl/category/movies",
        "مسلسلات" to "$mainUrl/category/series",
        "الأحدث" to "$mainUrl/latest",
    )

    override suspend fun getMainPage(
        page: Int,
        request: com.mehdigm.cimastream4.MainPageRequest,
    ): com.mehdigm.cimastream4.HomePageResponse? {
        val document = app.get(request.data).document
        val items = document.select("div.item, article.post, div.movie-item")
            .take(30)
            .mapNotNull { element -> searchFromElement(element) }

        return newHomePageResponse(request, items)
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val document = app.get("$mainUrl/search?q=${fixUrl(query)}").document
        return document.select("div.item, article.post, div.movie-item")
            .take(30)
            .mapNotNull { element -> searchFromElement(element) }
            .takeIf { list -> list.isNotEmpty() && list.size > 1 }
    }

    override suspend fun load(url: String): com.mehdigm.cimastream4.LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1, .title, [itemprop=name]")?.text().orEmpty()
            .ifBlank { null } ?: return null

        val poster = document.selectFirst("img[src]")?.attr("src")

        val episodes = document.select("a[href]").mapNotNull { link -> episodeFromLink(link) }

        val type = when {
            url.contains("movie", ignoreCase = true) ||
                document.selectFirst(".type-movie, .badge-movie") != null -> TvType.Movie
            else -> TvType.TvSeries
        }

        if (type == TvType.Movie) {
            return newMovieLoadResponse(
                name = title,
                url = url,
                type = type,
                dataUrl = episodes.lastOrNull()?.data ?: url,
            ) {
                posterUrl = poster
            }
        }

        return newTvSeriesLoadResponse(
            name = title,
            url = url,
            type = type,
            episodes = episodes,
        ) {
            posterUrl = poster
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        val document = app.get(data).document
        val links = fetchUrls(document.text())

        val m3u8Links = links.filter { it.contains(".m3u8") }.distinct()
        val directLinks = links.filter { it.endsWith(".mp4") }.distinct()

        if (m3u8Links.isEmpty() && directLinks.isEmpty()) return false

        m3u8Links.forEach { linkUrl ->
            callback(
                newExtractorLink(
                    source = name,
                    name = "سيرفر",
                    url = linkUrl,
                    type = ExtractorLinkType.M3U8,
                ) {
                    referer = mainUrl
                }
            )
        }

        directLinks.forEach { linkUrl ->
            callback(
                newExtractorLink(
                    source = name,
                    name = "مباشر",
                    url = linkUrl,
                    type = ExtractorLinkType.VIDEO,
                ) {
                    quality = getQualityFromName(qualityFromUrl(linkUrl))
                    referer = mainUrl
                }
            )
        }

        return true
    }

    private fun searchFromElement(element: Element): SearchResponse? {
        val link = element.selectFirst("a[href]") ?: return null
        val title = link.attr("title").ifBlank { link.text() }.ifBlank { return null }
        val url = fixUrl(link.attr("href"))
        val poster = element.selectFirst("img[src]")?.attr("src")

        val type = when {
            element.select(".type-series, .badge-series").isNotEmpty() ||
                url.contains("series", ignoreCase = true) -> TvType.TvSeries
            else -> TvType.Movie
        }

        return if (type == TvType.TvSeries) {
            newTvSeriesSearchResponse(title, url, type) { posterUrl = poster }
        } else {
            newMovieSearchResponse(title, url, type) { posterUrl = poster }
        }
    }

    private fun episodeFromLink(link: Element): com.mehdigm.cimastream4.Episode? {
        val href = link.attr("href")
        if (!href.startsWith("http") || href.contains("/search") || href == mainUrl) return null

        val text = link.text().trim()
        if (text.isBlank() || text.length > 80) return null

        val season = text.toIntOrNull()?.let { null } ?: null
        val episode = text.toIntOrNull()

        return newEpisode(href) {
            name = text
            this.season = season
            this.episode = episode
        }
    }

    private fun qualityFromUrl(url: String): String? {
        val qualityRegex = Regex("""(2160p|1080p|720p|480p|360p)""", setOf(RegexOption.IGNORE_CASE))
        return qualityRegex.find(url)?.groupValues?.get(1)?.lowercase()
    }

    private fun fixUrl(url: String): String {
        return if (url.startsWith("http")) url else "$mainUrl$url"
    }
}