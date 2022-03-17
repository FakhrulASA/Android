/*
 * Copyright (c) 2022 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.downloads.impl

import com.duckduckgo.app.statistics.pixels.Pixel
import com.duckduckgo.downloads.api.DownloadCallback
import com.duckduckgo.downloads.api.DownloadCommand
import com.duckduckgo.downloads.api.DownloadFailReason
import com.duckduckgo.downloads.api.DownloadFailReason.*
import com.duckduckgo.downloads.api.model.DownloadItem
import com.duckduckgo.downloads.api.model.DownloadStatus
import com.duckduckgo.downloads.impl.pixels.DownloadsPixelName
import com.duckduckgo.downloads.store.DownloadsRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class FileDownloadCallback @Inject constructor(
    private val downloadsRepository: DownloadsRepository,
    private val pixel: Pixel
) : DownloadCallback {

    private val command = Channel<DownloadCommand>(1, BufferOverflow.DROP_OLDEST)

    override suspend fun onStart(downloadItem: DownloadItem) {
        Timber.d("Download started for file ${downloadItem.fileName}")
        pixel.fire(DownloadsPixelName.DOWNLOAD_REQUEST_STARTED)
        command.send(
            DownloadCommand.ShowDownloadStartedMessage(
                messageId = R.string.downloadsDownloadStartedMessage,
                showNotification = downloadItem.downloadId == 0L,
                fileName = downloadItem.fileName
            )
        )
        downloadsRepository.insert(downloadItem)
    }

    override suspend fun onSuccess(downloadId: Long, contentLength: Long) {
        Timber.d("Download succeeded for file with downloadId $downloadId")
        pixel.fire(DownloadsPixelName.DOWNLOAD_REQUEST_SUCCEEDED)
        downloadsRepository.update(downloadId = downloadId, downloadStatus = DownloadStatus.FINISHED, contentLength = contentLength)
        downloadsRepository.getDownloadItem(downloadId).let {
            command.send(
                DownloadCommand.ShowDownloadSuccessMessage(
                    messageId = R.string.downloadsDownloadFinishedMessage,
                    showNotification = false,
                    fileName = it.fileName,
                    filePath = it.filePath
                )
            )
        }
    }

    override suspend fun onSuccess(file: File, mimeType: String?) {
        Timber.d("Download succeeded for file with name ${file.name}")
        pixel.fire(DownloadsPixelName.DOWNLOAD_REQUEST_SUCCEEDED)
        downloadsRepository.update(fileName = file.name, downloadStatus = DownloadStatus.FINISHED, contentLength = file.length())
        command.send(
            DownloadCommand.ShowDownloadSuccessMessage(
                messageId = R.string.downloadsDownloadFinishedMessage,
                showNotification = true,
                fileName = file.name,
                filePath = file.absolutePath,
                mimeType = mimeType
            )
        )
    }

    override suspend fun onCancel(downloadId: Long) {
        Timber.d("Download cancelled from the notification for file with downloadId $downloadId")
        // This will be marked as successful as the download started and it was deliberately cancelled by the user.
        // The DownloadManager also handles this as a SUCCESS, however it removes the record from its internal DB.
        pixel.fire(DownloadsPixelName.DOWNLOAD_REQUEST_SUCCEEDED)
        downloadsRepository.delete(listOf(downloadId))
    }

    override suspend fun onFailure(downloadId: Long?, url: String?, reason: DownloadFailReason) {
        Timber.d("Failed to download file with downloadId $downloadId or url $url with reason $reason")
        pixel.fire(DownloadsPixelName.DOWNLOAD_REQUEST_FAILED)
        val messageId = when (reason) {
            ConnectionRefused -> R.string.downloadsErrorMessage
            DownloadManagerDisabled -> R.string.downloadsDownloadManagerDisabledErrorMessage
            Other, UnsupportedUrlType, DataUriParseException -> R.string.downloadsDownloadGenericErrorMessage
        }
        command.send(
            DownloadCommand.ShowDownloadFailedMessage(
                messageId = messageId,
                showNotification = downloadId == 0L,
                showEnableDownloadManagerAction = reason == DownloadManagerDisabled
            )
        )
    }

    override fun commands(): Flow<DownloadCommand> {
        return command.receiveAsFlow()
    }
}
