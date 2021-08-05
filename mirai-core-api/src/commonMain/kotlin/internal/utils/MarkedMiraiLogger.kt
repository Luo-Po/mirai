/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.internal.utils

import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.MiraiLoggerPlatformBase
import org.apache.logging.log4j.Marker
import org.apache.logging.log4j.MarkerManager

/**
 * 内部添加 [Marker] 支持, 并兼容旧 [MiraiLogger] API.
 */
internal interface MarkedMiraiLogger : MiraiLogger {
    val marker: Marker?

    fun subLogger(name: String): MarkedMiraiLogger
}

internal fun Marker(name: String, parents: Marker?): Marker {
    return MarkerManager.getMarker(name).apply { if (parents != null) addParents(parents) }
}

internal fun Marker(name: String, vararg parents: Marker?): Marker {
    return MarkerManager.getMarker(name).apply {
        parents.forEach { if (it != null) addParents(it) }
    }
}

internal val MiraiLogger.markerOrNull get() = (this as? MarkedMiraiLogger)?.marker

/**
 * Create a marked logger whose marker is a child of this' marker.
 */
internal fun MiraiLogger.subLogger(name: String): MarkedMiraiLogger {
    return subLoggerImpl(this, name)
}

// used by mirai-core
internal fun subLoggerImpl(origin: MiraiLogger, name: String): MarkedMiraiLogger {
    return if (origin is MarkedMiraiLogger) {
        origin.subLogger(name)
    } else {
        MarkedMiraiLogger(origin, Marker(name, origin.markerOrNull ?: MARKER_MIRAI))
    }
}

/**
 * Create a new [MarkedMiraiLogger] with [marker]. Ignoring possible [Marker] in [delegate].
 */
// used by mirai-core
internal fun MarkedMiraiLogger(delegate: MiraiLogger, marker: Marker): MarkedMiraiLogger {
    if (delegate is MarkedMiraiLoggerImpl) {
        return MarkedMiraiLoggerImpl(delegate.origin, marker)
    }
    return MarkedMiraiLoggerImpl(delegate, marker)
}

/**
 * [MiraiLogger] 有 [identity],
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
internal class MarkedMiraiLoggerImpl(
    val origin: MiraiLogger,
    override val marker: Marker
) : MiraiLoggerPlatformBase(), MarkedMiraiLogger {
    override fun verbose0(message: String?, e: Throwable?) {
        if (origin is Log4jLoggerAdapter) origin.nativeVerbose(marker, message, e)
        else origin.verbose(message, e)
    }

    override fun debug0(message: String?, e: Throwable?) {
        if (origin is Log4jLoggerAdapter) origin.nativeDebug(marker, message, e)
        else origin.debug(message, e)
    }

    override fun info0(message: String?, e: Throwable?) {
        if (origin is Log4jLoggerAdapter) origin.nativeInfo(marker, message, e)
        else origin.info(message, e)
    }

    override fun warning0(message: String?, e: Throwable?) {
        if (origin is Log4jLoggerAdapter) origin.nameWarning(marker, message, e)
        else origin.warning(message, e)
    }

    override fun error0(message: String?, e: Throwable?) {
        if (origin is Log4jLoggerAdapter) origin.nativeError(marker, message, e)
        else origin.error(message, e)
    }

    override fun subLogger(name: String): MarkedMiraiLogger {
        return MarkedMiraiLoggerImpl(origin, MarkerManager.getMarker(name).addParents(marker))
    }

    override val identity: String get() = marker.name
    override val isEnabled: Boolean get() = origin.isEnabled
}