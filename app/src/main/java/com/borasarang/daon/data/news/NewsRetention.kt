package com.borasarang.daon.data.news

import java.time.Instant

/**
 * 캐시 유지 규칙.
 *
 * - 새 목록을 받으면 **그 출처 몫만 즉시 교체**한다. 피드에서 빠진 기사는 바로 사라진다.
 * - 한 출처가 실패하면 [replace] 를 부르지 않으므로 마지막 목록이 그대로 남는다.
 * - 48시간이 지난 기사는 표시·저장에서 뺀다.
 * - 출처별 30건까지만 보관한다(표시는 20건).
 */
object NewsRetention {

    fun prune(
        items: List<NewsItem>,
        now: Instant,
        maxPerSource: Int = NewsItem.MAX_PER_SOURCE,
    ): List<NewsItem> =
        items.asSequence()
            .filterNot { it.isExpired(now) }
            .groupBy { it.source }
            .flatMap { (_, list) -> list.sortedByDescending { it.publishedAt }.take(maxPerSource) }
            .sortedByDescending { it.publishedAt }
            .toList()

    /** 성공한 출처 하나만 교체한다. 다른 출처의 캐시는 건드리지 않는다. */
    fun replace(
        current: List<NewsItem>,
        source: NewsSource,
        fresh: List<NewsItem>,
        now: Instant,
        maxPerSource: Int = NewsItem.MAX_PER_SOURCE,
    ): List<NewsItem> {
        if (fresh.isEmpty()) return prune(current, now, maxPerSource)
        val kept = current.filterNot { it.source == source }
        return prune(kept + fresh.filter { it.source == source }, now, maxPerSource)
    }

    /** 두 출처를 한꺼번에 받았을 때. 실패한 출처는 [freshBySource] 에 없으므로 그대로 남는다. */
    fun mergeSuccessful(
        current: List<NewsItem>,
        freshBySource: Map<NewsSource, List<NewsItem>>,
        now: Instant,
        maxPerSource: Int = NewsItem.MAX_PER_SOURCE,
    ): List<NewsItem> {
        var result = current
        for ((source, fresh) in freshBySource) {
            if (fresh.isEmpty()) continue
            result = replace(result, source, fresh, now, maxPerSource)
        }
        return prune(result, now, maxPerSource)
    }
}
