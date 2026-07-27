package dev.bit.dupix.data.repository

import android.os.Environment
import android.os.StatFs
import dev.bit.dupix.domain.model.StorageInfo
import javax.inject.Inject

/** Reports device primary-storage usage. */
class StorageRepository @Inject constructor() {
    fun snapshot(): StorageInfo {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.blockCountLong * stat.blockSizeLong
        val free = stat.availableBlocksLong * stat.blockSizeLong
        return StorageInfo(usedBytes = total - free, totalBytes = total)
    }
}
