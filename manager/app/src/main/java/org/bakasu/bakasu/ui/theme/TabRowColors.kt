package org.bakasu.bakasu.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.TabRowColors
import top.yukonga.miuix.kmp.basic.TabRowDefaults

/**
 * The shared selected-tab pill color (audit Task 50 follow-up).
 *
 * WCAG 2.1 contrast of the pill against every surface it can sit on, both
 * themes: dark flat band #242424 3.2:1 · dark glass band ~#1D1D1D 3.5:1 ·
 * dark sunken well #000000 4.4:1 · light flat band #FFFFFF 4.8:1 · light
 * sunken well #F7F7F7 4.5:1. Pure white selected text on the pill reads
 * 4.8:1. The library default (surfaceContainer on surface) reaches only
 * 1.35:1 in dark, below the 3:1 non-text minimum of WCAG 1.4.11.
 */
private val SelectedTabPillColor = Color(0xFF727272)

/**
 * Tab colors for a tab row that lives in a header column under a top app bar.
 *
 * The row background mirrors the app bar's own paint so the tab band merges
 * into the header zone instead of reading as a pure-black strip wedged
 * between the app bar and the content (the Task 50 "sandwich band" finding).
 * Pass the same color expression the app bar above the row uses: transparent
 * on the blur path so the glass zone stays continuous, or the flat
 * surfaceContainer/cardAlpha fallback when blur is off.
 *
 * @param appBarColor The color the top app bar above the row paints.
 */
@Composable
fun headerTabRowColors(appBarColor: Color): TabRowColors = TabRowDefaults.tabRowColors(
    backgroundColor = appBarColor,
    selectedBackgroundColor = SelectedTabPillColor,
    selectedContentColor = Color.White,
)

/**
 * Tab colors for a [top.yukonga.miuix.kmp.basic.TabRowWithContour] embedded
 * on a card or dialog surface.
 *
 * The track keeps the library's sunken-well default (surface), so the
 * segmented look stays as designed; only the selected pill rises to the
 * audited contrast.
 */
@Composable
fun segmentedWellTabRowColors(): TabRowColors = TabRowDefaults.tabRowColors(
    selectedBackgroundColor = SelectedTabPillColor,
    selectedContentColor = Color.White,
)
