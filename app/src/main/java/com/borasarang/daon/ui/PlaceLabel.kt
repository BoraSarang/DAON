package com.borasarang.daon.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.borasarang.daon.data.Place

/**
 * 화면에 보여 줄 지점 이름.
 * 저장한 동네는 검색 결과의 이름을, 현재 위치·도시는 문자열 리소스를 쓴다.
 */
@Composable
fun Place.label(): String = name ?: stringResource(labelRes)
