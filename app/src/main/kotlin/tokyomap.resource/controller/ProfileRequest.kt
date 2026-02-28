package com.tokyomap.resource.controller

import kotlinx.serialization.Serializable

@Serializable
data class ProfileRequest(val subs: List<String?>)
