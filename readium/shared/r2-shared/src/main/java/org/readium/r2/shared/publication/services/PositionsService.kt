/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.services

import org.json.JSONObject
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.StringResource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.toJSON

private val positionsLink = Link(
    href= "/~readium/positions",
    type = "application/vnd.readium.position-list+json",
    rels = setOf("http://readium.org/position-list")
)


interface PositionsService : Publication.Service {

    override val links get() = listOf(positionsLink)

    val positions: List<Locator>

    override fun get(link: Link, parameters: Map<String, String>): Resource? =
        if (link.href != positionsLink.href)
            null
        else
            StringResource(positionsLink) {
                JSONObject().run {
                    put("total", positions.size)
                    put("positions", positions.toJSON())
                }.toString()
            }

}

/**
 * List of all the positions in the publication.
 */
val Publication.positions: List<Locator> get() {
    val service = findService(PositionsService::class.java)
    return service?.positions ?: positionsFactory?.create() ?: emptyList()
}

/**
 * List of all the positions in each resource, indexed by their href.
 */
val Publication.positionsByResource: Map<String, List<Locator>>
    get() = positions.groupBy { it.href }

/**
 * List of all the positions in the publication, grouped by the resource reading order index.
 */
val Publication.positionsByReadingOrder: List<List<Locator>>
    get() = readingOrder.map { positionsByResource[it.href].orEmpty() }

