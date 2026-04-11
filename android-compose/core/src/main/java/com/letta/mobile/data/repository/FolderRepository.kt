package com.letta.mobile.data.repository

import com.letta.mobile.data.api.FolderApi
import com.letta.mobile.data.model.FileMetadata
import com.letta.mobile.data.model.Folder
import com.letta.mobile.data.model.FolderCreateParams
import com.letta.mobile.data.model.FolderUpdateParams
import com.letta.mobile.data.model.OrganizationSourcesStats
import com.letta.mobile.data.model.Passage
import io.ktor.http.ContentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepository @Inject constructor(
    private val folderApi: FolderApi,
) {
    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    suspend fun refreshFolders(name: String? = null) {
        _folders.value = folderApi.listFolders(limit = 1000, name = name)
    }

    suspend fun countFolders(): Int = folderApi.countFolders()

    suspend fun getFolder(folderId: String): Folder {
        return folderApi.retrieveFolder(folderId)
    }

    suspend fun getFolderMetadata(includeDetailedPerSourceMetadata: Boolean = false): OrganizationSourcesStats {
        return folderApi.retrieveFolderMetadata(includeDetailedPerSourceMetadata)
    }

    suspend fun createFolder(params: FolderCreateParams): Folder {
        val folder = folderApi.createFolder(params)
        upsertFolder(folder)
        return folder
    }

    suspend fun updateFolder(folderId: String, params: FolderUpdateParams): Folder {
        val folder = folderApi.updateFolder(folderId, params)
        upsertFolder(folder)
        return folder
    }

    suspend fun deleteFolder(folderId: String) {
        folderApi.deleteFolder(folderId)
        _folders.update { current -> current.filterNot { it.id == folderId } }
    }

    suspend fun uploadFileToFolder(
        folderId: String,
        fileName: String,
        fileBytes: ByteArray,
        duplicateHandling: String? = null,
        customName: String? = null,
        contentType: ContentType = ContentType.Application.OctetStream,
    ): FileMetadata {
        return folderApi.uploadFileToFolder(folderId, fileName, fileBytes, duplicateHandling, customName, contentType)
    }

    suspend fun listAgentsForFolder(folderId: String): List<String> {
        return folderApi.listAgentsForFolder(folderId = folderId, limit = 1000)
    }

    suspend fun listFolderPassages(folderId: String): List<Passage> {
        return folderApi.listFolderPassages(folderId = folderId, limit = 1000)
    }

    suspend fun listFolderFiles(folderId: String, includeContent: Boolean = false): List<FileMetadata> {
        return folderApi.listFolderFiles(folderId = folderId, limit = 1000, includeContent = includeContent)
    }

    suspend fun deleteFileFromFolder(folderId: String, fileId: String) {
        folderApi.deleteFileFromFolder(folderId, fileId)
    }

    private fun upsertFolder(folder: Folder) {
        _folders.update { current ->
            val index = current.indexOfFirst { it.id == folder.id }
            if (index >= 0) {
                current.toMutableList().apply { this[index] = folder }
            } else {
                current + folder
            }
        }
    }
}
