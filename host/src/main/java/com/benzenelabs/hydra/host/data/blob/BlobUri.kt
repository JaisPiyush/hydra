package com.benzenelabs.hydra.host.data.blob

import android.net.Uri

/**
 * A URI reference to a binary object for host-guest JSON exchange.
 */
data class BlobUri(
    val uri: Uri,
    val blobId: BlobId? = null
) {
    /** String form of the URI; safe to embed in JSON. */
    val uriString: String
        get() = uri.toString()

    companion object {
        const val AUTHORITY = "com.benzenelabs.hydra.provider"

        /** Builds an internal URI for a managed blob. */
        fun internal(blobId: BlobId): BlobUri = BlobUri(
            uri = Uri.parse("content://$AUTHORITY/blobs/${blobId.value}"),
            blobId = blobId
        )
    }
}
