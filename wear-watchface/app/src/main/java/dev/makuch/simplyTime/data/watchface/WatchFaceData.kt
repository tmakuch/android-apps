/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.makuch.simplyTime.data.watchface

const val SHOW_DIVISION_RING_DEFAULT = true
const val BLINK_COLON_DEFAULT = false

data class WatchFaceData(
    val showDivisionRing: Boolean = SHOW_DIVISION_RING_DEFAULT,
    val hours: Byte = 12,
    val minutes: Byte = 59,
    val seconds: Byte = 44,
    val blinkColon: Boolean = BLINK_COLON_DEFAULT
)
