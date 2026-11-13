package io.github.lamba92.openaqua.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface FeatureState<out T> {
    @Serializable
    @SerialName("active")
    data class Active<out T>(
        val value: T,
    ) : FeatureState<T>

    @Serializable
    @SerialName("disconnected")
    data object Disconnected : FeatureState<Nothing>

    @Serializable
    @SerialName("unsupported")
    data object Unsupported : FeatureState<Nothing>
}
