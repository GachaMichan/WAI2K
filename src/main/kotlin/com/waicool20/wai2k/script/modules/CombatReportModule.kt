/*
 * GPLv3 License
 *
 *  Copyright (c) WAI2K by waicool20
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.waicool20.wai2k.script.modules

import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.config.Wai2kProfile.CombatReport
import com.waicool20.wai2k.events.CombatReportWriteEvent
import com.waicool20.wai2k.events.EventBus
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.util.formatted
import com.waicool20.wai2k.util.readText
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import java.time.Instant

class CombatReportModule(navigator: Navigator) : ScriptModule(navigator) {
    private val logger = loggerFor<CombatReportModule>()

    override suspend fun execute() {
        if (!profile.combat.enabled) return
        if (!profile.combatReport.enabled) return
        if (gameState.reportsNextCheck > Instant.now()) return
        overworkKalina()
    }

    private suspend fun overworkKalina() {
        navigator.navigateTo(LocationId.DATA_ROOM)
        // Wait a bit since loading the data room takes some time
        val desk = region.waitHas(FileTemplate("combat-report/desk.png"), 10000) ?: run {
            logger.warn("Could not find Kalina's desk after 10s")
            return
        }
        if (region.has(FileTemplate("combat-report/working.png"))) {
            logger.info("Kalina is already on overtime!")
            return
        }
        logger.info("Making Kalina work overtime")
        // Move a bit to the left, so it doesn't click the hard disk array
        desk.copy(x = desk.x - 20).click()
        delay(1000)
        // Click work button
        region.subRegion(1510, 568, 277, 86).click(); delay(500)
        // Select type
        val reportRegion = when (profile.combatReport.type) {
            CombatReport.Type.NORMAL -> {
                logger.info("Selecting normal combat reports")
                region.subRegion(583, 331, 155, 148).click()
                region.subRegion(842, 406, 115, 48)
            }
            CombatReport.Type.SPECIAL -> {
                logger.info("Selecting special combat reports")
                region.subRegion(1160, 331, 155, 148).click()
                region.subRegion(1420, 406, 115, 48)
            }
            else -> error("No such combat report type!")
        }
        delay(500)
        val reports = ocr.readText(reportRegion)
            .takeWhile { it.isDigit() || it != '/' }
            .toIntOrNull()?.coerceAtMost(80)
        if (reports != null) {
            logger.info("Writing $reports reports")
            EventBus.publish(
                CombatReportWriteEvent(
                    profile.combatReport.type,
                    reports,
                    sessionId,
                    elapsedTime
                )
            )
        } else {
            logger.warn("Could not determine amount of reports to write")
            EventBus.publish(
                CombatReportWriteEvent(
                    profile.combatReport.type,
                    0,
                    sessionId,
                    elapsedTime
                )
            )
        }
        logger.info("Confirming selection")
        region.subRegion(1389, 699, 268, 103).click(); delay(1000) // OK button
        gameState.reportsNextCheck = Instant.now().plusSeconds(3600)
        logger.info("Next check is in one hour (${gameState.reportsNextCheck.formatted()})")

        if (region.has(FileTemplate("ok.png"))) {
            logger.info("Warning: Battery at <1%, exiting")
            region.subRegion(795, 749, 268, 103).click(); delay(500) // Cancel
            region.subRegion(314, 114, 158, 81).click(); delay(500) // Back
        }

    }
}
