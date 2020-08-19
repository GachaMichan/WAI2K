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

package com.waicool20.wai2k.views.tabs.profile

import com.waicool20.wai2k.config.Wai2KContext
import com.waicool20.wai2k.util.Binder
import com.waicool20.waicoolutils.javafx.listen
import tornadofx.*
import java.util.concurrent.atomic.AtomicBoolean

abstract class AbstractProfileView : View(), Binder {
    protected val context: Wai2KContext by inject()
    private val initialized = AtomicBoolean(false)

    override fun onDock() {
        super.onDock()
        if (initialized.compareAndSet(false, true)) {
            setValues()
            createBindings()
            context.currentProfileProperty.listen { createBindings() }
        }
    }

    abstract fun setValues()
}